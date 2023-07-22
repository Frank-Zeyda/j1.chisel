package j1.chisel.test

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
    dut.memory.mem.length.intValue
  }

  def readMem(addr: Int): Short = {
    require(addr < memSize)
    peekAt(dut.memory.mem, addr).shortValue
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
    code.foreach { insn =>
      require(0x0 to 0xFFFF contains insn)
      writeMem(addr, insn.toShort)
      addr = addr + 1
    }
  }

  /* ********************* */
  /* Dumping Machine State */
  /* ********************* */

  def dumpPC() = {
    val pc = peek(dut.probe.pc).intValue
    val insn = peekAt(dut.memory.mem, pc)
    val mnemonic = j1Disasm.decode(insn.shortValue)
    Console.println(f"PC: 0x${pc}%04X [${BLUE}${mnemonic}${RESET}]")
  }

  def dumpReboot() = {
    val reboot = peek(dut.probe.reboot)
    val COLOR = if (reboot == 0) { GREEN } else { RED }
    Console.println(f"${COLOR}Reboot${RESET}: ${reboot}")
  }

  def dumpStack() = {
    val dsp = peek(dut.probe.dsp).intValue
    val st0 = peekSigned(dut.probe.st0)
    Console.print(f"Data Stack: <${dsp}>")
    for (idx <- 2 to dsp) {
      val size = dut.j1cpu.dstack.mem.length.intValue
      val next = peekAtSigned(dut.j1cpu.dstack.mem, idx % size)
      Console.print(f" ${next}")
    }
    if (dsp > 0) {
      Console.print(f" ${st0}")
    }
    Console.println()
  }

  def dumpRStack() = {
    val rsp = peek(dut.probe.rsp).intValue
    Console.print(f"Return Stack: <${rsp}>")
    for (idx <- 1 to rsp) {
      val size = dut.j1cpu.rstack.mem.length.intValue
      val next = peekAt(dut.j1cpu.rstack.mem, idx % size)
      Console.print(f" $$${next}%04X")
    }
    Console.println()
  }

  def dumpState() = {
    dumpPC()
    dumpReboot()
    dumpStack()
    dumpRStack()
  }
}
