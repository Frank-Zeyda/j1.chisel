package j1.utils

class OutOfRangeException[T](val min: T, val max: T)
  extends Exception(s"${min} .. ${max}") {
}

object OutOfRangeException {
  def apply[T](min: T, max: T) = {
    new OutOfRangeException(min, max)
  }

  // Mimicking the message of NumberFormatException
  def apply[T](numStr: String, radix: Int, min: T, max: T) = {
    new OutOfRangeException(min, max) {
      override def getMessage(): String = {
        s"For input string: \"${numStr.trim()}\"" +
        (if (radix == 10) "" else f" under radix ${radix}") +
        s" (range ${min} .. ${max})"
      }
    }
  }
}
