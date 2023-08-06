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

import InsnMask.{JMP, JPZ, CALL, ALU}

/* TODO: Take variant configuration into account to fine-tune checks:
 * - branches could consider the amount of available memory
 * - there may me some ALU instructions that are ill-formed */

object Validation {
  def isValidAddr(addr: Int): Boolean =
    0x0 to 0xFFFF contains addr

  def isValidInsn(insn: Int): Boolean =
    0x0 to 0xFFFF contains insn

  def isValidImm(value: Int): Boolean =
    0x0 to 0x7FFF contains value

  def isValidTarget(target: Int): Boolean =
    0x0 to 0x1FFF contains target

  def isBranchInsn(insn: Int): Boolean =
    isValidInsn(insn) &&
    (Seq(JMP, JPZ, CALL) contains (insn & 0xE000))

  def isAluInsn(insn: Int): Boolean =
    isValidInsn(insn) && (insn & 0xE000) == ALU
}
