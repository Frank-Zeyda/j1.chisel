package j1.chisel

import /*j1.chisel.*/utils.Regs

import chisel3._
import chisel3.util._

/* We assume a data/io memory space of no more than 65536 words. */

class j1(datawidth: Int, val dstkDepth: Int, rstkDepth: Int) extends Module {
  // Interface
  val io = IO(new Bundle {
    val codeaddr = Output(UInt(16.W))
    val insn = Input(UInt(16.W))
    val mem_addr = Output(UInt(16.W))
    val mem_din = Input(UInt(datawidth.W))
    val io_din = Input(UInt(datawidth.W))
    val dout = Output(UInt(datawidth.W))
    val mem_wr = Output(Bool())
    val io_wr = Output(Bool())
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
  val dstack = Module(new Stack(datawidth, dstkDepth))
  val rstack = Module(new Stack(datawidth, rstkDepth))

  // Registers
  val reboot = Regs.Reboot
  val (pc, pcN) = Regs.InitWithWire(0.U(16.W))
  val (st0, st0N) = Regs.InitWithWire(0.U(datawidth.W))
  val (dsp, dspN) = Regs.InitWithWire(0.U(dstkDepth.W))
  val (rsp, rspN) = Regs.InitWithWire(0.U(rstkDepth.W))

  // Data Stack: Aux Wires
  val dspI = Wire(SInt(dstkDepth.W))
  val dstkW = Wire(Bool())
  dspN := dsp + dspI.asUInt

  // Return Stack: Aux Wires
  val rspI = Wire(SInt(dstkDepth.W))
  val rstkD = Wire(UInt(datawidth.W))
  val rstkW = Wire(Bool())
  rspN := rsp + rspI.asUInt

  /* Helper wires for instruction decoding */
  val is_alu = (io.insn(15, 13) === "b011".U)

  /* Helper wires for ALU functions */
  val func_T_N   = (io.insn(6, 4) === 1.U)
  val func_T_R   = (io.insn(6, 4) === 2.U)
  val func_write = (io.insn(6, 4) === 3.U)
  val func_iowrt = (io.insn(6, 4) === 4.U)
  val func_bank  = (io.insn(6, 4) === 5.U)
  val func_prot  = (io.insn(6, 4) === 6.U)
  val func_halt  = (io.insn(6, 4) === 7.U)
  val func_exit  = (io.insn(7)    === 1.U)

  /* Memory Connections */
  io.codeaddr := pcN
  io.mem_addr := st0N
  io.dout := dstack.io.rdData
  io.mem_wr := !reboot && is_alu && func_write
  io.io_wr  := !reboot && is_alu && func_iowrt

  /* Data Stack Connections */
  val st1 = dstack.io.rdData
  dstack.io.rdAddr := dsp
  dstack.io.wrAddr := dspN
  dstack.io.wrData := st0
  dstack.io.wrEna := dstkW

  /* Return Stack Connections */
  val rst0 = rstack.io.rdData
  rstack.io.rdAddr := rsp
  rstack.io.wrAddr := rspN
  rstack.io.wrData := rstkD
  rstack.io.wrEna := rstkW

  // Connect signal probes for simulation testing
  probe.pc := pc
  probe.reboot := reboot
  probe.dsp := dsp
  probe.rsp := rsp
  probe.st0 := st0
  probe.st1 := st1

  /* Update of TOS register (st0). */
  when (reboot) {
    /* To save logic resources, we can remove this case since
     * the value of st0 ought be irrelevant when dsp is 0. */
    st0N := 0.U
  }
  .otherwise {
    when (io.insn(15)) {
      /* Immediate instruction */
      /* TODO: Support configuration of sign-extended immediate pushes. */
      st0N := io.insn(14, 0).pad(datawidth)
    }
    .otherwise {
      st0N := st0 // default case
      switch (io.insn(14, 13)) {
        is ("b00".U) {
          /* Jmp instruction */
          st0N := st0
        }
        is ("b01".U) {
          /* Jpz instruction */
          st0N := st1
        }
        is ("b10".U) {
          /* Call instruction */
          st0N := st0
        }
        is ("b11".U) {
          /* Alu instruction */
          when (!io.insn(12)) {
            switch (io.insn(11, 8)) {
              /* Standard ISA */
              is ("b0000".U) {
                st0N := st0
              }
              is ("b0001".U) {
                st0N := st1
              }
              is ("b0010".U) {
                st0N := st0 + st1
              }
              is ("b0011".U) {
                st0N := st0 & st1
              }
              is ("b0100".U) {
                st0N := st0 | st1
              }
              is ("b0101".U) {
                st0N := st0 ^ st1
              }
              is ("b0110".U) {
                st0N := ~st0
              }
              is ("b0111".U) {
                st0N := Fill(datawidth, st0 === st1)
              }
              is ("b1000".U) {
                st0N := Fill(datawidth, st0.asSInt < st1.asSInt)
              }
              // TODO: Support several shifter variants (configurable).
              is ("b1001".U) {
                st0N := st1 >> st0(4, 0)
              }
              is ("b1010".U) {
                st0N := st1 << st0(4, 0)
              }
              is ("b1011".U) {
                st0N := rst0
              }
              is ("b1100".U) {
                st0N := io.mem_din
              }
              is ("b1101".U) {
                st0N := io.io_din
              }
              is ("b1110".U) {
                st0N := rsp ## dsp
              }
              is ("b1111".U) {
                st0N := Fill(datawidth, st0 < st1)
              }
            }
          }
          .otherwise {
            /* ISA Extensions */
            /* TODO: Add extended instruction set (configurable). */
          }
        }
      }
    }
  }

  /* NOTE: The two when () blocks below can be shortened, but though more
   * verbose, I prefer the epxplicit style, giving the behaviour for each
   * instruction separately rather than relying on default cases / values. */

  /* Assignment of dspI and dstkW */
  when (reboot) {
    dspI := 0.S
    dstkW := false.B
  }
  .otherwise {
    when (io.insn(15)) {
      /* Immediate instruction */
      dspI := 1.S
      dstkW := true.B
    }
    .otherwise {
      /* The following two ought not be needed. Bug in Chisel? */
      dspI := 0.S
      dstkW := false.B
      switch (io.insn(14, 13)) {
        is ("b00".U) {
          /* Jmp instruction */
          dspI := 0.S
          dstkW := false.B
        }
        is ("b01".U) {
          /* Jpz instruction */
          dspI := (-1).S
          dstkW := false.B
        }
        is ("b10".U) {
          /* Call instruction */
          dspI := 0.S
          dstkW := false.B
        }
        is ("b11".U) {
          /* Alu instruction */
          dspI := io.insn(1, 0).asSInt.pad(dstkDepth)
          dstkW := func_T_N
        }
      }
    }
  }

  /* Assignment of rspI and rstkW */
  when (reboot) {
    rspI := 0.S
    rstkW := false.B
  }
  .otherwise {
    when (io.insn(15)) {
      /* Immediate instruction */
      rspI := 0.S
      rstkW := false.B
    }
    .otherwise {
      /* The following two ought not be needed. Bug in Chisel? */
      rspI := 0.S
      rstkW := false.B
      switch (io.insn(14, 13)) {
        is ("b00".U) {
          /* Jmp instruction */
          rspI := 0.S
          rstkW := false.B
        }
        is ("b01".U) {
          /* Jpz instruction */
          rspI := 0.S
          rstkW := false.B
        }
        is ("b10".U) {
          /* Call instruction */
          rspI := 1.S
          rstkW := true.B
        }
        is ("b11".U) {
          /* Alu instruction */
          rspI := io.insn(3, 2).asSInt.pad(rstkDepth)
          rstkW := func_T_R
        }
      }
    }
  }

  /* The below, I presume, is equivalent. Check with Martin Schoeberl. */
  /* TODO: So if there is any benefit using this version wrt logic usage. */
/*
  rspI := 0.S
  rstkW := false.B
  switch (io.insn(15, 13)) {
    is ("b10".U) {
      /* Call instruction */
      rspI := 1.S
      rstkW := true.B
    }
    is ("b11".U) {
      /* Alu instruction */
      rspI := io.insn(3, 2).asSInt.pad(rstkDepth)
      rstkW := func_T_R
    }
  }
*/

  /* Assignment of rstkD */
  /* NOTE: For Jpz, we have rstkW := false.B, so rstkD is irrelevant. */
  rstkD := Mux(io.insn(13), st0 /* Jpz or Alu */, pc + 1.U)

  /* Assignment of pcN */
  when (reboot) {
    pcN := 0.U
  }
  .otherwise {
    pcN := pc + 1.U // default behaviour
    switch (io.insn(15, 13)) {
      is ("b000".U) {
        /* Jmp instruction */
        pcN := io.insn(12, 0)
      }
      is ("b001".U) {
        /* Jpz instruction */
        when (!st0.orR) {
          pcN := io.insn(12, 0)
        }
      }
      is ("b010".U) {
        /* Call instruction */
        pcN := io.insn(12, 0)
      }
      is ("b011".U) {
        /* Alu instruction */
        when (io.insn(7)) {
          pcN := rst0
        }
      }
    }
  }
}
