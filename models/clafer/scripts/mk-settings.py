#!/usr/bin/env python3
################################################################################
# Copyright 2023 Frank Zeyda                                                   #
#                                                                              #
# Licensed under the Apache License, Version 2.0 (the "License");              #
# you may not use this file except in compliance with the License.             #
# You may obtain a copy of the License at                                      #
#                                                                              #
#     http://www.apache.org/licenses/LICENSE-2.0                               #
#                                                                              #
# Unless required by applicable law or agreed to in writing, software          #
# distributed under the License is distributed on an "AS IS" BASIS,            #
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.     #
# See the License for the specific language governing permissions and          #
# limitations under the License.                                               #
################################################################################
import sys, os, re
import argparse, codecs

from ansiesc import *

# Bulk-generation of j1.chisel settings files from Clafer instance .data files

# Dynamically infer name of this script.
SCRIPT_NAME = os.path.basename(__file__)

# Determines the version of the script.
SCRIPT_VERSION = "1.0"

# String corresponding to a single indentation in the clafer specs.
INDENT_STR = ' ' * 2

# Directory scanning for clafer (j1.clf.<N>.data) instance files.
INSTANCES_DIR = 'instances'

# Directory to output the corresponding (j1.<N>.conf) settings files.
SETTINGS_DIR = 'settings'

# Regular expression to detect clafer instance files by their name.
INSTANCE_FILE = r'j1\.clf\.(?P<number>\d+)\.data'

# Setting the below to True to enables verbose output. (option --verbose)
VERBOSE_OUTPUT = False

# Setting the below to True to enables debugging output. (option --debug)
DEBUG_OUTPUT = False

# Default properties transferred to the Java settings file.
# We require these since it is not clear from the j1 clafer
# instance specification which settings are disabled (false).
DEFAULT_SETTINGS = {
  "j1.cpu.datawidth": None,           # mandatory
  "j1.cpu.signext": False,            # overwritten
  "j1.cpu.protect": False,            # overwritten
  "j1.cpu.protmem": 0xff,             # overwritten
  "j1.cpu.shifter": None,             # mandatory
  "j1.cpu.shifter.none": None,        # will be deleted
  "j1.cpu.shifter.minimal": None,     # will be deleted
  "j1.cpu.shifter.singlestep": None,  # will be deleted
  "j1.cpu.shifter.multistep": None,   # will be deleted
  "j1.cpu.shifter.fullbarrel": None,  # will be deleted
  "j1.cpu.stackchecks": False,        # overwritten
  "j1.cpu.relbranches": False,        # overwritten
  "j1.cpu.isa.bank": False,           # overwritten
  "j1.cpu.isa.halt": False,           # overwritten
  "j1.cpu.isa.swap16": False,         # overwritten
  "j1.cpu.isa.swap32": False,         # overwritten
  "j1.memory.size": None,             # mandatory
  "j1.memory.bbtdp": None,            # mandatory
  "j1.dstack.depth": None,            # mandatory
  "j1.rstack.depth": None,            # mandatory
}

# Symbolic enumeration values of the memsize enum type.
# This table is used to replace those by actual integers.
MEMSIZE_ENUM = {
  "MEM_1KB":   2 ** 10,
  "MEM_2KB":   2 ** 11,
  "MEM_4KB":   2 ** 12,
  "MEM_8KB":   2 ** 13,
  "MEM_16KB":  2 ** 14,
  "MEM_32KB":  2 ** 15,
  "MEM_64KB":  2 ** 16,
  "MEM_128KB": 2 ** 17,
}

#####################
# Utility Functions #
#####################

# Returns the first n characters of a string.
def prefix(lst, n):
  return lst[:n]

# Returns the last n characters of a string.
def suffix(lst, n):
  return lst[-n:]

# Prints a horizontal line of dashes.
def hline(dashes = None):
  if dashes is None: # derive dashes from terminal size
    dashes = os.get_terminal_size().columns
  print('-' * dashes)

# Like print() but outputs to stderr by default.
def eprint(*args, **kwargs):
  print(*args, file = sys.stderr, **kwargs)

# Emits an error message (msg) and exists the script.
def critical(msg):
  eprint(f"{intoRed('error')}: " + msg.strip())
  eprint("-> Aborting script due to error(s)")
  exit(1)

# Interprets and substitutes escape characters in text.
def interp_escapes(text):
  return codecs.decode(text, 'unicode_escape')

####################################
# Parsing of j1.clf.<N>.data files #
####################################

# Parses a single line within in a clafer instance specifiction.
# Returns tuple of the form (indents, clafer-name, ref-value [may be None])
def parse_line(text):
  count = 0
  tmp = text.rstrip()
  while tmp.startswith(INDENT_STR):
    tmp = tmp[len(INDENT_STR):]
    count += 1
  if '->' in tmp:
    [key, value] = tmp.split('->', 1)
    key = key.strip()
    value = value.strip()
    if value.isdigit():
      value = int(value)
  else:
    key = tmp.strip()
    value = True
  # remove $<ID> suffix added by clafer for uniqueness reasons
  key = key.split('$')[0]
  return count, key, value

# Creates a regular expression to detect the beginning of an
# instance specification for a given (abstract) clafer type.
# IMPORTANT: use --addtypes for instance generation via ClaferIG.
def mk_inst_regex(abstract, concrete):
  return r'^' + concrete + '\s*:\s*' + abstract + '\s*$'

# Parses a j1 instance specification from a j1.clf.<N>.data file.
def parse_inst(filename, lines, concrete, abstract = 'j1'):
  INST_REGEX = mk_inst_regex(abstract, concrete)
  result = []
  matching = False
  for line in lines:
    if not(matching):
      if re.match(INST_REGEX, line):
        line = abstract
        matching = True
    else:
      if not(line.startswith(INDENT_STR)):
        matching = False
        # finish parsing after the first instance is found
        return result
    if matching:
      result.append(parse_line(line))
  if result == []:
    critical(f"no instance found for clafer {intoBlue(concrete)} " +
             f"in {filename}")
  return result

# Converts the output of parse_inst(lines) into a settings dictionary.
# Only keys occuring in DEFAULT_SETTINGS are included into the result.
def parse_settings(inst):
  path = []
  settings = DEFAULT_SETTINGS.copy()
  for (idt, name, value) in inst:
    # extend / prune path
    path = prefix(path, idt) + [name]
    key = ".".join(path)
    # only define settings that exist
    if key in settings.keys():
      settings[key] = value
  return settings

# Flattens a property in a settings dictionary. The last element of
# the scoped key is considered to be the value of the property.
# Note that this function removes all extensions of base_key.
def flatten_property(settings, base_key):
  rem_keys = []
  for key, value in settings.items():
    if key.startswith(base_key + '.'):
      if value:
        # obtain last path element
        extension = key.split('.')[-1]
        settings[base_key] = extension
      rem_keys.append(key)
  # postpone deletion of keys
  for key in rem_keys:
    settings.pop(key)

# Post-processes settings by replacing symbolic enum values and
# assigning the j1.cpu.shifter property based on extension keys.
def postproc_settings(settings):
  for key, value in settings.items():
    if key == 'j1.memory.size' and value != None:
      assert value in MEMSIZE_ENUM.keys(), \
        f"unexpected symbolic value {value} for {key} setting"
      settings[key] = MEMSIZE_ENUM[value]
  flatten_property(settings, 'j1.cpu.shifter')

############################################
# Generation of j1.<N>.conf settings files #
############################################

# Converts a settings dictionary into a string (j1 configuration).
# Carries out some cosmetic adjustments while rendering the settings.
def serialize_settings(settings):
  result = ""
  for (key, value) in settings.items():
    if isinstance(value, bool):
      value = "yes" if value else "no"
    if isinstance(value, int):
      if key == "j1.cpu.protmem":
        value = f"0x{value:02x}"
    if value is not None:
      result += f"{key}: {value}\n"
  return result

# Writes a given settings dictionary to a j1 configuration file.
# The filename chosen is derived from number: j1.{number}.conf.
def write_settings(settings, number):
  filename = f'j1.{number}.conf'
  filepath = os.path.join(SETTINGS_DIR, filename)
  with open(filepath, "w") as file:
    text = serialize_settings(settings)
    file.write(text)

# Prints the corresponding j1 configuration of a settings dictionary.
def dump_settings(settings):
  text = serialize_settings(settings)
  print(text, end="")

#################
# Main Function #
#################

# Outputs the content of a j1.clf.<N>.data file after some massaging.
def print_clf_content(filepath):
  hline()
  with open(filepath) as file:
    lines = file.readlines()
    lines = filter(lambda line: not line.startswith("MEM_"), lines)
    print("".join(lines).rstrip())
  hline()

# Scans all j1.clf.<N>.data instance files and writes the resp. settings files.
def main(args):
  total = 0
  for filename in os.listdir(INSTANCES_DIR):
    filepath = os.path.join(INSTANCES_DIR, filename)
    match = re.match(INSTANCE_FILE, filename)
    if os.path.isfile(filepath) and match:
      number = int(match.group('number'))
      with open(filepath) as file:
        lines = file.readlines()
        inst = parse_inst(filename, lines, args.clafer)
        settings = parse_settings(inst)
        postproc_settings(settings)
        write_settings(settings, number)
        if DEBUG_OUTPUT:
          print_clf_content(filepath)
          dump_settings(settings)
      total += 1
  if DEBUG_OUTPUT: hline()
  if VERBOSE_OUTPUT:
    print(f"Processed {total} j1.clf.<N>.data files in total")
    print(f"-> All generating settings files are in the " +
          f"'{intoBold(SETTINGS_DIR)}' folder.")

###################
# Argument Parser #
###################

parser = argparse.ArgumentParser(
  prog = SCRIPT_NAME,
  description = '''
Utility to bulk-convert Clafer instance specifications (j1.clf.<N>.data) into
Java settings/properties files (j1.<N>.conf) for Chisel J1 CPU configuration.
'''.strip(),
  epilog = '''
* RELEASED UNDER APACHE LICENSE 2.0 (www.apache.org/licenses/LICENSE-2.0)
'''.strip())

parser.add_argument('--clafer', '-c', metavar = 'NAME',
  dest = 'clafer',
  action = 'store',
  default = 'j1_inst',
  help = 'name of (concrete) Clafer to use for instance generation; '     +
         'must be a direct subclafer of the (abstract) clafer j1, i.e., ' +
         'with some constraints attached to mitigate state explosion')

parser.add_argument('--idt', metavar = 'STRING or NUMBER',
  dest = 'indent',
  action = 'store',
  default = INDENT_STR,
  help = 'string (or number), corresponding to the indentation (level) used ' +
         'by Clafer to format instances; the default is two spaces -- '       +
         'ought not need changing')

parser.add_argument('--input-dir', '-d', metavar = 'DIRNAME',
  dest = 'inputDir',
  action = 'store',
  default = INSTANCES_DIR,
  help = 'directory from which Clafer instance specifications ' +
         '(j1.clf.<N>.data files) are read')

parser.add_argument('--output-dir', '-o', metavar = 'DIRNAME',
  dest = 'outputDir',
  action = 'store',
  default = SETTINGS_DIR,
  help = 'directory to which respective Chisel settings configurations ' +
         '(j1.<N>.conf files) are written')

parser.add_argument('--verbose', '-v',
  dest = 'verbose',
  action = 'store_true',
  default = VERBOSE_OUTPUT,
  help = 'enable verbose output, e.g., giving information about the number ' +
         'of processed instance files')

parser.add_argument('--debug', '-vv',
  dest = 'debug',
  action = 'store_true',
  default = DEBUG_OUTPUT,
  help = 'enable debug output for examining the translation from clafer ' +
         'instance specs to Java settings files')

parser.add_argument('--version', '-V',
  dest = 'version',
  action = 'store_true',
  default = False,
  help = 'print version number of the script')

# Changes the value of the following global variables:
# INDENT_STR, INSTANCES_DIR, SETTINGS_DIR, VERBOSE_OUTPUT, DEBUG_OUTPUT
def parse_args():
  global INDENT_STR
  global INSTANCES_DIR
  global SETTINGS_DIR
  global VERBOSE_OUTPUT
  global DEBUG_OUTPUT
  args = parser.parse_args()
  # pre-process option values
  if args.indent.isdigit():
    args.indent = ' ' * int(args.indent)
  args.indent = interp_escapes(args.indent)
  if args.debug: # enable verbose by default of --debug
    args.verbose = True
  # validate option values
  if not re.fullmatch(r'[a-zA-Z_][a-zA-Z0-9_]*', args.clafer):
    critical(f"'{intoBlue(args.clafer)}' not a valid clafer identifier")
  if not re.fullmatch(r'[ \t]+', args.indent):
    critical("indent string is empty or not exclusively made from spaces")
  if not os.path.isdir(args.inputDir):
    critical(f"input directory '{intoBold(args.inputDir)}' does not exist")
  if not os.path.isdir(args.outputDir):
    critical(f"output directory '{intoBold(args.outputDir)}' does not exist")
  # reassign globals according to options
  INDENT_STR = args.indent
  INSTANCES_DIR = args.inputDir
  SETTINGS_DIR = args.outputDir
  VERBOSE_OUTPUT = args.verbose
  DEBUG_OUTPUT = args.debug
  return args

###############
# Entry Point #
###############

# Calls main() function of the script.
if __name__ == "__main__":
  args = parse_args()
  if args.version:
    print(SCRIPT_VERSION, end = '')
  else: main(args)
