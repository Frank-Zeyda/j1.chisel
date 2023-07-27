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
