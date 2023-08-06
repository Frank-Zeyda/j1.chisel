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
package j1.chisel

import chisel3._

import chisel3.util.HasBlackBoxInline

import chisel3.util.experimental.loadMemoryFromFileInline

import firrtl.annotations.MemoryLoadFileType

/* True dual-ported RAM is not correctly inferred and synthesized from
 * Chisel SyncReadMem. This is at least so on Quartus II (13.0sp1) when
 * targeting a Cyclone II FPGA (EP2C5T144C8). Hence, a black-box Verilog
 * model was added below from which the TDP RAM is correctly inferred. */

trait DualPortedRAM {
  // Parameters
  val width: Int
  val size: Int

  // Interface
  val io = IO(new Bundle {
    /* Clock */
    val clock = Input(Clock()) // only required by BB version

    /* Port A */
    val rdAddrA = Input(UInt(16.W))
    val rdDataA = Output(UInt(width.W))
    val wrAddrA = Input(UInt(16.W))
    val wrDataA = Input(UInt(width.W))
    val wrEnaA = Input(Bool())

    /* Port B */
    val rdAddrB = Input(UInt(16.W))
    val rdDataB = Output(UInt(width.W))
    val wrAddrB = Input(UInt(16.W))
    val wrDataB = Input(UInt(width.W))
    val wrEnaB = Input(Bool())
  })

  // Access to memory during testing (undefined for black-box module)
  val mem: SyncReadMem[UInt]
}

/* Chisel module of DualPortedRAM (use for testing) */
class ChiselDualPortedRAM(val width: Int, val size: Int) extends Module
  with DualPortedRAM {
  // Preconditions
  require(size <= 65536)

  // Submodules
  val mem = SyncReadMem(size, UInt(16.W))

  // Load memory content from file
  loadMemoryFromFileInline(mem,
    "meminit.hex", MemoryLoadFileType.Hex)

  /* Connect Read Ports (A & B) */
  io.rdDataA := mem.read(io.rdAddrA)
  io.rdDataB := mem.read(io.rdAddrB)

  /* Connect Write Ports (A) */
  when (io.wrEnaA) {
    mem.write(io.wrAddrA, io.wrDataA)
  }

  /* Connect Write Ports (B) */
  when (io.wrEnaB) {
    mem.write(io.wrAddrB, io.wrDataB)
  }
}

/* Black-box version of DualPortedRAM (use for code generation) */
class BBDualPortedRAM(val width: Int, val size: Int) extends BlackBox
  with HasBlackBoxInline with DualPortedRAM {
  // Preconditions
  require(size <= 65536)

  // Submodules
  lazy val mem: SyncReadMem[UInt] = throw new Error(
    "cannot access SyncReadMem instance of black-box dual-ported memory " +
   f"(class ${getClass.getSimpleName})")

  // Verilog Description
  setInline("BBDualPortedRAM.sv",
  f"""// Hand-coded black-box module for TPD RAM.
    |module BBDualPortedRAM (
    |  // Clock
    |  input clock,
    |
    |  // Port A
    |  input  [15:0] rdAddrA,
    |  output [${width-1}:0] rdDataA,
    |  input  [15:0] wrAddrA,
    |  input  [${width-1}:0] wrDataA,
    |  input         wrEnaA,
    |
    |  // Port B
    |  input  [15:0] rdAddrB,
    |  output [${width-1}:0] rdDataB,
    |  input  [15:0] wrAddrB,
    |  input  [${width-1}:0] wrDataB,
    |  input         wrEnaB
    |);
    |
    |  reg [15:0] ram[${size-1}:0]; // memory array
    |
    |  // Initialize memory from file
    |  `ifdef ENABLE_INITIAL_MEM_
    |    initial
    |      $$readmemh("meminit.hex", ram);
    |  `endif // ENABLE_INITIAL_MEM_
    |
    |  // Port A (read)
    |  always @(posedge clock)
    |  begin
    |    rdDataA <= ram[rdAddrA];
    |  end
    |
    |  // Port A (write)
    |  always @(posedge clock)
    |  begin
    |    if (wrEnaA)
    |    begin
    |      ram[wrAddrA] = wrDataB;
    |    end
    |  end
    |
    |  // Port B (read)
    |  always @(posedge clock)
    |  begin
    |    rdDataB <= ram[rdAddrB];
    |  end
    |
    |  // Port B (write)
    |  always @(posedge clock)
    |  begin
    |    if (wrEnaB)
    |    begin
    |      ram[wrAddrB] = wrDataB;
    |    end
    |  end
    |endmodule
    |""".stripMargin)
}
