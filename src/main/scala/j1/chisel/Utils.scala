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
package j1.chisel.utils

import chisel3._

object Regs {
  // Creates a register that is asserted for one cycle upon reset.
  def Reboot = RegNext(next = false.B, init = true.B)

  // Creates a register with an associated wire for the next value.
  def InitWithWire[T <: Data](init: T) = {
    val reg = RegInit(init)
    val regN = Wire(chiselTypeOf(init))
    reg := regN // connect regN wire to reg's next value
    (reg, regN)
  }
}

object Wires {
  implicit class RichBits(val w1: Bits) {
    def ovrride (w2: Bits) = {
      require(w1.getWidth > 0)
      require(w2.getWidth > 0)
      if (w1.getWidth > w2.getWidth) {
        w1(w1.getWidth - 1, w2.getWidth) ## w2
      }
      else {
        w2(w1.getWidth - 1, 0)
      }
    }
  }
}
