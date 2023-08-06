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

import scala.collection.mutable.Set

import Validation._

// NOTE: We assume that labels are only located once in the life-time.

class Label(val name: String)(implicit memIf: MemInterface) {
  // Address of this label, or None if the label is yet unlocated.
  private var location: Option[Int] = None

  // Dynamically recordc callers of this label (JMP, JPZ or CALL).
  private val callers: Set[Int] = Set() // set of caller addresses

  // Returns the address of the label, or None prior to location.
  def address: Option[Int] = location

  // Fills in the target address of a caller of this label.
  private def resolve(caller: Int, target: Int) = {
    require(isValidAddr(caller))
    require(isValidTarget(target))
    isBranchInsn(memIf.readMem(caller))
    val insn_mask = memIf.readMem(caller) & 0xE000
    memIf.writeMem(caller, (insn_mask | target).toShort)
  }

  // Locate this label to a particular address in program memory.
  def locate(target: Int) = {
    require(!location.isDefined)
    require(isValidTarget(target))
    location = Some(target)
    callers.foreach {
      caller => resolve(caller, target)
    }
  }

  // Record the address of a caller of this label (branch instruction).
  def calledFrom(source: Int) = {
    require(isValidAddr(source))
    isBranchInsn(memIf.readMem(source))
    callers.add(source)
    // Set branch address immediately if the label is already located.
    if (location.isDefined) {
      resolve(source, location.get)
    }
  }
}
