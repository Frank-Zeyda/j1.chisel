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

import scala.util.matching.Regex

object ParseUtils {
  /* Regexes for the parseInt(...) function. */
  private val zeroRegex = raw"(\+|-)?[ \t]*(0)+".r
  private val decRegex = raw"(\+|-)?[ \t]*([1-9][0-9]*)".r
  private val hexRegex = raw"(\+|-)?[ \t]*(?:0x|0X|h|\$$|#)([0-9a-fA-F]+)".r
  private val octRegex = raw"(\+|-)?[ \t]*(?:0|o)([0-7]+)".r
  private val binRegex = raw"(\+|-)?[ \t]*(?:0b|0B|b|%)([0-1]+)".r

  /* Parsing an integer supporting various common base formats. */
  def parseInt(numStr: String,
               min: BigInt = Int.MinValue,
               max: BigInt = Int.MaxValue): Int = {
    // preprocess numStr by trimming and removing underscores
    val strVal = numStr.trim().replaceAll("_", "")
    strVal match {
      case zeroRegex(sign, value) => {
        0
      }
      case decRegex(sign, value) => {
        var result = BigInt(value, 10)
        if (sign != null && sign.equals("-")) {
          result = -result
        }
        if (!(result >= min && result <= max)) {
          throw OutOfRangeException(numStr, 10, min, max)
        }
        result.toInt
      }
      case hexRegex(sign, value) => {
        var result = BigInt(value, 16)
        if (sign != null && sign.equals("-")) {
          result = -result
        }
        if (!(result >= min && result <= max)) {
          throw OutOfRangeException(numStr, 16, min, max)
        }
        result.toInt
      }
      case octRegex(sign, value) => {
        var result = BigInt(value, 8)
        if (sign != null && sign.equals("-")) {
          result = -result
        }
        if (!(result >= min && result <= max)) {
          throw OutOfRangeException(numStr, 8, min, max)
        }
        result.toInt
      }
      case binRegex(sign, value) => {
        var result = BigInt(value, 2)
        if (sign != null && sign.equals("-")) {
          result = -result
        }
        if (!(result >= min && result <= max)) {
          throw OutOfRangeException(numStr, 2, min, max)
        }
        result.toInt
      }
      case _ => {
        throw new NumberFormatException(s"For input string: \"${numStr}\"")
      }
    }
  }

  /* Regexes for the parseBoolean(...) function (case-insensitive). */
  private val trueRegex  = raw"(?i)(true|yes|y|1)".r
  private val falseRegex = raw"(?i)(false|no|n|0)".r

  /* Parsing a boolean literal supporting various common nomencaltures. */
  def parseBoolean(boolStr: String): Boolean = {
    // preprocess boolStr by trimming
    val strVal = boolStr.trim()
    strVal match {
      case  trueRegex(_) => true
      case falseRegex(_) => false
      case _ => {
        throw new NumberFormatException(s"For input string: \"${boolStr}\"")
      }
    }
  }
}
