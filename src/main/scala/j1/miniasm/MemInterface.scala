package j1.miniasm

/* Memory interface for reading / writing the J1 program memory. */

trait MemInterface {
  // Total number of memory addresses.
  val memSize: Int

  // Reads memory word at address addr.
  def readMem(addr: Int): Short

  // Writes memory word at address addr. Returns previous content.
  def writeMem(addr: Int, value: Short): Short
}
