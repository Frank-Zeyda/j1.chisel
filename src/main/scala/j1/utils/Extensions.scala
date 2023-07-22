package j1.utils

object Extensions {
  /* Extensions to BigInt type */
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
}
