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
package j1.chisel

import j1.utils.Extensions.RichBoolean
import j1.utils.Extensions.RichString
import j1.utils.Extensions.RichProperties
import j1.utils.FiniteEnum
import j1.utils.PropValue
import j1.utils.Output

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

import scala.io.AnsiColor._

/* Enumeration type for bit-shifter setting */
sealed abstract case class j1Shifter(propValue: String) extends PropValue {
  override def toString: String = {
    //getClass.getSimpleName.removeSuffix("$")
    propValue
  }
}

/* Enumeration values for bit-shifter setting */
object j1Shifter {
  // Do not deploy a bit-shifter (full emulation).
  object NOSHIFTER  extends j1Shifter("none")
  // Only supports right shifts by one step.
  object MINIMAL    extends j1Shifter("minimal")
  // Supports left and right shifts by one step.
  object SINGLESTEP extends j1Shifter("singlestep")
  // Supports 1-4-8 shifts in both directions.
  object MULTISTEP  extends j1Shifter("multistep")
  // Full barrel shifter in both directions.
  object FULLBARREL extends j1Shifter("fullbarrel")
  // Instantiation of type class Enum[j1Shifter]
  implicit val `enum` = FiniteEnum(
    NOSHIFTER, MINIMAL, SINGLESTEP, MULTISTEP, FULLBARREL)
}

/* TODO: Perhaps add a sub-configuration object for the ISA. */

case class j1Config(
  datawidth: Int = 16,
  dstkDepth: Int = 5,
  rstkDepth: Int = 5,
  memsize: Int = 4096,
  use_bb_tdp: Boolean = false,
  signext: Boolean = false,
  protect: Boolean,
  protmem: Int = 0xff,
  shifter: j1Shifter = j1Shifter.FULLBARREL,
  stackchecks: Boolean,
  relbranches: Boolean,
  bank_insn: Boolean,
  halt_insn: Boolean,
  swap16_insn: Boolean,
  swap32_insn: Boolean) {

  // REVIEW: The configuration constraints may be a little arbitrary.
  // - e.g., larger values for datawidth may cause RAM synthesis problems
  require(4 to 128 contains datawidth)
  require(4 to 12 contains dstkDepth) // dstack size: 16..4096 elements
  require(4 to 12 contains rstkDepth) // rstack size: 16..4096 elements
  require(memsize <= 65536)           // memory size: 0..65536 words (128 KB)
  require(protmem <= 65536)           // lower protected memory size
  require(swap16_insn ==> datawidth >= 16)
  require(swap32_insn ==> datawidth >= 32)

  /* Same configuration but with use_bb_tdp enabled. */
  def with_bb_tdp: j1Config = {
    copy(use_bb_tdp = true)
  }

  /* Same configuration but with use_bb_tdp disabled. */
  def without_bb_tdp: j1Config = {
    copy(use_bb_tdp = false)
  }

  /* We use reflection to report the values of all fields. */
  def dump(): Unit = {
    Output.debug(getClass.getSimpleName + " is")
    val fields = getClass.getDeclaredFields
    fields.foreach {
      field =>
        Output.debug("." + field.getName + " == " + field.get(this).toString)
    }
  }

  /* We use reflection to report the values of all fields. */
  override def toString: String = {
    val fields = getClass.getDeclaredFields
    "j1Config(" + (
      fields.map {
        field => field.getName + "=" + field.get(this).toString
      }
    ).mkString(",") +
    ")"
  }
}

object j1Config {
  // Default configuration file to be read by load(). */
  val DEFAULT_CONFIG_FILE: String = "j1.conf"

  /* Create j1 configuration instance programmatically. */
  def apply(datawidth: Int,
            dstkDepth: Int,
            rstkDepth: Int,
            memsize: Int,
            use_bb_tdp: Boolean,
            signext: Boolean,
            protect: Boolean,
            protmem: Int,
            shifter: j1Shifter,
            stackchecks: Boolean,
            relbranches: Boolean,
            bank_insn: Boolean,
            halt_insn: Boolean,
            swap16_insn: Boolean,
            swap32_insn: Boolean) = {
    /* Create j1Config instance according to the above arguments. */
    new j1Config(datawidth,
                 dstkDepth,
                 rstkDepth,
                 memsize,
                 use_bb_tdp,
                 signext,
                 protect,
                 protmem,
                 shifter,
                 stackchecks,
                 relbranches,
                 bank_insn,
                 halt_insn,
                 swap16_insn,
                 swap32_insn)
  }

  /* Load properties from a given configuration file. */
  def load(filename: String = DEFAULT_CONFIG_FILE): j1Config = {
    val props = new Properties
    try {
      props.load(new FileInputStream(filename))
    }
    catch {
      case e: FileNotFoundException => {
        Output.critical(
          f"Configuration file missing: '${BLUE}${filename}${RESET}'")
      }
    }

    /* Read relevant properties from configuration file. */
    var datawidth = props.getIntProperty("j1.cpu.datawidth", 16, 4, 128)
    var dstkDepth = props.getIntProperty("j1.dstack.depth", 5, 4, 12)
    var rstkDepth = props.getIntProperty("j1.rstack.depth", 5, 4, 12)
    var memsize = props.getIntProperty("j1.memory.size", 4096, 0, 65536)
    var use_bb_tdp = props.getBooleanProperty("j1.memory.bbtpd", true)
    val signext = props.getBooleanProperty("j1.cpu.signext", false)
    val protect = props.getBooleanProperty("j1.cpu.protect", false)
    val protmem = props.getIntProperty("j1.cpu.protmem", 0xff, 0x1, 0xffff)
    val shifter = props.getEnumProperty[j1Shifter]("j1.cpu.shifter",
                                                   j1Shifter.FULLBARREL)
    val stackchecks = props.getBooleanProperty("j1.cpu.stackchecks", false)
    val relbranches = props.getBooleanProperty("j1.cpu.relbranches", false)
    val bank_insn = props.getBooleanProperty("j1.cpu.isa.bank", false)
    val halt_insn = props.getBooleanProperty("j1.cpu.isa.halt", false)
    val swap16_insn = props.getBooleanProperty("j1.cpu.isa.swap16", false)
    val swap32_insn = props.getBooleanProperty("j1.cpu.isa.swap32", false)

    /* Create corresponding j1 configuration instance. */
    j1Config(datawidth, dstkDepth, rstkDepth, memsize, use_bb_tdp,
             signext, protect, protmem, shifter, stackchecks, relbranches,
             bank_insn, halt_insn, swap16_insn, swap32_insn)
  }
}
