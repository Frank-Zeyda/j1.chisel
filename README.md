# Chisel conversion of the J1 CPU for FPGA

This repository contains a conversion of [James Bowman's J1 CPU](https://github.com/jamesbowman/j1) for FPGA into [Chisel](https://www.chisel-lang.org/).

The [sbt](https://www.scala-sbt.org/) tool is require to generate the hardware design and example, and perform simulation-based testing.
- To compile the generator source code, execute `sbt compile`.
- To generate the complete Verilog design for a smoke test (3-LED chaser light), execute `sbt run`.
- To execute a test using [chiseltest](https://github.com/ucb-bar/chiseltest), execute `sbt test`.

Note that all generated Verilog files and the memory initialization file are output to the *generated* folder.

More detailed documentation on the current state of the work and how to use it will follow soon ...
