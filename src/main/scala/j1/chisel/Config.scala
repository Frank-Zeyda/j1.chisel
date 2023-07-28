package j1.chisel

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
    getClass.getSimpleName.removeSuffix("$")
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

case class j1Config(
  datawidth: Int = 16,
  dstkDepth: Int = 5,
  rstkDepth: Int = 5,
  memsize: Int = 4096,
  use_bb_tdp: Boolean = false,
  signext: Boolean = false,
  shifter: j1Shifter = j1Shifter.FULLBARREL) {

  // REVIEW: The configuration constraints may be a little arbitrary.
  // - e.g., larger values for datawidth may cause RAM synthesis problems
  require(4 to 128 contains datawidth)
  require(4 to 12 contains dstkDepth) // dstack size: 16..4096 elements
  require(4 to 12 contains rstkDepth) // rstack size: 16..4096 elements
  require(memsize <= 65536)           // memory size: 0..65536 words (128 KB)

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
            shifter: j1Shifter) = {
    new j1Config(
      datawidth, dstkDepth, rstkDepth, memsize, use_bb_tdp, signext, shifter)
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
    val shifter = props.getEnumProperty[j1Shifter]("j1.cpu.shifter",
                                                   j1Shifter.FULLBARREL)

    /* Create corresponding j1 configuration instance. */
    j1Config(
      datawidth, dstkDepth, rstkDepth, memsize, use_bb_tdp, signext, shifter)
  }
}
