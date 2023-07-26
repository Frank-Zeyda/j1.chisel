package j1.chisel.test

import j1.chisel.j1Config
import j1.chisel.j1System
import j1.miniasm.j1Asm
import j1.miniasm.j1Disasm

import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec

import scala.io.AnsiColor._

class j1Test extends AnyFlatSpec with ChiselScalatestTester {
  val MAX_STEPS = 100 // to bail out of simulation when non-termating

  /* Load configuration from j1.conf properties file. */
  Console.println(f"------ ${RED}CONFIG${RESET} ------")
  /* For testing, ensure that we are not Chisel SyncReadMem. */
  implicit val cfg = j1Config.load("j1.conf").without_bb_tdp
  cfg.dump()

  behavior of "j1Asm" /* execute sample test case */
  it should "generate programs that execute correctly on the j1 core" in {
    test(new j1System).runPeekPoke(dut => new j1PeekPokeTester(dut) {
      info("See above console output for the execution trace.")

      /* First of all clear all data in code memory. */
      clearProgMem()

      /* Creatie a j1 machine program for the test. */
      val program = new j1Asm { /* the program is written in j1Asm's E-DSL */
        push(1)
        push(2)
        plus
        jmp("skip") // labels can be called before declaration
        push(3)
        label("skip")
        call("subroutine")
        plus.^      // we can also agument with .^^ or chain as in .^.x
        softhalt    // tells the simulator that we are done
        label("subroutine")
        push(-1)
        push(-2)
        plus.x
        // exit
        done
      }

      Console.println(f"------ ${RED}DISASM${RESET} ------")
      program.disasm()

      Console.println(f"------ ${RED}DEPLOY${RESET} ------")
      program.deploy(this)

      /* Carry out simulation until a software halt. */
      Console.println(f"------- ${RED}INIT${RESET} -------")
      reset()
      dumpState()
      step(1) // clears reboot flag
      var halt = false
      var step = 1
      while (!halt && step <= MAX_STEPS) {
        Console.println(f"------ ${RED}STEP ${step}${RESET} ------")
        dumpState()
        val pcOld = peek(dut.probe.pc)
        step(1)
        val pcNew = peek(dut.probe.pc)
        // Detect software halt instruction (jmp $pc)
        halt = (pcNew == pcOld)
        step = step + 1
      }
      Console.println(f"------- ${RED}DONE${RESET} -------")
      dumpState()

      /* Assert a properties of the the resulting machine state. */
      expect(peek(dut.probe.st0) == 0, f"-> ${UNDERLINED}st0 == 0${RESET}")
      expect(peek(dut.probe.dsp) == 2, f"-> ${UNDERLINED}dsp == 2${RESET}")
      expect(peek(dut.probe.rsp) == 0, f"-> ${UNDERLINED}rsp == 0${RESET}")
    }
  )}
}
