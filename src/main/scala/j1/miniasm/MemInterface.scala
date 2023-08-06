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
package j1.miniasm

/* Memory interface for reading / writing the J1 program memory. */

trait MemInterface {
  // Total number of memory addresses.
  val memSize: Int

  // Reads memory word at address addr.
  def readMem(addr: Int): Short

  // Writes memory word at address addr. Returns previous content.
  def writeMem(addr: Int, value: Short): Short
}
