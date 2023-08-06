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
package j1.miniasm

import j1.miniasm.Validation._

import j1.utils.Output
import j1.utils.Extensions.RichShort

import scala.collection.mutable.{ListBuffer}
import scala.collection.mutable.{Map, HashMap}

import scala.io.AnsiColor._

import java.io.PrintWriter

class j1Asm(start: Int = 0) extends MemInterface {
  require(isValidAddr(start))

  // Maximum amount of usable memory (codespace depends on j1Config).
  val MEMSIZE_MAX = 65536 // 128 KB (REVIEW)

  // Hash table to record labels during compilation.
  private val labels: Map[String, Label] = HashMap[String, Label]()

  // Memory regions written to during compilation.
  private val regions: ListBuffer[Range] = ListBuffer[Range]()

  // Internal memory array to compile programs and data to.
  private val memory: Array[Short] = new Array[Short](MEMSIZE_MAX)

  // Start of the current compilation segment.
  private var cStart: Int = start

  // Address to compile the next instruction to.
  private var cAddr: Int = start

  /* ************ */
  /* MemInterface */
  /* ************ */

  val memSize: Int = {
    memory.size
  }

  def readMem(addr: Int): Short = {
    require(addr < memSize)
    memory(addr)
  }

  def writeMem(addr: Int, content: Short): Short = {
    require(addr < memSize)
    val prevContent = readMem(addr)
    memory(addr) = content
    prevContent
  }

  /* *********** */
  /* Private API */
  /* *********** */

  // Fetches a label with a given name from the label table.
  // Creates a table entry and instance if the label does not exist.
  private def fetchLabel(name: String): Label = {
    labels.getOrElseUpdate(name, new Label(name)(this))
  }

  /* ******************* */
  /* REWORKED UNTIL HERE */
  /* ******************* */

  /* Checks if a given address is used by another region. */
  private def usedByRegion(addr: Int): Boolean = {
    (cStart until cAddr contains addr) ||
    regions.exists {
      region => (region contains addr)
    }
  }

  /* Converts a region to a pretty string representation. */
  private def strOfRegion(region: Range, font: String = BOLD) = {
    f"[${font}0x${region.start}%X${RESET} .." +
    f" ${font}0x${region.last }%X${RESET}]"
  }

  /* Extracts the dsp increment from an instruction. */
  private def dspIncOf(insn: Int): Int = {
    require(isAluInsn(insn))
    (insn & 0x3) >> 0 match {
      case 0x0 =>  0
      case 0x1 =>  1
      case 0x2 => -2
      case 0x3 => -1
    }
  }

  /* Extracts the rsp increment from an instruction. */
  private def rspIncOf(insn: Int): Int = {
    require(isAluInsn(insn))
    (insn & 0xC) >> 2 match {
      case 0x0 =>  0
      case 0x1 =>  1
      case 0x2 => -2
      case 0x3 => -1
    }
  }

  /* Augments the last compiled instruction via a function upd. */
  private def augment(opname: String, windBack: Boolean)
                     (upd: Int => Option[Int]): j1Asm = {
    if (cAddr > cStart) {
      var lastAddr: Int = cAddr - 1
      var lastInsn: Int = memory(lastAddr) & 0xFFFF
      require(isValidInsn(lastInsn))
      /* NOTE: Tweak to support store.{^,^^} and iostore.{^,^^}. */
      if (windBack && lastInsn == Basewords.DROP.encode && lastAddr > cStart) {
        lastAddr = cAddr - 2 // wind back to skip over DROP
        lastInsn = memory(lastAddr) & 0xFFFF
      }
      if (isAluInsn(lastInsn)) {
        upd(lastInsn) match {
          case Some(augInsn) => {
            require(isValidInsn(augInsn))
            memory(lastAddr) = augInsn.toShort
            if (!j1Disasm.known(augInsn.toShort)) {
              Output.warn(
                f"Augmented instruction ${RED}0x${augInsn}%04X${RESET} " +
                f"via ${opname} not part of the ISA.")
            }
          }
          case None => {
            val mnemonic: String = j1Disasm.decode(lastInsn.toShort)
            Output.warn(
              f"Failed to augment ${BLUE}${mnemonic}${RESET} with ${opname}.")
          }
        }
      }
      else {
        Output.warn(f"Cannot augment a non-ALU instruction 0x${lastInsn}04X.")
      }
    }
    else {
      Output.warn("No instruction compiled to augment.")
    }
    this // to allow for chaining of further augmentation
  }

  /* Automatically closes the current compilation segment via done. */
  private def autoclose() = {
    if (cAddr > cStart) {
      Output.warn("Automatically closing compilation segment " +
                 f"(${BOLD}done${RESET} inserted).")
      done
    }
  }

  /* ********** */
  /* Public API */
  /* ********** */

  // Disassembles all compiled segments including the open one.
  def disasm(): Unit = {
    regions.foreach {
      region => {
        Console.println("Disassembling Segment: " + strOfRegion(region))
        j1Disasm(region)(this)
      }
    }
    // Also disassemble the currently open segment (in progress).
    if (cAddr > cStart) {
      Console.println("Disassembling Segment: " +
        strOfRegion(cStart until cAddr) + f" (${RED}open${RESET})")
      j1Disasm(cStart until cAddr)(this)
      Console.println(f"${cAddr}%04X: ...")
    }
  }

  // Deploys all compiled segments via the provided MemInterface.
  def deploy(implicit memIf: MemInterface): Unit = {
    autoclose()
    // TODO: Check that all labels are located before deploymnet.
    // Use the provided memory interface to deploy the program.
    regions.foreach {
      region => {
        Output.debug("Deploying Segment: " + strOfRegion(region))
        region.foreach {
          addr => {
            memIf.writeMem(addr, memory(addr))
            val codeword = memory(addr)
            val mnemonic = j1Disasm.decode(codeword)
            Output.debug(f"MEM[${addr}%04X] := 0x${codeword}%04X" +
                         f" (${BLUE}${mnemonic}${RESET})")
          }
        }
      }
    }
  }

  /* Writes memory content as a hexadecimal file (for $readmemh). */
  def writehex(filename: String, region: Range): Boolean = {
    require(0 until MEMSIZE_MAX contains region.start)
    require(0 until MEMSIZE_MAX contains region.last)
    if (region.isEmpty) {
      Output.warn("Writing empty memory region to '${filename}'.")
    }
    new PrintWriter(filename) {
      region.foreach {
        addr => println(memory(addr).toHexString)
      }
      close
    }.checkError
  }

  /* Writes memory content as a hexadecimal file (for $readmemh). */
  def writehex(filename: String): Boolean = {
    autoclose()
    val maxUsedAddr = regions.map(_.last).max
    writehex(filename, 0 to maxUsedAddr)
  }

  /* Writes memory content as a binary file (for $readmemb). */
  def writebin(filename: String, region: Range): Boolean = {
    require(0 until MEMSIZE_MAX contains region.start)
    require(0 until MEMSIZE_MAX contains region.last)
    if (region.isEmpty) {
      Output.warn("Writing empty memory region to '${filename}'.")
    }
    new PrintWriter(filename) {
      region.foreach {
        addr => println(memory(addr).toBinString)
      }
      close
    }.checkError
  }

  /* Writes memory content as a binary file (for $readmemb). */
  def writebin(filename: String): Boolean = {
    autoclose()
    val maxUsedAddr = regions.map(_.last).max
    writebin(filename, 0 to maxUsedAddr)
  }

  /* ************ */
  /* Embedded DSL */
  /* ************ */

  /* REVIEW: Should all E-DSL methods return "this" for uniformity? */

  def init = {
    labels.clear()
    regions.clear()
    memory.mapInPlace(_ => 0) // clear memory
    cStart = start
    cAddr = start
  }

  def org(addr: Int) = {
    require(isValidAddr(addr))
    if (usedByRegion(addr)) {
      Output.critical(f"${BLUE}org 0x${addr}%04X${RESET}" +
                       " overlaps with an existing segment.")
    }
    // Automatically close current compilation region.
    if (cAddr > cStart) {
      regions += (cStart until cAddr)
    }
    cStart = addr
    cAddr = addr
  }

  def done = {
    if (cAddr > cStart) {
      regions += (cStart until cAddr)
    }
    cStart = cAddr // open a new region after this one
  }

  def fill(words: Int = 1, value: Int = 0x0000) = {
    require(words >= 0)
    for (i <- 1 to words) {
      if (usedByRegion(cAddr)) {
        Output.critical(f"Compilation at ${YELLOW}0x${cAddr}%04X${RESET}" +
                         " overlaps with an existing segment.")
      }
      memory(cAddr) = value.toShort
      cAddr = cAddr + 1
    }
  }

  def compile(insn: Int) = {
    require(isValidInsn(insn))
    require(isValidAddr(cAddr))
    if (usedByRegion(cAddr)) {
      Output.critical(f"Compilation at ${YELLOW}0x${cAddr}%04X${RESET}" +
                       " overlaps with an existing segment.")
    }
    memory(cAddr) = insn.toShort
    cAddr = cAddr + 1
  }

  def compile(alu: Alu): j1Asm = {
    compile(alu.encode)
    this // to allow for chaining with ".^", ".^^" and ".x".
  }

  def label(name: String) = {
    fetchLabel(name).locate(cAddr)
  }

  def imm(value: Int) = {
    require(isValidImm(value))
    compile(InsnMask.IMM | value)
  }

  // NOTE: push() currently only works for the 16 bit architecture.
  // TODO: Implement a generic push() that works for any data width.
  def push(value: Int) = {
    require(-32768 until 65536 contains value)
    if ((value & 0x8000) == 0x0) {
      assert(isValidImm(value))
      imm(value)
    }
    else {
      val inv_value = (~value) & 0xFFFF
      assert(isValidImm(inv_value))
      imm(inv_value)
      invert // compile bitwise negation
    }
  }

  def jmp(target: Int) = {
    require(isValidTarget(target))
    compile(InsnMask.JMP | target)
  }

  def jmp(name: String) = {
    compile(InsnMask.JMP | 0x0000)
    fetchLabel(name).calledFrom(cAddr - 1) // REVIEW
  }

  def jpz(target: Int) = {
    require(isValidTarget(target))
    compile(InsnMask.JPZ | target)
  }

  def jpz(name: String) = {
    compile(InsnMask.JPZ | 0x0000)
    fetchLabel(name).calledFrom(cAddr - 1) // REVIEW
  }

  def call(target: Int) = {
    require(isValidTarget(target))
    compile(InsnMask.CALL | target)
  }

  def call(name: String) = {
    compile(InsnMask.CALL | 0x0000)
    fetchLabel(name).calledFrom(cAddr -1) // REVIEW
  }

  /* ********* */
  /* Basewords */
  /* ********* */

  def noop = compile(Basewords.NOOP)
  def plus = compile(Basewords.PLUS)
  def and = compile(Basewords.AND)
  def or = compile(Basewords.OR)
  def xor = compile(Basewords.XOR)
  def invert = compile(Basewords.INVERT)
  def equal = compile(Basewords.EQUAL)
  def less = compile(Basewords.LESS)
  def uless = compile(Basewords.ULESS)
  def swap = compile(Basewords.SWAP)
  def dup = compile(Basewords.DUP)
  def drop = compile(Basewords.DROP)
  def over = compile(Basewords.OVER)
  def nip = compile(Basewords.NIP)
  def to_r = compile(Basewords.TO_R)
  def from_r = compile(Basewords.FROM_R)
  def r_fetch = compile(Basewords.R_FETCH)
  def fetch = compile(Basewords.FETCH)
  def iofetch = compile(Basewords.IOFETCH)
  def rshift = compile(Basewords.RSHIFT)
  def lshift = compile(Basewords.LSHIFT)
  def depths = compile(Basewords.DEPTHS)
  def protect = compile(Basewords.PROTECT)
  def halt = compile(Basewords.HALT)
  def exit = compile(Basewords.EXIT)

  /* NOTE: The DROP really is needed to recover the TOS. */
  def store = {
    compile(Basewords.STORE)
    compile(Basewords.DROP)
  }

  /* NOTE: The DROP really is needed to recover the TOS. */
  def iostore = {
    compile(Basewords.IOSTORE)
    compile(Basewords.DROP)
  }

  /* ************ */
  /* Elided Words */
  /* ************ */

  // Augments an instruction to keep one operand on the stack.
  def ^ = {
    augment("^", true /* wind back skipping DROP */) {
      insn => {
        val dspIncNew = dspIncOf(insn) + 1
        if ((-2 to 1 contains dspIncNew) &&
            (insn & 0x0070) != AluFunc.T2N.mask) { // REVIEW
          Some((insn & ~0x3) | (dspIncNew & 0x3))
        }
        else {
          None
        }
      }
    }
  }

  // Augments an instruction to keep two operands on the stack.
  def ^^ = {
    augment("^^", true /* wind back skipping DROP */) {
      insn => {
        val dspIncNew = dspIncOf(insn) + 2
        if ((-2 to 1 contains dspIncNew) &&
            (insn & 0x0070) == AluFunc.None.mask) {
          Some((insn & ~0x3) | (dspIncNew & 0x3) | AluFunc.T2N.mask)
        }
        else {
          None
        }
      }
    }
  }

  // Augments an instruction to perform an implicit exit.
  def x = {
    augment("x", false /* do not wind back */) {
      insn => { // insn must not already EXIT or change the rsp
        if ((insn & 0x0080) == 0x0 && rspIncOf(insn) == 0 &&
            (insn & 0x0070) != AluFunc.T2R.mask) { // REVIEW
          Some((insn | 0xC /* rsp == -1 */) | AluFunc.EXIT.mask)
        }
        else {
          None
        }
      }
    }
  }

  /* Elided words that are not agumented base words. */
  def rdrop = compile(ElidedWords.RDROP)
  def tuck_store = compile(ElidedWords.TUCK_STORE)

  /* ************* */
  /* Miscellaneous */
  /* ************* */

  // Restart carries out a jump to address 0x0.
  def restart = {
    jmp(0x0)
  }

  // Software HALT operation, using a JMP <PC>.
  def softhalt = {
    jmp(cAddr)
  }
}

object j1Disasm {
  // Look-up table for Alu instructions from their encodings.
  private val aluLookup: Map[Short, Alu] = HashMap[Short, Alu]()

  // Create a look-up table for all known Alu instructions.
  Isa.ALLWORDS.foreach {
    insn => {
      aluLookup(insn.encode) = insn
      if (insn.permitsExit) {
        aluLookup(withExit(insn).encode) = withExit(insn)
      }
    }
  }

  /* *********** */
  /* Private API */
  /* *********** */

  // Infers the name of an instruction based on the Alu subclass name.
  private def mkAluName(alu: Alu): String = {
    if (alu.isInstanceOf[withExit]) {
      mkAluName(alu.asInstanceOf[withExit].wrapped) + "[EXIT]"
    }
    else {
      alu.getClass.getSimpleName
        .replace("$", "") // removes '$' suffix from object class
        .replaceFirst("(KEEP1_)(.*)", "$2^" )
        .replaceFirst("(KEEP2_)(.*)", "$2^^")
    }
  }

  /* ********** */
  /* Public API */
  /* ********** */

  /* Checks if an instruction codeword is known by the disassembler. */
  def known(codeword: Short): Boolean = {
    if (isAluInsn(codeword)) {
      aluLookup contains codeword
    }
    else true
  }

  /* Decodes an instruction codeword into a String representation. */
  def decode(codeword: Short): String = {
    if ((codeword & InsnMask.IMM) != 0x0) {
      f"IMM ${codeword & 0x7FFF}"
    }
    else {
      (codeword & 0x6000) match {
        case InsnMask.JMP =>
          f"JMP ${codeword & 0x1FFF}%04X"

        case InsnMask.JPZ =>
          f"JPZ ${codeword & 0x1FFF}%04X"

        case InsnMask.CALL =>
          f"CALL ${codeword & 0x1FFF}%04X"

        case InsnMask.ALU => {
          aluLookup.get(codeword) match {
            case Some(alu) => mkAluName(alu)
            case None      => f"<${RED}0x${codeword}04X${RESET}>"
          }
        }
      }
    }
  }

  // Create disassembly output for a given address range. */
  def apply(start: Int, end: Int)(implicit memIf: MemInterface): Unit = {
    assert(isValidAddr(start))
    assert(isValidAddr(end))
    (start to end).foreach {
      addr => {
        val codeword = memIf.readMem(addr)
        Console.println(
          f"${addr}%04X: [${YELLOW}0x${codeword}%04X${RESET}] " +
          f"${decode(codeword)}")
      }
    }
  }

  // Create disassembly output for a given memory region. */
  def apply(region: Range)(implicit memIf: MemInterface): Unit = {
    apply(region.start, region.last)
  }
}
