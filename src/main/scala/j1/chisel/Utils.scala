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
