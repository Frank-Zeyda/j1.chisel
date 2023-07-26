package j1.chisel

import j1.utils.Extensions.RichProperties
import j1.utils.Output

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

import scala.io.AnsiColor._

/* We assume a data/io memory space of no more than 65536 words. */

case class j1Config(
  datawidth: Int = 16,
  dstkDepth: Int = 5,
  rstkDepth: Int = 5,
  memsize: Int = 4096,
  use_bb_tdp: Boolean = false) {

  // REVIEW: The configuration constraints may be a little arbitrary.
  // - e.g., larger values for datawidth may cause RAM synthesis problems
  require(4 to 128 contains datawidth)
  require(4 to 12 contains dstkDepth) // dstack size: 16..4096 elements
  require(4 to 12 contains rstkDepth) // rstack size: 16..4096 elements
  require(memsize <= 65536)           // memory size: 0..65536 words (128 KB)

  def with_bb_tdp: j1Config = {
    copy(use_bb_tdp = true)
  }

  def without_bb_tdp: j1Config = {
    copy(use_bb_tdp = false)
  }

  /* We use reflection to report the values of all fields. */
  def dump(): Unit = {
    val fields = getClass.getDeclaredFields
    Output.debug(getClass.getSimpleName + " is")
    fields.foreach { field =>
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

  def apply(datawidth: Int,
            dstkDepth: Int,
            rstkDepth: Int,
            memsize: Int,
            use_bb_tdp: Boolean) = {
    new j1Config(datawidth, dstkDepth, rstkDepth, memsize, use_bb_tdp)
  }

  def load(filename: String = DEFAULT_CONFIG_FILE): j1Config = {
    /* Load properties from the given configuration file. */
    val props = new Properties
    try {
      props.load(new FileInputStream(filename))
    }
    catch {
      case e: FileNotFoundException => {
        Output.critical(
          f"Failed to load configuration file: '${BLUE}${filename}${RESET}'")
      }
    }

    /* Read relevant properties from configuration file. */
    var datawidth = props.getIntProperty("j1.cpu.datawidth", 16, 4, 128)
    var dstkDepth = props.getIntProperty("j1.dstack.depth", 5, 4, 12)
    var rstkDepth = props.getIntProperty("j1.rstack.depth", 5, 4, 12)
    var memsize = props.getIntProperty("j1.memory.size", 4096, 0, 65536)
    var use_bb_tdp = props.getBooleanProperty("j1.memory.bbtpd", true)

    /* Create corresponding j1 configuration instance. */
    j1Config(datawidth, dstkDepth, rstkDepth, memsize, use_bb_tdp)
  }
}
