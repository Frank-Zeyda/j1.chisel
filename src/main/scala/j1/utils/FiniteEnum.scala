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

object FiniteEnum {
  /* Type class for small (finite) enumerations. */
  trait Enum[T] {
    val universe: Set[T]
  }

  /* Facilitates instantion of the Enum[T] type class. */
  def apply[T](values: T*): Enum[T] = {
    new Enum[T] { val universe = values.toSet }
  }

  /* Implicit exentions methods of implementing classes. */
  object Implicits {
    implicit class EnumOps[T](x: T)(implicit enumInst: Enum[T]) {
      val values: Set[T] = enumInst.universe
    }
  }
}
