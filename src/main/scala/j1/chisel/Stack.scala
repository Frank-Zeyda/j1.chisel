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
