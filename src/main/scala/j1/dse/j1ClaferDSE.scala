/**
 * Copyright 2023 Frank Zeyda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package j1.chisel

import j1.utils.Output

import circt.stage.ChiselStage

import java.io.File

import scala.reflect.io.Directory

import scala.io.AnsiColor._

object j1ClaferDSE extends App {
  /* Input directory from which J1 configuration files are read. */
  val SETTINGS_DIR = new File("models/clafer/settings")

  /* Output directory for SystemVerilog RTL design instances. */
  val RTL_ROOT = new File("models/clafer/rtl")

  /* Regular expression for detecting settings files: j1.<N>.conf */
  val J1_CONF_REGEX = raw"j1\.(\d+).conf".r

  /* ***************** */
  /* Utility Functions */
  /* ***************** */

  /* Tests if a given Java File is a j1.<N>.conf settings file. */
  def is_j1_conf_file(file: File): Boolean = {
    file.isFile && J1_CONF_REGEX.matches(file.getName)
  }

  /* Less-than function used to order lists of j1.<N>.conf files. */
  def j1_conf_file_le(file1: File, file2: File): Boolean = {
    var n1: Int = 0;
    var n2: Int = 0;
    file1.getName() match {
      case J1_CONF_REGEX(n) => n1 = n.toInt
    }
    file2.getName match {
      case J1_CONF_REGEX(n) => n2 = n.toInt
    }
    n1 < n2
  }

  /* Scans the SETTINGS_DIR for j1.<N>.conf configuration files and sorts
   * them in ascending order according to N. The result is a File List. */
  def scanForConfFiles: List[File] = {
    val sourceDir = SETTINGS_DIR
    if (sourceDir.isDirectory) {
      sourceDir.listFiles
               .filter(is_j1_conf_file)
               .sortWith(j1_conf_file_le)
               .toList
    }
    else {
      Output.critical(
        s"Settings directory '${BOLD}${SETTINGS_DIR}${RESET}' does not exist.")
      Nil
    }
  }

  /* ******************** */
  /* Application Behavior */
  /* ******************** */

  /* Delete and recreate the RTL_ROOT directory for RTL outputs. */
  Output.info(
    s"Emptying '${BOLD}${RTL_ROOT}${RESET}' directory for RTL outputs.")
  if (RTL_ROOT.exists) {
    Output.warn(s"Directory '${BOLD}${RTL_ROOT}${RESET}' exists, " +
                 "recursively deleting it!")
    if (!Directory(RTL_ROOT).deleteRecursively()) {
      Output.critical(
        s"Failed to delete directory '${BOLD}${RTL_ROOT}${RESET}'.")
    }
  }
  if (!RTL_ROOT.mkdir) {
    Output.critical(
      s"Failed to (re)create directory '${BOLD}${RTL_ROOT}${RESET}'.")
  }

  /* Scan SETTINGS_DIR for j1.<N>.conf configuration files. */
  Output.info(
    s"Scanning for j1.<N>.conf files in '${BOLD}${SETTINGS_DIR}${RESET}'.")
  var no_settings_files: Boolean = true
  scanForConfFiles.foreach(
    file => {
      /* Record (base)name and path of the j1.<N>.conf file. */
      val filename = file.getName
      val filepath = file.getPath

      /* Create subfolder in RTL_ROOT (j1.<N>.rtl) for outputs. */
      val RTL_SUBDIR =
        new File(RTL_ROOT, filename.replaceAll("\\.conf$", ".rtl"))
      assert(!RTL_SUBDIR.exists)
      if (!RTL_SUBDIR.mkdir) {
        Output.critical(
          s"Failed to create directory '${BOLD}${RTL_SUBDIR}${RESET}'.")
      }

      /* Load j1 configuration from j1.<N>.conf settings file. */
      Output.info(f"Loading configuration from '${BOLD}${filepath}${RESET}'.")
      implicit val cfg = j1Config.load(filepath).with_bb_tdp

      /* Generate SystemVerilog files for the Chisel design. */
      Output.info(
        f"Generating SystemVerilog files under '${BOLD}${RTL_SUBDIR}${RESET}'.")
      ChiselStage.emitSystemVerilogFile(new j1System,
        Array("--target-dir", "dummy"),
        Array("--strip-debug-info",
              "--disable-all-randomization",
              "--split-verilog", "-o", RTL_SUBDIR.getPath))

      /* The SETTINGS_DIR folder contains at least one j1.<N>.conf file. */
      no_settings_files = false
    }
  )

  /* Output an error message if not settings files were detected. */
  if (no_settings_files) {
    Output.error(
      s"No settings files found in '${BOLD}${SETTINGS_DIR}${RESET}'.\n" +
      s"-> Execute ${BOLD}make${RESET} in the models/clafer top-level folder.")
  }
}
