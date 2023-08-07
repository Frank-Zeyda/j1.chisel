# Chisel conversion of the J1 CPU for FPGA

This repository contains a conversion of James Bowman's [J1](https://github.com/jamesbowman/j1) [CPU](https://hackaday.com/2010/12/01/j1-a-small-fast-cpu-core-for-fpga/) for FPGA into the [Chisel](https://www.chisel-lang.org/) Scala-based hardware description language. It includes a richer feature set than the original J1 (see section [CPU Configuration](#cpu-configuration) below), albeit aiming to stay close to James Bowman's original easy-to-follow [Verilog design](https://github.com/jamesbowman/j1/blob/master/verilog/j1.v) in the Chisel implementation. An instance of the CPU can be generated that ought be 100% compatible with James Bowman's J1, as it was described in the paper: [J1: a small Forth CPU Core for FPGAs](https://excamera.com/files/j1.pdf).

## Rationale for this work

On the one hand, the conversion serves as a teaching vehicle for Product Line Engineering ([PLE](https://productlineengineering.com/resources/what-is-product-line-engineering/)), Design Space Exploration ([DSE](https://en.wikipedia.org/wiki/Design_space_exploration)), and Rigorous Digital Engineering ([RDE](https://galois.com/services/rigorous-digital-engineering/)) in Chisel, exemplifying the advantages of Chisel over plain Verilog / VHDL in this context. On the other, it is an attempt to make the J1 CPU more viable for industrial-scale deployment. A tool-chain for remote compilation and testing is currently under development (see [Future Work Roadmap](#future-work-roadmap) section), as well as a pluggable component framework that facilitates the generation of J1-based SoC, similar to the [Chipyard architecture](https://chipyard.readthedocs.io/en/stable/). This project will soon be accompanied by teaching and training material related to the aforementioned concerns.

This repository currently includes:
- The Chisel code for generating configurable J1 CPU instances.
- A simple reference design, tested on a low-cost Cyclone II FPGA ([EP2C5T144](https://hobbycomponents.com/altera/819-altera-cyclone-ii-es2c5t144-fpga-dev-board)) and using Quartus II [13.1](https://www.intel.com/content/www/us/en/software-kit/666220/intel-quartus-ii-web-edition-design-software-version-13-1-for-linux.html?) for synthesis.
- A J1 mini-assembler and disassembler as a shallowly embedded DSL (E-DSL) into Scala.
- An extended [PeekPokeTester](https://github.com/ucb-bar/chiseltest/blob/main/src/main/scala/chiseltest/iotesters/PeekPokeTester.scala) based on [chiseltest](https://github.com/ucb-bar/chiseltest) to facilitate stepwise execution of the CPU.

*Please watch this repository, more content, bug fixes and material will likely be added in the next few weeks & months ...*

*TODO: I am currently running Quartus II [13.0sp1](https://www.intel.com/content/www/us/en/software-kit/711790/intel-quartus-ii-web-edition-design-software-version-13-0sp1-for-linux.html). Check that all works with [13.1](https://www.intel.com/content/www/us/en/software-kit/666220/intel-quartus-ii-web-edition-design-software-version-13-1-for-linux.html?), as hinted above.*

# Prerequisites

The [sbt](https://www.scala-sbt.org/) tool is require to compile the Scala/Chisel sources, generate the hardware design and example program, and perform simulation-based testing.
- To compile the generator source code, execute `sbt compile`.
- To generate the complete Verilog design for a smoke test (3-LED chaser light), execute `sbt run`.
- To execute a simple CPU simulation using [chiseltest](https://github.com/ucb-bar/chiseltest), execute `sbt test`.

Note that `sbt` ought to automatically fetch all required [Scala 2.13.10](https://github.com/scala/scala/releases/tag/v2.13.10) and [Chisel 5.0.0](https://github.com/chipsalliance/chisel/) dependencies from the respective online repositories, including [ScalaTest](https://www.scalatest.org/) and [Chiseltest](https://github.com/ucb-bar/chiseltest). All generated Verilog files and the memory initialization file are output to the `generated` subfolder, which is created by `sbt run`.

In addition, it is necessary to install a recent version of the [firtool](https://github.com/llvm/circt/releases) (version 1.44.0 or newer) and add its location to the `PATH` environment variable, so that the Chisel/Chiseltest run-time can find it!

# Files and Folders

Below is an overview of all files and folders currently located inside the repository, with a brief description.

| Path/File | Description |
| --------- | ----------- |
| [README.md](README.md) | This mark-down project documentation |
| [build.sbt](build.sbt) | Build configuration for the `sbt` tool |
| [j1.conf](j1.conf) | J1 CPU instance configuration file |
| [meminit.hex](meminit.hex) | Default memory initialization file (*) |
| [.gitignore](.gitignore) | Local files and folders ignored by `git` by default |
| [project/...](project/) | Further sbt settings and dynamic project files |
| ↳ [project/build.properties](project/build.properties) | Further sbt setting: *build properties* |
| ↳ [project/plugins.sbt](project/plugins.sbt) | Further sbt settings: *plugin options* |
| [src/...](src/) | All Chisel & Scala source files |
| ↳ [src/main/scala/j1/chisel/...](src/main/scala/j1/chisel/) | Chisel sources for the J1 CPU and reference design |
| &nbsp; ↳ [src/main/scala/j1/chisel/Config.scala](src/main/scala/j1/chisel/Config.scala) | Encodes J1 CPU configurations and reads them from settings files |
| &nbsp; ↳ [src/main/scala/j1/chisel/Memory.scala](src/main/scala/j1/chisel/Memory.scala) | Chisel design for J1 TDP Memory (incl. [black-box](https://www.intel.com/content/www/us/en/docs/programmable/683082/22-3/true-dual-port-synchronous-ram.html) variant) |
| &nbsp; ↳ [src/main/scala/j1/chisel/Stack.scala](src/main/scala/j1/chisel/Stack.scala) | Chisel design for J1 data and return stack memories |
| &nbsp; ↳ [src/main/scala/j1/chisel/System.scala](src/main/scala/j1/chisel/System.scala) | Chisel reference design for the chaser-light smoke test |
| &nbsp; ↳ [src/main/scala/j1/chisel/Utils.scala](src/main/scala/j1/chisel/Utils.scala) | Chisel utilities for **Regs** and **Wires** |
| &nbsp; ↳ [src/main/scala/j1/chisel/j1.scala](src/main/scala/j1/chisel/j1.scala) | Chisel design of the J1 CPU product line |
| ↳ [src/main/scala/j1/examples/...](src/main/scala/j1/examples/) | Example programs using the mini-assembler for compilation |
| &nbsp; ↳ [src/main/scala/j1/examples/ChaserLight3.scala](src/main/scala/j1/examples/ChaserLight3.scala) | 3-LED chaser light program (in two variants) |
| ↳ [src/main/scala/j1/miniasm/...](src/main/scala/j1/miniasm/) | Sources for the J1 mini-assembler / disassembler |
| &nbsp; ↳ [src/main/scala/j1/miniasm/InsnEnc.scala](src/main/scala/j1/miniasm/InsnEnc.scala) | Components related to J1 instruction encoding and ISA |
| &nbsp; ↳ [src/main/scala/j1/miniasm/Label.scala](src/main/scala/j1/miniasm/Label.scala) | Class for recording dynamic labels during compilation |
| &nbsp; ↳ [src/main/scala/j1/miniasm/MemInterface.scala](src/main/scala/j1/miniasm/MemInterface.scala) | Memory interface trait used for program deployment |
| &nbsp; ↳ [src/main/scala/j1/miniasm/MiniAsm.scala](src/main/scala/j1/miniasm/MiniAsm.scala) | J1 mini-assembler and disassembler components (E-DSL) |
| &nbsp; ↳ [src/main/scala/j1/miniasm/Validation.scala](src/main/scala/j1/miniasm/Validation.scala) | Utility functions for data validation |
| ↳ [src/main/scala/j1/utils/...](src/main/scala/j1/utils/) | Extensions classes and generic utility components |
| &nbsp; ↳ [src/main/scala/j1/utils/Exceptions.scala](src/main/scala/j1/utils/Exceptions.scala) | Exceptions used by various components |
| &nbsp; ↳ [src/main/scala/j1/utils/Extensions.scala](src/main/scala/j1/utils/Extensions.scala) | Various (implicit) Scala extension classes |
| &nbsp; ↳ [src/main/scala/j1/utils/FiniteEnum.scala](src/main/scala/j1/utils/FiniteEnum.scala) | Finite enumeration type class (trait) |
| &nbsp; ↳ [src/main/scala/j1/utils/Output.scala](src/main/scala/j1/utils/Output.scala) | Basic utility for producing log output to the Console |
| &nbsp; ↳ [src/main/scala/j1/utils/Parsing.scala](src/main/scala/j1/utils/Parsing.scala) | Utility functions for parsing numbers and booleans |
| ↳ [src/test/scala/j1/...](src/test/scala/j1/) | Simple testing framework for simulated CPU execution |
| &nbsp; ↳ [src/test/scala/j1/j1PeekPokeTester.scala](src/test/scala/j1/j1PeekPokeTester.scala) | [PeekPokerTester](https://github.com/ucb-bar/chiseltest/blob/main/src/main/scala/chiseltest/iotesters/PeekPokeTester.scala) extension tailored for testing J1 designs |
| &nbsp; ↳ [src/test/scala/j1/j1Test.scala](src/test/scala/j1/j1Test.scala) | Sample test illustrating simulation of the reference design |

(*) This `meminit.hex` file is actually not used by the reference design smoke test (on target) and neither the CPU simulation example.

# CPU Configuration

The generated J1 CPU instance is configured via the properties file [`j1.conf`](j1.conf).

⇨ Such is read in and parsed during generation of the reference design (`sbt run`) and example simulation test (`sbt test`).

The available configuration options are listed with their permissible value ranges below.

| Settings Key | Range of Values | Default Value | Short Description |
| ------------ | --------------- | ------------- | ----------------- |
| `j1.cpu.datawidth` | `16`..`256` | `16` | Bitwidth of data/return stack cells and thus operations. |
| `j1.cpu.signext` | `yes`/`no` or `true`/`false` | `no` | Enables sign-extension of immediate pushes. |
| `j1.cpu.protect` | `yes`/`no` or `true`/`false` | `no` | Enables lower-memory protection via an added `PROTECT` instruction. |
| `j1.cpu.protmem` | `yes`/`no` or `true`/`false` | `0x7F` | Lower-memory limit to which protection applies. |
| `j1.cpu.shifter` | `none`, `minimal`, `singlestep`, `multistep`, or `fullbarrel` | `fullbarrel` | Type of bit shifter to be deployed. |
| `j1.cpu.stackchecks` | `yes`/`no` or `true`/`false` | `yes` | Enables run-time stack checks in hardware. |
| `j1.cpu.relbranches` | `yes`/`no` or `true`/`false` | `no` | Enables support for relative branches. |
| `j1.cpu.isa.bank` | `yes`/`no` or `true`/`false` | `no` | Extends absolute branch targets to a full 16-bit code space, via the `BANK <N>` instruction. |
| `j1.cpu.isa.halt` | `yes`/`no` or `true`/`false` | `no` | Includes a `HALT` instruction to stop the machine. |
| `j1.cpu.isa.swap16` | `yes`/`no` or `true`/`false` | `no` | Includes a `SWAP16` instruction to swap the lower bytes of the TOS and push the result. |
| `j1.cpu.isa.swap32` | `yes`/`no` or `true`/`false` | `no` | Includes a `SWAP32` instruction to swap the lower 16-bit words of the TOS and push the result. |
| `j1.memory.size` | `128`..`65536` | `4096` | Amount of deployed memory for code and data, given in 16 bit words. |
| `j1.memory.bbtpd` | `yes`/`no` or `true`/`false` | `yes` | Uses a black-box Verilog design for TDP RAM. |
| `j1.dstack.depth` | `4`..`12` | `5` | Depth of the data stack in bits. |
| `j1.rstack.depth` | `4`..`12` | `5` | Depth of the return stack in bits. |

A few additional notes on each setting follow:
- `j1.cpu.datawidth` is the effective bitwidth of the machine. The lower limit of 16 originates from the requirement that the data and return stack need to record addresses, which may require 16 bit. The upper limit could potentially be raised (?)
- `j1.cpu.signext` changes the value range of immediate pushes from 0..32767 to -16384..16383 (immediate values on the J1 are always encoded via 15 bits). This means a negative value can be pushed with a single instruction rather than an immediate instruction followed by `INVERT`.
- `j1.cpu.protect` enables protection of a lower memory segment `0`..`N`. After a reboot, data can be written into that segment. A supplied `PROTECT` instruction prohibits further writes to it. Typically, that segment may host a UART boot-loader.
- `j1.cpu.protmem` determines the lower-memory bound `N` that applies for memory protection (see above).
- `j1.cpu.shifter` permits different kinds of bit shifters to be deployed. Shifting is typically a resource-expensive operation, so using a less powerful shifter can reduce LE/LUT count considerably. The available options are:
  1. `none`: no bit shifter is to be deployed;
  2. `minimal`: a minimal shifter that only shifts a single step to the right (left shifts can be efficiently emulated with `DUP PLUS`;
  3. `singlestep`: a single-step shifter that shifts in both directions;
  4. `multistep`: a multi-step shifter that shifts either 1, 4, or 8 bits in both directions;
  5. `fullbarrel`: a full barrel shifter (in both directions).
- `j1.cpu.stackchecks` enables run-time stack checks for overflow and underflow, of both the data and return stack. If a stack error is detected, the machine goes into a **halting state** and outputs a three-bit error code on the `status` output signal to identify the fault. (See the [Reference Design](#reference-design) section for a detailed account on fault signals.)
- `j1.cpu.relbranches` enables support for relative branches at a distance of `-2048`..`2047` from the current `PC`. This feature takes up one bit of the default 13 bit address target range of branch instructions. Hence, the range for *absolute* branches shrinks from `0`..`8191` to `0`..`4095` but can be extended again to `0`..`65535` if the `j1.cpu.isa.bank` option is enabled. Relative branches are executed in one cycle.
- `j1.cpu.isa.bank` provides a `BANK 0..15` instruction that extends the target range of absolute branches to `0`..`65535` (128 KB of effective code memory). Far branches hence take two cycles rather than one. A preceding `BANK` instructions determines the upper 4 bits of the target address. (**NOTE**: The bank register is automatically reset to `0` after one cycle.)
- `j1.cpu.isa.halt` adds a `HALT` instruction to halt execution of the CPU and output `b111` on the status signal. Note that halting can be emulated in software via a `JMP` to the current address. Only a reset revives the CPU after halting.
- `j1.cpu.isa.swap16` adds a `SWAP16` instruction that swaps the lower two bytes of the TOS and pushes the result on the data stack. Other bits greater than 16 of the TOS value are just copied over.
- `j1.cpu.isa.swap32` adds a `SWAP32` instruction that swaps the lower two (16 bit) words of the TOS and pushes the result on the data stack. Other bits greater than 32 of the TOS value are just copied over. Note that this option is only available if `j1.cpu.datawidth` is equal than or greater to `32`.
- `j1.memory.size` Size of the combined code/data memory. Note that the J1 CPU can address at most 65536 memory words (128 KB), i.e., if `j1.cpu.isa.bank` and/or `j1.cpu.relbranches` is/are enabled. Otherwise, the maximum addressable memory is 8192 words (16 KB), as per the original J1 design.
- `j1.memory.bbtpd` uses a Verilog black-box model for the True Dual-Ported RAM of the reference design. This is desired for synthesis since EDA tools seem to have trouble with synthesizing TDP RAM from Chisel's `SyncReadMem` module. For testing and simulation, however, we want to set this to `no` or `false`.
- `j1.dstack.depth` Depth (in bits) of the data stack. The stack size corresponds to (2^`j1.dstack.depth`) + 1 (since the TOS is kept in a separate register by the CPU core).
- `j1.rstack.depth` Depth (in bits) of the return stack. The stack size corresponds to 2^`j1.rstack.depth`.

In order to fully appreciate the above options and extensions to the J1 CPU core, the reader is encouraged to study the original J1 [paper](https://excamera.com/files/j1.pdf) and [Verilog design](https://github.com/jamesbowman/j1/blob/master/verilog/xilinx-top.v) of James Bowman. The following configuration ought indeed be fully compatible with the original J1:

```
j1.cpu.datawidth: 32
j1.cpu.signext: no
j1.cpu.protect: no
j1.cpu.protmem: 0xff
j1.cpu.shifter: fullbarrel
j1.cpu.stackchecks: no
j1.cpu.relbranches: no
j1.cpu.isa.bank: no
j1.cpu.isa.halt: no
j1.cpu.isa.swap16: no
j1.cpu.isa.swap32: no
j1.memory.size: 8192
j1.memory.bbtpd: yes
j1.dstack.depth: 4
j1.rstack.depth: 4
```

*Disclaimer: At this stage, many of the above features have not been (extensively) tested yet, and the mini-assembler (see below) is not aware of their configuration either. If you find issues, bugs, or unexpected behaviors, please do not despair and let the developer(s) know &mdash; we shall be trying to address them all in the near future.*

## A note on the settings key and value format

### Setting keys

Settings keys are case-sensitive and typically have to be written in lower case.

Note that unknown keys are silently ignored.

### Setting values

Numbers can be given in
- decimal,
- hexadecimal (prefix `0x`, `0X`, `h`, `$` or `#`),
- octal (prefix `0` or `o`), or
- binary (prefix `0b`, `0B`, `b` or `%`)

format. (A `+`/`-` symbol always precedes the prefix.)

Numeric values are typically associated with ranges, and the tool checks those upon processing a J1 settings file.

For Booleans, the following symbolic values are permissible:
- `yes` or `no`
- `true` or `false`
- `0` or `1`

Both, upper- and lowercase names are permitted here.

# Verilog Generation

As already noted, generation of the [SystemVerilog](https://en.wikipedia.org/wiki/SystemVerilog) files for the reference design is triggered via `sbt run`. The respective code to carry out generation is included in the [System.scala](src/main/scala/j1/chisel/System.scala) Chisel source file and recaptured by the following code fragment.

```scala
package j1.chisel

import j1.examples.ChaserLight3

import circt.stage.ChiselStage

object j1SystemGen extends App {
  /* Step 1: Load design configuration from j1.conf properties file. */
  /* For generation, ensure that we are using black-box TDP RAM. */
  implicit val cfg = j1Config.load("j1.conf").with_bb_tdp

  /* Step 2: Generate SystemVerilog files for the Chisel design. */
  ChiselStage.emitSystemVerilogFile(new j1System,
    Array("--target-dir", "dummy"),
    Array("--strip-debug-info",
          "--disable-all-randomization",
          "--split-verilog", "-o", "generated"))

  /* Step 3: Create memory initialization file for the example program. */
  ChaserLight3.writehex("generated/meminit.hex")
}
```

Note that for generation, we augment [j1.conf](j1.conf) above to ensure that the `j1.memory.bbtpd` setting is set to `true`.

For compilation and deployment via a given EDA tool, all `*.sv` files output to the local [generated](generated) folder are required, as well as the memory initialization file [meminit.hex](generated/meminit.hex). The latter contains the example program of our 3-LED chaser light smoke test; it is defined in the [ChaserLight3.scala](src/main/scala/j1/examples/ChaserLight3.scala) source.

The top-level SystemVerilog design is included in [j1System.sv](generated/j1System.sv).

**Tip**: For deployment, you may like to write a custom bash script `deploy-rtl.sh` to copy those files over to your EDA tool project. The [build.sbt](build.sbt) configuration already includes a custom `deploy` target that will execute such as script, i.e., via `sbt deploy`.

# Reference Design

The top-level Chisel design file is [`j1System.scala`](src/main/scala/j1/chisel/System.scala). Here is an extract that includes all external signals:

```scala
class j1System(clk_freq: Int = 50000000)(implicit cfg: j1Config) extends Module {
  // Interface
  val io = IO(new Bundle {
    val led0 = Output(Bool())
    val led1 = Output(Bool())
    val led2 = Output(Bool())
  })

  // Test Probes
  val probe = IO(new Bundle {
    val pc = Output(UInt(16.W))
    val reboot = Output(Bool())
    val dsp = Output(UInt(dstkDepth.W))
    val rsp = Output(UInt(rstkDepth.W))
    val st0 = Output(UInt(datawidth.W))
    val st1 = Output(UInt(datawidth.W))
    val status = Output(Bits(3.W))
  })

  // Submodules
  ...
}
```

The generated [j1System.sv](generated/j1System.sv) SystemVerilog file additionally includes a `reset` and `clock` signal implicit in the Chisel design:

```systemverilog
module j1System(
  input         clock,
                reset,
  output        io_led0,
                io_led1,
                io_led2,
  output [15:0] probe_pc,
  output        probe_reboot,
  output [4:0]  probe_dsp,
                probe_rsp,
  output [15:0] probe_st0,
                probe_st1,
  output [2:0]  probe_status
);
// ...
endmodule
```

For deploying / integrating the SystemVerilog design into a given FPGA SoC ...
- The `clock` input ought be connected to a 50 MHz clock. The CPU operates/triggers on the rising edge of the clock.
- The `reset` input is high-active, i.e., must be deasserted during execution of the design.
- The `io_led0`, `io_led1` and `io_led2` LED outputs are all high-active.
- The various `probe_...` outputs are used by Chisel testing and simulation, and do not need to be connected on the target. (The EDA tool may emit a warning thus that can be ignored.)

The reference design monitors the `status` output of the J1 CPU, and if that output becomes non-zero due to a `HALT` or run-time stack error, overrides the LED I/O mapping and uses the 3 LEDS to emit the CPU status instead. To delineate this situation from normal operation, the LEDS will fast-flash while emitting the CPU status. Below is a summary of possible status outputs:

| Status Output | Fault | Caveat |
|:-------------:| ----- | ------ |
| `000` | no fault (CPU running) | |
| `001` | illegal access to memory | currently not implemented (future work) |
| `010` | data stack underflow | requires `j1.cpu.stackchecks` to be enabled |
| `011` | data stack overflow | requires `j1.cpu.stackchecks` to be enabled |
| `100` | return stack underflow | requires `j1.cpu.stackchecks` to be enabled |
| `101` | return stack overflow | requires `j1.cpu.stackchecks` to be enabled |
| `110` | watchdog failure | currently not implemented (future work) |
| `111` | CPU halting | requires `j1.cpu.isa.halt` to be enabled |

The design relies on a 50 MHz clock, mainly for software time delays in the chaser light smoke test; a lower or higher frequency, e.g., in the range of 10..100 MHz might well work too, provided timing constraints of the synthesis are fulfilled (please ensure that relevant configuration files are created in your EDA tool to check for timing violations).

A subtle point of failure turns out to be inference and synthesis of **True Dual-Ported RAM** (TDP RAM) for the main memory of the design. This synthesis mismatch might occur *without the EDA tool emitting a suitable warning or error message*. Hence, please manually check that TDP RAM was synthesized correctly, and otherwise enable the `j1.memory.bbtpd` configuration to circumvent the issue.

## Note on I/O Mapping

The reference design illustrates how mapping I/O addresses suitably to hardware devices can be achieved. Here is the relevant fragment from [`j1System.scala`](src/main/scala/j1/chisel/System.scala):

```scala
/* Example system with three connected LEDS. */
class j1System(clk_freq: Int = 50000000)(implicit cfg: j1Config) extends Module {
  // ...

  /**************/
  /* IO Mapping */
  /**************/

  /* Buffer j1cpu comibatorial outputs to delay device writes. */
  val io_addr = RegNext(next = j1cpu.io.mem_addr, init = 0.U)
  val io_dout = RegNext(next = j1cpu.io.dout, init = 0.U)
  val io_wr   = RegNext(next = j1cpu.io.io_wr , init = false.B)

  // LEDS Device Register
  val leds_state = RegInit(7.U(3.W))

  /* IO Mapping: Read Action */
  j1cpu.io.io_din := 0.U
  switch (j1cpu.io.dout) {
    is ("h0000".U) {
      j1cpu.io.io_din := leds_state
    }
  }

  /* IO Mapping: Write Action */
  when (io_wr) {
    switch (io_addr) {
      is ("h0000".U) {
        leds_state := io_dout
      }
    }
  }
```

Note that the local `io_addr`, `io_dout` and `io_wr` registers buffer/delay the respective CPU outputs for one clock cycle. This is a necessary technicality due the J1 CPU generally carrying out memory access in a synchronous manner.

# Assembler / Disassembler

Primarily for writing test programs, the development contains a mini-assembler that is realized as a [shallowly embedded DSL](https://havelund.com/Publications/dsl-scala-2015.pdf) into Scala. Currently, the assembler only works well with the default [j1.conf](j1.conf) CPU configuration. *Work is underway to elaborate its functionality in order to make it configuration-aware and fully support the J1 configuration space ...*

A typical program might look like the following (this is the chaser light example of the smoke test):

```scala
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
```

We hence have to extend the `j1Asm` class and provide the instructions for the program in the subclass body. The `j1Asm` class provides moreover a parameter to set the start address for compilation. E.g., one could write:

```scala
object ChaserLight3 extends j1Asm(0x100) {
  // ...
}
```

If absent, `0x0` is used &mdash; which is where execution starts after a CPU reset.

## Machine Instructions

The categories of atomic J1 instructions are IMMEDIATE, BRANCH, and ALU. Note that COMPOUND instructions are *sequences* made of atomic ones, and may take more than one CPU cycle. In the table below, **TOS** and **NOS** refer top-of-stack and next-of-stack, respectively. A flag is typically encoded as `0` (false) or `non-zero` (true). The instruction set is reminiscent of a minimal Forth run-time system upon which a more elaborate Forth environment can be bootstrapped, via a *nucleus* and Forth interpreter (*see the original [J1 tool-chain](https://github.com/jamesbowman/j1/tree/master/toolchain), we shall provide our own integrating tightly with this work in the near future as well ...*).

| Syntax | Type | Value Range | Description | Note |
| ------ |:----:|:-----------:| ----------- | ---- |
| `imm(x)` | IMMEDIATE | `0`..`32767` | pushes `x` onto the data stack | single instruction |
| `push(x)` | IMMEDIATE or COMPOUND | `-32768`..`65535` | pushes `x` onto the data stack | `1`..`2` instructions |
| `jmp(addr)` | BRANCH | `0x0`..`0x1FFF` | unconditional jump to address `addr` | no stack changes |
| `jmp("name")` | BRANCH | non-empty `String` | unconditional jump to label `name` | no stack changes |
| `jpz(addr)` | BRANCH | `0x0`..`0x1FFF` | conditional jump to address `addr` | pops flag/condition from the data stack |
| `jpz("name")` | BRANCH | non-empty `String` | conditional jump to label `name` | pops flag/condition from the data stack |
| `call(addr)` | BRANCH | `0x0`..`0x1FFF` | subroutine call to address `addr` | pushes address onto the return stack |
| `call("name")` | BRANCH | non-empty  `String` | subroutine call to label `name` | pushes address onto the return stack |
| `exit` | ALU | &ndash; | exits/returns from subroutine call | pops address from the return stack |
| `noop` | ALU | &ndash; | no-op instruction (one-cycle delay) | no stack changes |
| `plus` | ALU | &ndash; | adds the TOS and NOS values | consumes operands from the data stack and pushes back result |
| `and` | ALU | &ndash; | bitwise AND of the TOS and NOS values | consumes operands from the data stack and pushes back result |
| `or` | ALU | &ndash; | bitwise OR of the TOS and NOS values | consumes operands from the data stack and pushes back result |
| `xor` | ALU | &ndash; | bitwise XOR of the TOS and NOS values | consumes operands from the data stack and pushes back result |
| `invert` | ALU | &ndash; | bitwise complement of the TOS value | consumes operand from the data stack and pushes back result |
| `equal` | ALU | &ndash; | checks whether the TOS and NOS values are equal | consumes operands from the data stack and pushes flag (`0` or `-1`) |
| `less` | ALU | &ndash; | checks whether the TOS value is (signed) less than the NOS value | consumes operands from the data stack and pushes flag (`0` or `-1`) |
| `uless` | ALU | &ndash; | checks whether the TOS value is (unsigned) less than the NOS value | consumes operands from the data stack and pushes flag (`0` or `-1`) |
| `swap` | ALU | &ndash; | swaps the TOS and NOS | data stack retains its size |
| `dup` | ALU | &ndash; | duplicates the TOS value | data stack grows by one |
| `drop` | ALU | &ndash; | drops the TOS value | data stack shrinks by one |
| `over` | ALU | &ndash; | copies over the NOS value | data stack grows by one|
| `nip` | ALU | &ndash; | drops the NOS value | data stack shrinks by one |
| `rdrop` | ALU | &ndash; | drops the top value from the return stack | return stack shrinks by one |
| `to_r` | ALU | &ndash; | moves the TOS value to the return stack | data stack shrinks by one and return stack grows by one |
| `from_r` | ALU | &ndash; | moves the return TOS value to the data stack | data stack grows by one and return stack shrinks by one |
| `r_fetch` | ALU | &ndash; | copies the return TOS value to the data stack | data stack grows by one and return stack remains unchanged |
| `fetch` | ALU | &ndash; | fetches the value at the TOS address from memory | consumes address from the data stack and pushes back memory content |
| `iofetch` | ALU | &ndash; | fetches the value at the TOS address from IO space | consumes address from the data stack and pushes back IO register content |
| `store` | COMPOUND | &ndash; | writes the NOS value to the TOS memory address | consumes written value and address from the data stack |
| `iostore` | COMPOUND | &ndash; | writes the NOS value to the TOS IO space address | consumes written value and address from the data stack |
| `rshift` | ALU | depends on shifter configuration (TOS) | right bitshift of the NOS value by TOS bits | consumes operands from the data stack and pushes back result |
| `lshift` | ALU | depends on shifter configuration (TOS) | left bitshift of the NOS value by TOS bits | consumes operands from the data stack and pushes back result |
| `depths` | ALU | &ndash; | pushes the current sizes of the return and data stack as a concatenated bitstring `rsp ## dsp` | data stack grows by one; width of `rsp`/`dsp` depends on stack configuration |
| `protect` | ALU | &ndash; | enables lower-memory protection after a reset, if configured | no stack changes |
| `bank(n)` | `0`..`15` | &ndash; | sets value of the bank register (used by far absolute branches up to `0xFFFF`, if configured) | no stack changes |
| `halt` | ALU | &ndash; | halts CPU execution until a reset is carried out | no stack changes |

To gain a deeper understanding of the instruction set of the J1, it is helpful to consult the earlier mentioned paper: [J1: a small Forth CPU Core for FPGAs](https://excamera.com/files/j1.pdf). The Scala data structures for instruction encoding can be found in [InsnEnc.scala](src/main/scala/j1/miniasm/InsnEnc.scala).

## Control Commands

The mini-assembler provides a small number of CONTROL commands which are listed in the table below.

| Syntax | Type | Value Range | Description | Note |
| ------ |:----:|:-----------:| ----------- | ---- |
| `init` | CONTROL | &ndash; | (re)initializes compilation | discards all previously compiled segments |
| `done` | CONTROL | &ndash; | closes the current compilation segment | optional: some API functions do this automatically if needed |
| `org(addr)` | CONTROL | `0`..`0xFFFF` | continues compilation at the given address | opens a new compilation segment while closing the current one |
| `label("name")` | CONTROL | non-empty `String` | declares the location of a label | labels can be used as targets by `jmp`, `jpz` and `call` (see previous table) |

Note that labels can be used *before* they are declared, so that forward branches are feasible. There exists currently no scoping of label names, meaning they have to be globally unique (*ongoing work on the assembler may address this*).

When a compilation segment is closed via `done`, a new one is automatically opened at the current compilation address. If nothing else is compiled, that segment will, however, be implicitly discarded.

The assembler carries out a number of sporadic dynamic checks to ensure that compilation segments to not overlap and all used label names have been defined via `label("name")`. *NOTE: A more sophisticated framework to detect compilation errors and raise runtime exceptions is currently under development.*

## Augmentation

James Bowman introduced the concept of `Elided Words` into his [core instruction set](https://github.com/jamesbowman/j1/blob/master/toolchain/basewords.fs). Elided words are variations of the core machine instructions that may
- leave one operand on the data stack (rather than consuming it);
- leave two operands on the data stack (rather than consuming them);
- embed a subroutine `exit` into the current instruction.

Elided words take advantage of the instruction decoding and execution mechanism; they can sometimes be used to optimize code by *compressing* two or more instructions into a single one.

Rather than defining a separate instruction for each elided word, we allow ALU instructions to be suffixed by `.^`, `.^^`, or `.x`:

| Suffix | Modified Behavior | Note |
| ------ | ------------------ | ---- |
| `.^` | keeps one operand on the data stack | applies to unary and binary operations, e.g., `invert` and `plus` |
| `.^^` | keeps both operands on the data stack | applies to binary operations only, e.g., `plus` and `uless` |
| `.x` | embeds an `exit` into the current instruction | |

Note that augmentations can be combined. E.g., `plus.^^.x` is permissible. Beware that not all all ALU instructions support a particular augmentation. The compiler carries out validity checks, doing its best to spot cases where augmentation cannot be supported due to the nature of the instruction.

To see how augmentation works in action, have a look at the [ChaserLight3](src/main/scala/j1/examples/ChaserLight3.scala) assembler program at the start of this section.

## Miscellaneous Instructions

We lastly have a few miscellaneous instructions, for instance, to fill the memory with raw data (type DATA) or execute a software halt when no `HALT` instruction is configured / available.

| Syntax | Type | Value Range | Description | Note |
| ------ |:----:|:-----------:| ----------- | ---- |
| `softhalt` | BRANCH | &ndash; | executes a `jmp` to the same address | results in an infinite loop (software halt) |
| `restart` | BRANCH | &ndash; | soft reset: compiles a branch to address zero: `jmp(0x0000)` | does **not** clear the stacks! |
| `fill` | DATA | &ndash; | compile one zero memory word | a memory words is 16 bit |
| `fill(n)` | DATA | &ndash; | compile `n` zero memory words | a memory words is 16 bit |
| `fill(n,v)` | DATA | `-32768`..`65535` | fill `n` memory words of with data `v` | a memory words is 16 bit |

## Debugging and Deployment

Deployment of j1Asm programs can be achieved by the public API function:

```scala
def deploy(implicit memIf: MemInterface): Unit
```

The function must be passed a [MemInterface](src/main/scala/j1/miniasm/MemInterface.scala) trait instance, either explicitly or implicitly. Such is used to write the program, e.g., into the Chisel design for testing (see [j1Test](src/test/scala/j1/j1Test.scala)). The `deploy(...)` function produces debugging output of the disassembled deployed program by default.

For deployment onto an FPGA target, the functions:

```scala
  def writehex(filename: String): Boolean
  def writebin(filename: String): Boolean
```

write a memory initialization file that can be included in Chisel via `loadMemoryFromFileInline(...)`, see [Memory.scala](src/main/scala/j1/chisel/Memory.scala).

There also exist versions that confine the HEX/BIN output to a particular memory region:

```scala
  def writehex(filename: String, region: Range): Boolean
  def writebin(filename: String, region: Range): Boolean
```

Note that the initialization file may be truncated wrt to the full size of the memory. This can result in warnings from the EDA tool that, however, can be safely ignored.

Lastly, `j1Asm` provides a disassemble function:

```scala
  def disasm(): Unit
```

that utilizes the `j1Disasm` component (see next section) in order to produce a disassembled output of the compiled program for validation and debugging purposes.

## Disassembler

The `j1Disasm` component (defined in [MiniAsm.scala](src/main/scala/j1/miniasm/MiniAsm.scala) too) can moreover be used in isolation, using its object *apply* methods:

```scala
object j1Disasm {
  def apply(start: Int, end: Int)(implicit memIf: MemInterface): Unit
  def apply(region: Range)(implicit memIf: MemInterface): Unit
}
```

and requiring again an implementation of the [MemInterface](src/main/scala/j1/miniasm/MemInterface.scala) trait to read the portion from the memory to be disassembled. In order to disassemble a single 16-bit instruction (given as a `Short`), use the

```scala
object j1Disasm {
  def decode(codeword: Short): String
}
```

object method.

# Testing Framework

To simulate and test the J1 reference design ([System.scala](src/main/scala/j1/chisel/System.scala)), an extended [PeekPokerTester](https://github.com/ucb-bar/chiseltest/blob/main/src/main/scala/chiseltest/iotesters/PeekPokeTester.scala) is provided via the source file [j1PeekPokeTester.scala](src/test/scala/j1/j1PeekPokeTester.scala). It can be incorporated into a [`chiseltest`](https://github.com/ucb-bar/chiseltest) test case as follows:

```scala
package j1.chisel.test

import j1.chisel.j1Config
import j1.chisel.j1System

import org.scalatest.flatspec.AnyFlatSpec
import chiseltest._

class j1Test extends AnyFlatSpec with ChiselScalatestTester {
  implicit val cfg = j1Config.load("j1.conf").without_bb_tdp
  cfg.dump()

  behavior of "<UNIT>"
  it should "<REQUIREMENT>" in { /* execute sample test case */
    test(new j1System).runPeekPoke(dut => new j1PeekPokeTester(dut) {
      // ...
    }
  }
}
```

The methods of `j1PeekPokeTester` permit one to easily inspect the state of the J1 CPU and stacks via the following API:

```scala
class j1PeekPokeTester(dut: j1System) extends PeekPokeTester(dut) with MemInterface {
  /* Utility API functions for dumping CPU state and stacks. */
  def dumpPC() = { ... }
  def dumpReboot() = { ... }
  def dumpStatus() = { ... }
  def dumpStack() = { ... }
  def dumpRStack() = { ... }

  /* Dump entire state of the J1 CPU and stack content. */
  def dumpState() = {
    dumpPC()
    dumpReboot()
    dumpStatus()
    dumpStack()
    dumpRStack()
  }
}
```

Since `j1PeekPokeTester` implements `MemInterface`, we can directly pass it to the `deploy(...)` methods of `j1Asm` (see Section [Debugging and Deployment](#debugging-and-deployment)). Otherwise, `j1PeekPokeTester` also provides the following methods to write programs into memory:

```scala
  /* Clears program memory by writing zero words to all of it. */
  def clearProgMem() = { ... }

  /* Initializes program memory for a sequence of instructions. */
  def initProgMem(code: Seq[Int], start: Int = 0) = { ... }
```

We are currently exploring in what other ways `j1PeekPokeTester` could be extended to facilitate simulation and testing of J1 CPU instances. Test developers are encouraged to explore the [j1Test](src/test/scala/j1/j1Test.scala) sample test. It presents a basic *how-to* for performing compilation, deployment, simulated execution, and verification of a simple J1 program.

# Caveats and Limitations

Much of the Chisel design and J1 assembler E-DSL still needs thorough testing. I.e., while [src/main/scala/j1/chisel/j1.scala](src/main/scala/j1/chisel/j1.scala) is, in essence, feature complete with respect to the described configuration space, various combinations of settings still have to be thoroughly tested to validate, e.g., issues due to cross-feature dependencies. We are in the process of building a [Clafer](https://www.clafer.org/) model for the configuration space, with suitable constraints on instance generation.

The J1 mini-assembler currently needs work in terms of taking into account the CPU configuration ([j1.config](j1.config)) and, correspondingly, adjusting code generation and carrying out relevant validation. The author is not entirely happy with the E-DSL syntax (i.e., need for brackets around arguments) and might explore ways to improve and beautify it, while making it more robust too.

When experimenting with deployment of the generated SystemVerilog onto an FPGA target (Cyclone II [EP2C5T144](https://hobbycomponents.com/altera/819-altera-cyclone-ii-es2c5t144-fpga-dev-board) board with with Quartus II [13.0sp1](https://www.intel.com/content/www/us/en/software-kit/711790/intel-quartus-ii-web-edition-design-software-version-13-0sp1-for-linux.html)), problems occurred due to TDP BRAM not being inferred and synthesized correctly. There are some discussions of similar issues [HERE](https://github.com/chipsalliance/chisel/issues/1788) and [HERE](https://stackoverflow.com/questions/54789756/vivado-cant-recognize-the-double-port-ram-while-using-syncreadmem). Our solution is to manually adjusts `j1.memory.bbtpd`, setting this option to `true` for SystemVerilog generation (`sbt run`) and `false` for Treadle-based simulation (`sbt test`).

The J1 mini-assembler is useful and sufficient for writing programs for test cases, but inappropriate for developing realistic applications for the J1. The author already developed a tool-chain (in [Standard ML](https://www.polyml.org/)) for remote J1 Forth development, optimization and testing. This shall be integrated with this project in the near future, to provide a comprehensive development system for the J1.

# Future Work Roadmap

The Chisel conversion is a means to an end to illustrate and teach [RDE](https://galois.com/services/rigorous-digital-engineering/), as it has been advocated by the Formal Methods and MDE/MBSE research communities, and is already practiced on large-scale industrial systems by companies such as [Galois, Inc.](https://galois.com) across many technological domains. We envisage the following items of future work:
- Create a formal feature model for the J1 CPU, e.g., in [Clafer](https://www.clafer.org/) or [Lando/Lobot](https://github.com/GaloisInc/BESSPIN-Lando).
- Include resource requirements, i.e., in terms of LE/LUTs into the feature model and develop automatic scripts to empirically sample resource use by feeding back the synthesis reports of EDA tools.
- Show how the feature model aids us in design-space exploration and optimization of CPU configurations with respect to a formal set of constraints and requirements.
- Enhance the J1 mini-assembler implementation to fully acknowledge the CPU configuration.
- Provide a comprehensive collection of configuration-aware manual test cases to validate the j1 Chisel hardware model.
- Define an abstract execution model of the CPU in a theorem prover such as [Isabelle](https://isabelle.in.tum.de/) or [Coq](https://coq.inria.fr/).
- Generate verified Scala code from the above formal model and integrate it into the J1 testing framework.
- Use the verified abstract machine as a (model-based) *test oracle* to validate correctness of the J1 Chisel design under arbitrary configurations.
- Automatically generate test cases via automated exploration strategies, such [intelligent fuzzing](https://en.wikipedia.org/wiki/Fuzzing).
- Examine coverage of the above mentioned testing strategy, both in terms of the configuration space and Chisel/SystemVerilog design (for a given configuration).
- Provide a pluggable framework for J1-based SoC, similar to the [Chipyard](https://chipyard.readthedocs.io/en/stable/) framework, but more geared towards training and teaching in terms of its complexity and learning curve.
- Provide a UART module and boot-loader implementation to upload programs without the need to reprogram the FPGA.
- Implement a Scala-based tool-chain for **J1 Forth** with tight integration for Chisel simulation/testing and on-target deployment of programs.

# Contributions

Contributions are very welcome, in particular, referring to the above roadmap.

# Repository Policy

Please create a branch for each contribution or bug fix, together with an issue and subsequent merge request.

# License

The software is released under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

The respective license file is included in the repository as [LICENSE](LICENSE).
