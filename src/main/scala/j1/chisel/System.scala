package j1.chisel

import j1.examples.ChaserLight3

import circt.stage.ChiselStage

import chisel3._
import chisel3.util._

import scala.io.AnsiColor._

/* Example system with three connected LEDS. */
class j1System(datawidth: Int = 16,
               dstkDepth: Int = 5,
               rstkDepth: Int = 5,
               memsize: Int = 8192,
               /* For testing disable and for Verilog generation enable */
               use_bb_tdpram: Boolean = false) extends Module {
  // Interface
  val io = IO(new Bundle {
    val led0 = Output(Bool())
    val led1 = Output(Bool())
    val led2 = Output(Bool())
  })

  // Test Probes
  val probe = IO(new Bundle {
    val pc = Output(UInt(16.W))
    val reboot = Output(Bool())
    val dsp = Output(UInt(dstkDepth.W))
    val rsp = Output(UInt(rstkDepth.W))
    val st0 = Output(UInt(datawidth.W))
    val st1 = Output(UInt(datawidth.W))
  })

  // Submodules
  val j1cpu = Module(new j1(datawidth, dstkDepth, rstkDepth))
  val memory: DualPortedRAM = {
    if (use_bb_tdpram) {
      Module(new BBDualPortedRAM(datawidth, memsize))
    }
    else {
      Module(new ChiselDualPortedRAM(datawidth, memsize))
    }
  }

  // Connections: Memory Inputs
  memory.io.clock := clock // only used by BB module
  memory.io.rdAddrA := j1cpu.io.codeaddr
  memory.io.wrAddrA := DontCare // UNUSED
  memory.io.rdAddrB := j1cpu.io.mem_addr
  memory.io.wrAddrB := j1cpu.io.mem_addr
  memory.io.wrDataA := DontCare // UNSUED
  memory.io.wrDataB := j1cpu.io.dout
  memory.io.wrEnaA  := false.B
  memory.io.wrEnaB  := j1cpu.io.mem_wr

  // Connections: J1 CPU Inputs
  j1cpu.io.insn    := memory.io.rdDataA
  j1cpu.io.mem_din := memory.io.rdDataB

  // Connections: Test Probes
  probe.pc := j1cpu.probe.pc
  probe.reboot := j1cpu.probe.reboot
  probe.dsp := j1cpu.probe.dsp
  probe.rsp := j1cpu.probe.rsp
  probe.st0 := j1cpu.probe.st0
  probe.st1 := j1cpu.probe.st1

  // LEDS Device Interface
  val leds_state = RegInit(7.U(3.W))

  // External Connections
  io.led0 := leds_state(0)
  io.led1 := leds_state(1)
  io.led2 := leds_state(2)

  /********************/
  /* IO Space Mapping */
  /********************/

  /* Buffer j1cup comibatorial outputs to delay device writes. */
  val io_addr = RegNext(next = j1cpu.io.mem_addr, init = 0.U)
  val io_dout = RegNext(next = j1cpu.io.dout, init = 0.U)
  val io_wr   = RegNext(next = j1cpu.io.io_wr , init = false.B)

  /* IO Mapping: Read Action */
  j1cpu.io.io_din := 0.U
  switch (j1cpu.io.dout) {
    is ("h0000".U) {
      j1cpu.io.io_din := leds_state
    }
  }

  /* IO Mapping: Write Action */
  when (io_wr) {
    switch (io_addr) {
      is ("h0000".U) {
        leds_state := io_dout
      }
    }
  }
}

object j1SystemGen extends App {
  import j1.utils.Output
  Output.info(
    f"Generating Verilog files inside '${BOLD}generated${RESET}' folder ...")
  ChiselStage.emitSystemVerilogFile(new j1System(16, 5, 5, 4096, true),
    Array("--target-dir", "dummy"),
    Array("--strip-debug-info",
          "--disable-all-randomization",
          "--split-verilog", "-o", "generated"))
  Output.info(
    f"Creating memory initialization file for example program" +
    f" (${RED}ChaserLight3${RESET})")
  ChaserLight3.disasm()
  ChaserLight3.writehex("generated/meminit.hex")
  Output.info(
    f"Find all generated files inside the '${BOLD}generated${RESET}' folder.")
}
