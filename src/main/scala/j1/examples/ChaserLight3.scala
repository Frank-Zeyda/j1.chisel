package j1.examples

import j1.miniasm.j1Asm

object ChaserLight3 extends j1Asm {
  /* Main Program */
  label("start")
  push(1)
  call("!led")
  call("delay")
  push(2)
  call("!led")
  call("delay")
  push(4)
  call("!led")
  call("delay")
  push(2)
  call("!led")
  call("delay")
  jmp("start")
  /* Subroutines */
  /* Write state of LEDS */
  label("!led")
  push(0x0000)
  iostore.x
  /* Delay execution by approx. 1s (at 50 MHz clock) */
  label("delay")
  push(-1)
  push(1000)
  label("delay.loop")
  call("wait.1ms")
  plus.^
  dup
  jpz("delay.exit")
  jmp("delay.loop")
  label("delay.exit")
  drop
  drop.x
  /* Wait for precisely 1 ms (at 50 MHz clock, call cycle included) */
  label("wait.1ms")
  push(-1)
  push(12498)        // 12500 iteration corresponds to 1 ms since
  label("wait.loop") // each loop takes 4 cycles == 80 ns at 50 MHz.
  plus.^
  dup
  jpz("wait.exit")
  jmp("wait.loop")
  label("wait.exit")
  noop
  drop
  drop
  exit
  done
}

/* Alternative version using bit-shifting operations. */
object AltChaserLight3 extends j1Asm {
  /* Main Program */
  push(1)
  label("start")
  dup
  call("!led")
  call("delay")
  call("lshift")
  dup
  call("!led")
  call("delay")
  call("lshift")
  dup
  call("!led")
  call("delay")
  call("rshift")
  dup
  call("!led")
  call("delay")
  call("rshift")
  jmp("start")
  /* Subroutines */
  /* Write state of LEDS */
  label("!led")
  push(0x0000)
  iostore.x
  /* Left-shift by one place */
  label("lshift")
  push(1)
  lshift.x
  /* Right-shift by one place */
  label("rshift")
  push(1)
  rshift.x
  done
  /* Delay execution by approx. 1s (at 50 MHz clock) */
  label("delay")
  push(-1)
  push(1000)
  label("delay.loop")
  call("wait.1ms")
  plus.^
  dup
  jpz("delay.exit")
  jmp("delay.loop")
  label("delay.exit")
  drop
  drop.x
  /* Wait for precisely 1 ms (at 50 MHz clock, call cycle included) */
  label("wait.1ms")
  push(-1)
  push(12498)        // 12500 iteration corresponds to 1 ms since
  label("wait.loop") // each loop takes 4 cycles == 80 ns at 50 MHz.
  plus.^
  dup
  jpz("wait.exit")
  jmp("wait.loop")
  label("wait.exit")
  noop
  drop
  drop
  exit
  done
}
