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

import chisel3._

/* NOTE: Observe the use of combinatorial (asynchronous) memory. */
class Stack(width: Int, depth: Int) extends Module {
  // Interface
  val io = IO(new Bundle {
    val rdAddr = Input(UInt(depth.W))
    val rdData = Output(UInt(width.W))
    val wrAddr = Input(UInt(depth.W))
    val wrData = Input(UInt(width.W))
    val wrEna = Input(Bool())
  })

  // Submodules
  val mem = Mem(1 << depth, UInt(width.W))

  /* Connect Read Ports */
  io.rdData := mem.read(io.rdAddr)

  /* Connect Write Ports */
  when (io.wrEna) {
    mem.write(io.wrAddr, io.wrData)
  }
}
