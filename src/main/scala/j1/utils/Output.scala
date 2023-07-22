package j1.utils

import scala.io.AnsiColor._

object Output {
  /* Emits informative message. */
  def info(obj: Any): Unit = {
    Console.print(s"[${BLUE}info${RESET}] ")
    println(obj)
  }

  /* Emits debugging message. */
  def debug(obj: Any): Unit = {
    Console.print(s"[${CYAN}debug${RESET}] ")
    println(obj)
  }

  /* Emits warning message. */
  def warn(obj: Any): Unit = {
    Console.print(s"[${YELLOW}warn${RESET}] ")
    println(obj)
  }

  /* Emits error message. */
  def error(obj: Any): Unit = {
    Console.print(s"[${RED}error${RESET}] ")
    println(obj)
  }

  /* Emits error message and terminates the program. */
  def critical(obj: Any): Unit = {
    Console.print(s"[${RED}critical${RESET}] ")
    println(obj)
    System.exit(1)
  }

  /* Internal method for printing of an object. */
  private def println(obj: Any): Unit = {
    val text = obj.toString
    Console.print(text)
    if (!text.endsWith("\n")) {
      Console.println()
    }
  }
}
