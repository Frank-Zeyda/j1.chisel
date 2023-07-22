package j1.chisel.utils

import chisel3._

object Regs {
  // Creates a register that is asserted for one cycle upon reset.
  def Reboot = RegNext(next = false.B, init = true.B)

  // Creates a register with an associated wire for the next value.
  def InitWithWire[T <: Data](init: T) = {
    val regN = Wire(chiselTypeOf(init))
    val reg  = RegNext(next = regN, init = init)
    (reg, regN)
  }
}
