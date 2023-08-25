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

# Class providing definitions for ANSI escape sequences
class ANSI:
  RESET   = "\033[0m"
  BOLD    = "\033[1m"
  BLACK   = "\033[0;30m"
  RED     = "\033[0;31m"
  GREEN   = "\033[0;32m"
  YELLOW  = "\033[0;33m"
  BLUE    = "\033[0;34m"
  MAGENTA = "\033[0;35m"
  CYAN    = "\033[0;36m"
  WHITE   = "\033[0;36m"

  @staticmethod
  def wrap(open_esc):
    return lambda text: open_esc + text + ANSI.RESET

# Global functions for wrapping text in ANSI fonts/colors
intoRed     = ANSI.wrap(ANSI.RED)
intoGreen   = ANSI.wrap(ANSI.GREEN)
intoYellow  = ANSI.wrap(ANSI.YELLOW)
intoBlue    = ANSI.wrap(ANSI.BLUE)
intoMagenta = ANSI.wrap(ANSI.MAGENTA)
intoCyan    = ANSI.wrap(ANSI.CYAN)
intoWhite   = ANSI.wrap(ANSI.WHITE)
intoBold    = ANSI.wrap(ANSI.BOLD)
