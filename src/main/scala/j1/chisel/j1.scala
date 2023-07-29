package j1.chisel

import j1.chisel.utils.Regs

import chisel3._
import chisel3.util._

class j1(implicit cfg: j1Config) extends Module {
  // Configuration
  import cfg.{datawidth}
  import cfg.{dstkDepth,rstkDepth}
  import cfg.{signext}
  import cfg.{protect,protmem}
  import cfg.{shifter}
  import cfg.{stackchecks}
  import cfg.{bank_insn, halt_insn}

  // Status Output
  object Status {
    val RUNNING          = "b000".U
    val ILLEGAL_ACCCES   = "b001".U // (UNUSED -> FUTURE)
    val DSTACK_UNDERFLOW = "b010".U
    val DSTACK_OVERFLOW  = "b011".U
    val RSTACK_UNDERFLOW = "b100".U
    val RSTACK_OVERFLOW  = "b101".U
    val WATCHDOG_FAILURE = "b110".U // (UNUSED -> FUTURE)
    val HALTED           = "b111".U
  }

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
    val status = Output(UInt(3.W))
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
  val (dsp, dspN) = Regs.InitWithWire(0.U((dstkDepth + 1).W))
  val (rsp, rspN) = Regs.InitWithWire(0.U((rstkDepth + 1).W))
  val (bank, bankN) = Regs.InitWithWire(0.U(4.W))
  val (prot, protN) = Regs.InitWithWire(false.B)
  val (halt, haltN) = Regs.InitWithWire(false.B)

  // Data Stack: Aux Wires
  val dspI = Wire(SInt(dstkDepth.W))
  val dstkW = Wire(Bool())
  dspN := dsp + dspI.asUInt

  // Return Stack: Aux Wires
  val rspI = Wire(SInt(dstkDepth.W))
  val rstkD = Wire(UInt(datawidth.W))
  val rstkW = Wire(Bool())
  rspN := rsp + rspI.asUInt

  /* Data Stack: Runtime Checking */
  /* NOTE: The number of stack elements is (1 << dstkDepth) + 1 */
  val dstkOverflow  = dspN === ((1 << dstkDepth) + 2).U
  val dstkUnderflow = dspN.asSInt === (-1).S ||
                      dspN.asSInt === (-2).S

  /* Return Stack: Runtime Checking */
  /* NOTE: The number of stack elements is (1 << rstkDepth) */
  val rstkOverflow  = dspN === ((1 << rstkDepth) + 1).U
  val rstkUnderflow = rspN.asSInt === (-1).S ||
                      rspN.asSInt === (-2).S

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

  /* Helper wire for lower memory protection */
  val protect_gate = {
    if (protect) !(prot && io.mem_addr < protmem.U) else true.B
  }

  /* Helper wire for mutli-step shifter */
  val multistep = Wire(UInt(4.W))

  /* Only allow shifts by 8/4/1 bits, respectively. */
  multistep := Mux(st0(3), 8.U, Mux(st0(2), 4.U, Mux(st0(0), 1.U, 0.U)))

  /* Memory Connections */
  io.codeaddr := pcN
  io.mem_addr := st0N
  io.dout := dstack.io.rdData
  io.mem_wr := !reboot && !halt && is_alu && func_write && protect_gate
  io.io_wr  := !reboot && !halt && is_alu && func_iowrt

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

  /* Assignment of io.status */
  when (halt) {
    if (stackchecks) {
      when (dstkUnderflow) {
        io.status := Status.DSTACK_UNDERFLOW
      }
      .elsewhen (dstkOverflow) {
        io.status := Status.DSTACK_OVERFLOW
      }
      .elsewhen (rstkUnderflow) {
        io.status := Status.RSTACK_UNDERFLOW
      }
      .elsewhen (rstkOverflow) {
        io.status := Status.RSTACK_OVERFLOW
      }
      .otherwise {
        io.status := Status.HALTED
      }
    }
    else {
      io.status := Status.HALTED
    }
  }
  .otherwise {
    io.status := Status.RUNNING
  }

  /* Update of TOS register (st0). */
  when (reboot) {
    /* To save logic resources, we can remove this case since
     * the value of st0 ought be irrelevant when dsp is 0. */
    st0N := 0.U
  }
  .elsewhen (halt) {
    st0N := st0
  }
  .otherwise {
    when (io.insn(15)) {
      /* Immediate instruction */
      if (signext) {
        st0N := io.insn(14, 0).asSInt.pad(datawidth).asUInt
      }
      else {
        st0N := io.insn(14, 0).pad(datawidth)
      }
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
              is ("b1001".U) {
                shifter match {
                  case j1Shifter.NOSHIFTER => {
                    st0N := st1
                  }
                  case j1Shifter.MINIMAL => {
                    st0N := st1 >> 1
                  }
                  case j1Shifter.SINGLESTEP => {
                    st0N := st1 >> 1
                  }
                  case j1Shifter.MULTISTEP => {
                    st0N := st1 >> multistep
                  }
                  case j1Shifter.FULLBARREL => {
                    st0N := st1 >> st0(log2Ceil(datawidth), 0)
                  }
                }
              }
              is ("b1010".U) {
                shifter match {
                  case j1Shifter.NOSHIFTER => {
                    st0N := st1
                  }
                  case j1Shifter.MINIMAL => {
                    st0N := st1 // emulated with "DUP +" (2 cycles)
                  }
                  case j1Shifter.SINGLESTEP => {
                    st0N := st1 << 1
                  }
                  case j1Shifter.MULTISTEP => {
                    st0N := st1 << multistep
                  }
                  case j1Shifter.FULLBARREL => {
                    st0N := st1 << st0(log2Ceil(datawidth), 0)
                  }
                }
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
  when (reboot || halt) {
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
          when (!(bank_insn.B && func_bank)) {
            dspI := io.insn(1, 0).asSInt.pad(dstkDepth)
            dstkW := func_T_N
          }
        }
      }
    }
  }

  /* Assignment of rspI and rstkW */
  when (reboot || halt) {
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
          when (!(bank_insn.B && func_bank)) {
            rspI := io.insn(3, 2).asSInt.pad(rstkDepth)
            rstkW := func_T_R
          }
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
  .elsewhen (halt) {
    pcN := pc
  }
  .otherwise {
    pcN := pc + 1.U // default behaviour
    switch (io.insn(15, 13)) {
      is ("b000".U) {
        /* Jmp instruction */
        pcN := bank ## io.insn(12, 0)
      }
      is ("b001".U) {
        /* Jpz instruction */
        when (!st0.orR) {
          pcN := bank ## io.insn(12, 0)
        }
      }
      is ("b010".U) {
        /* Call instruction */
        pcN := bank ## io.insn(12, 0)
      }
      is ("b011".U) {
        /* Alu instruction */
        when (io.insn(7)) {
          pcN := rst0
        }
      }
    }
  }

  /* Assignment of bankN */
  if (bank_insn) {
    when (reboot) {
      bankN := 0.U
    }
    .otherwise {
      when (is_alu && func_bank) {
        bankN := io.insn(3, 0)
      }
      .otherwise {
        // automatically clear bank register after one cycle
        bankN := 0.U
      }
    }
  }
  else {
    bankN := 0.U
  }

  /* Assignment of protN */
  if (protect) {
    when (reboot) {
      protN := false.B
    }
    .otherwise {
      when (is_alu && func_prot) {
        protN := true.B
      }
      .otherwise {
        protN := prot
      }
    }
  }
  else {
    protN := false.B
  }

  /* Assignment of haltN */
  if (halt_insn || stackchecks) {
    when (reboot) {
      haltN := false.B
    }
    .otherwise {
      haltN := halt
      if (halt_insn) {
        when (is_alu && func_halt) {
          haltN := true.B
        }
      }
      if (stackchecks) {
        when (dstkUnderflow || dstkOverflow ||
              rstkUnderflow || rstkOverflow) {
          haltN := true.B
        }
      }
    }
  }
  else {
    haltN := false.B
  }
}
