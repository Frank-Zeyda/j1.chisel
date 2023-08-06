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
package j1.utils

import java.util.Properties

import scala.io.AnsiColor._

/* Allows for a generic treatment of enumerations in property files. */
trait PropValue {
  val propValue: String
}

object Extensions {
  /* Extensions to Boolean type */
  implicit class RichBoolean(val flag: Boolean) {
    def ==> (that: Boolean) = {
      !flag || that
    }
  }

  /* Extensions to Short type */
  implicit class RichShort(val num: Short) {
    def toHexString = {
      f"${num.toInt & 0xFFFF}%04X"
    }

    def toBinString = {
      val binStr = (num.toInt & 0xFFFF).toBinaryString
      ("0" * (16 - binStr.length)) + binStr
    }
  }

  /* Extensions to BigInt class */
  implicit class RichBigInt(val num: BigInt) {
    // Reinterprets a BigInt in the context of a given (bit)width.
    // The permissible range for self is -2^(width-1) to 2^(width)-1.
    // The result will be in the range -2^(width-1) to 2^(width-1)-1.
    def reinterp(width: Int): BigInt = {
      val min: BigInt = -(BigInt(2).pow(width - 1))
      val max: BigInt =   BigInt(2).pow(width) - 1;
      require(num >= min && num <= max)
      var result = num
      if (num >= 0) {
        val msb = width - 1
        if (num.testBit(msb)) {
          result = min + num.clearBit(msb)
        }
      }
      result
    }
  }

  /* Additional methods for type-class Numeric[T] */
  implicit class MoreNumericOps[T](val num: T)(implicit numeric: Numeric[T]) {
    def isSInt: Boolean = {
      import numeric.mkNumericOps
      num.toLong >= -(1L << 31) && num.toLong < (1L << 31)
    }

    def isUInt: Boolean = {
      import numeric.mkNumericOps
      num.toLong >= 0 && num.toLong < (1L << 32)
    }
  }

  /* Extensions to String class */
  implicit class RichString(s: String) {
    def removePrefix(prefix: String) = {
      if (s.startsWith(prefix))
        s.substring(prefix.length, s.length)
      else s
    }

    def removeSuffix(suffix: String) = {
      if (s.endsWith(suffix))
        s.substring(0, s.length - suffix.length)
      else s
    }
  }

  /* Extensions to Properties class */
  implicit class RichProperties(props: Properties) {
    /* Note that pruneComments() can handle nested comments too. */
    private def pruneComments(value: String) = {
      var result = value
      /* Remove enclosing (multi-line) comments */
      val ENCLOSING_COMMENT = raw"/\*(?!/\*)(.)*?\*/"
      var tmp = result.replaceAll(ENCLOSING_COMMENT, "")
      /* We need several iterations to remove nested comments. */
      while (!tmp.equals(result)) {
        result = tmp
        tmp = result.replaceAll(ENCLOSING_COMMENT, "")
      }
      /* Remove single-line comment at end-of-line. */
      result.split("//", 2)(0)
    }

    /* Parse value of Int property, supporting BIN/OCT/DEC/HEX format. */
    def getIntProperty(key: String,
                       default: Int,
                       min: BigInt = Int.MinValue,
                       max: BigInt = Int.MaxValue): Int = {
      var result: Option[Int] = None
      var strVal = pruneComments(props.getProperty(key))
      if (strVal != null) {
        try {
          result = Some(ParseUtils.parseInt(strVal.trim(), min, max))
        }
        catch {
          case e: NumberFormatException => {
            // REVIEW: Raise an Output.critical error here?
            Output.warn(s"Invalid value ${RED}${strVal}${RESET} " +
              s"for property ${BOLD}${key}${RESET} in configuration.")
          }
          case e: OutOfRangeException[Int] @unchecked => {
            // REVIEW: Raise an Output.critical error here?
            Output.warn(s"Value ${RED}${strVal}${RESET} " +
              s"for property ${BOLD}${key}${RESET} in configuration " +
              s"outside permissible range: [${e.min}..${e.max}].")
          }
        }
      }
      result match {
        case Some(value) => value
        case None => {
          Output.warn(s"Using default value ${GREEN}${default}${RESET} " +
            s"for ${BOLD}${key}${RESET} configuration property.")
          default
        }
      }
    }

    /* Parse value of Boolean property. */
    def getBooleanProperty(key: String, default: Boolean): Boolean = {
      var result: Option[Boolean] = None
      var strVal = pruneComments(props.getProperty(key))
      if (strVal != null) {
        try {
          result = Some(ParseUtils.parseBoolean(strVal.trim()))
        }
        catch {
          case e: NumberFormatException => {
            // REVIEW: Raise an Output.critical error here?
            Output.warn(s"Invalid value ${RED}${strVal}${RESET} " +
              s"for property ${BOLD}${key}${RESET} in configuration.")
          }
        }
      }
      result match {
        case Some(value) => value
        case None => {
          Output.warn(s"Using default value ${GREEN}${default}${RESET} " +
            s"for ${BOLD}${key}${RESET} configuration property.")
          default
        }
      }
    }

    /* Parse value of an enumeration property (FiniteEnum.Enum[T]). */
    def getEnumProperty[T <: PropValue](key: String, default: T)
                           (implicit `enum`: FiniteEnum.Enum[T]) = {
      var result: Option[T] = None
      var strVal = pruneComments(props.getProperty(key))
      if (strVal != null) {
        // preprocess numStr by trimming
        val propVal = strVal.trim()
        val values = `enum`.universe // enumeration values
        result = values.find(_.propValue.equalsIgnoreCase(propVal))
        if (!result.isDefined) {
          Output.warn(s"Invalid value ${RED}${propVal}${RESET} " +
            s"for property ${BOLD}${key}${RESET} in configuration.")
          Output.warn(s"Permissible values are: [" +
            values.map("\"" + _.propValue + "\"").mkString(", ") + "]")
        }
      }
      result match {
        case Some(value) => value
        case None => {
          Output.warn(
            s"Using default value ${GREEN}${default.propValue}${RESET} " +
            s"for ${BOLD}${key}${RESET} configuration property.")
          default
        }
      }
    }
  }
}
