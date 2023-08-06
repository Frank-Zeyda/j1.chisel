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
package j1.chisel.test

import j1.chisel.j1Status
import j1.chisel.j1System
import j1.miniasm.MemInterface
import j1.miniasm.j1Disasm
import j1.utils.Extensions.RichBigInt

import chisel3.Element
import chisel3.MemBase

import chiseltest.iotesters.PeekPokeTester
import chiseltest.iotesters.Pokeable

import scala.io.AnsiColor._

// TODO: Review use of Console.print[ln] in favour of proper logging.

// TODO: Further reusable utility functions could be provided here.

/* PeekPokeTester is used since tests need to read and write memories.
 * NOTE: I could not find a way to do that with the generic backend. */
class j1PeekPokeTester(dut: j1System) extends PeekPokeTester(dut)
  with MemInterface /* enables deployment of programs by j1Asm */ {

  /* ****************************** */
  /* Implementation of MemInterface */
  /* ****************************** */

  val memSize: Int = {
    dut.memory.mem.length.toInt
  }

  def readMem(addr: Int): Short = {
    require(addr < memSize)
    peekAt(dut.memory.mem, addr).toShort
  }

  def writeMem(addr: Int, value: Short): Short = {
    require(addr < memSize)
    val prevContent = readMem(addr)
    pokeAt(dut.memory.mem, value, addr)
    prevContent
  }

  /* ********** */
  /* Public API */
  /* ********** */

  // Reinterprets a peeked UInt from a wire as an SInt.
  def peekSigned[E <: Element: Pokeable](signal: E): BigInt = {
    peek(signal).reinterp(signal.getWidth)
  }

  // Reinterprets a peeked UInt from memory as an SInt.
  def peekAtSigned[E <: Element: Pokeable]
      (mem: MemBase[E], offset: Int): BigInt = {
    peekAt(mem, offset).reinterp(mem.t.getWidth)
  }

  /* ****************** */
  /* Program Deployment */
  /* ****************** */

  /* Clears program memory by writing zero words to all of it. */
  def clearProgMem() = {
    for (addr <- 0 until memSize) {
      writeMem(addr, 0)
    }
  }

  /* Initializes program memory for a sequence of instructions. */
  def initProgMem(code: Seq[Int], start: Int = 0) = {
    require(start >= 0)
    var addr = start
    code.foreach {
      insn =>
        require(0x0 to 0xFFFF contains insn)
        writeMem(addr, insn.toShort)
        addr = addr + 1
    }
  }

  /* ********************* */
  /* Dumping Machine State */
  /* ********************* */

  def dumpPC() = {
    val pc = peek(dut.probe.pc).toInt
    val insn = peekAt(dut.memory.mem, pc)
    val mnemonic = j1Disasm.decode(insn.toShort)
    Console.println(f"PC: 0x${pc}%04X [${BLUE}${mnemonic}${RESET}]")
  }

  def dumpReboot() = {
    val reboot = peek(dut.probe.reboot)
    val COLOR = if (reboot == 0) { GREEN } else { RED }
    Console.println(f"${COLOR}Reboot${RESET}: ${reboot}")
  }

  def dumpStatus() = {
    val status = peek(dut.probe.status)
    val statusText =
    status.toInt match {
      case 0x0 => f"${GREEN}RUN${RESET}"
      case 0x1 => f"${RED}ILLEGAL ACCESS${RESET}"
      case 0x2 => f"${RED}UNDERFLOW${RESET} (${BOLD}DSTACK${RESET})"
      case 0x3 => f"${RED}OVERFLOW${RESET} (${BOLD}DSTACK${RESET})"
      case 0x4 => f"${RED}UNDERFLOW${RESET} (${BOLD}RSTACK${RESET})"
      case 0x5 => f"${RED}OVERFLOW${RESET} (${BOLD}RSTACK${RESET})"
      case 0x6 => f"${RED}WATCHDOG FAILURE${RESET}"
      case 0x7 => f"${BLUE}HALT${RESET}"
      case _ => assert(false, "status signal out of range")
    }
    Console.println(f"Status: ${statusText} (0x${status}%X)")
  }

  def dumpStack() = {
    var dsp = peek(dut.probe.dsp).toInt
    val st0 = peekSigned(dut.probe.st0)
    /* Correct dsp in case of stack overflow and underflow. */
    val status = peek(dut.probe.status).toInt
    if (status == 0x2) { dsp = 0 }
    if (status == 0x3) { dsp = dut.j1cpu.dstack.mem.length.toInt + 1 }
    /* Print content of data stack in the Forth style. */
    Console.print(f"Data Stack: <${dsp}>")
    for (idx <- 2 to dsp) {
      val size = dut.j1cpu.dstack.mem.length.toInt
      val next = peekAtSigned(dut.j1cpu.dstack.mem, idx % size)
      Console.print(f" ${next}")
    }
    if (dsp > 0) {
      Console.print(f" ${st0}")
    }
    Console.println()
  }

  def dumpRStack() = {
    //val status = peek(dut.probe.status)
    var rsp = peek(dut.probe.rsp).toInt
    /* Correct rsp in case of stack overflow and underflow. */
    val status = peek(dut.probe.status).toInt
    if (status == 0x4) { rsp = 0 }
    if (status == 0x5) { rsp = dut.j1cpu.dstack.mem.length.toInt }
    /* Print content of return stack in the Forth style. */
    Console.print(f"Return Stack: <${rsp}>")
    for (idx <- 1 to rsp) {
      val size = dut.j1cpu.rstack.mem.length.toInt
      val next = peekAt(dut.j1cpu.rstack.mem, idx % size)
      Console.print(f" $$${next}%04X")
    }
    Console.println()
  }

  def dumpState() = {
    dumpPC()
    dumpReboot()
    dumpStatus()
    dumpStack()
    dumpRStack()
  }
}
