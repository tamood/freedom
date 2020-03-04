Freedom E300 Nexys A7 FPGA Trainer Board
-----------------------------------------

The Freedom E300 Nexys A7 FPGA Trainer Board implements a single core Freedom E300 chip.

### How to build

The Makefile corresponding to the Freedom E300 Nexys A7 FPGA Trainer Board is
`Makefile.e300nexysA7devkit` and it consists of five main targets:

- `verilog`: to compile the Chisel source files and generate the Verilog files.
- `bit`: to create a Bitstream File.
- `buildbsp`: to generate Board-Support-Package in freedom-e-sdk/bsp. buildbsp must be done at one time before building elf.
- `buildrom`: to build the software project in freedom-e-sdk/software. Currently singlecore-gpio is fully tested on this build for proof-of-concept.
- `updaterom`: to patch Bitstream with elf generated with buildrom. This step must be repeated after buildrom.
- `uploadrom`: uploads the design into the FPGA with emulated Digilent JTAG port over USB.

To execute these targets, you can run the following commands:

```sh
$ make -f Makefile.e300nexysA7devkit verilog
$ make -f Makefile.e300nexysA7devkit mcs
$ make -f Makefile.e300nexysA7devkit buildbsp
$ make -f Makefile.e300nexysA7devkit buildrom
$ make -f Makefile.e300nexysA7devkit updaterom
$ make -f Makefile.e300nexysA7devkit uploadrom
```

Note: This flow was tested on Vivado 2019.2.


Freedom U500 Nexys A7 FPGA Trainer Board
-----------------------------------------

The Freedom U500 Nexys A7 FPGA Trainer Board implements a dualcore Freedom U500 chip.

### How to build

The Makefile corresponding to the Freedom Freedom U500 Nexys A7 FPGA Trainer Board is
`Makefile.u500nexysA7devkit` and it consists of five main targets:

- `verilog`: to compile the Chisel source files and generate the Verilog files.
- `bit`: to create a Bitstream File. 
- `buildbsp`: to generate Board-Support-Package in freedom-e-sdk/bsp. buildbsp must be done at one time before building elf.
- `buildrom`: to build the software project in freedom-e-sdk/software. Currently dualcore-gpio is fully tested on this build for proof-of-concept.
- `updaterom`: to patch Bitstream with elf generated with buildrom. This step must be repeated after buildrom.
- `uploadrom`: uploads the design into the FPGA with emulated Digilent JTAG port over USB.

To execute these targets, you can run the following commands:

```sh
$ make -f Makefile.u500nexysA7devkit verilog
$ make -f Makefile.u500nexysA7devkit mcs
$ make -f Makefile.u500nexysA7devkit buildbsp
$ make -f Makefile.u500nexysA7devkit buildrom
$ make -f Makefile.u500nexysA7devkit updaterom
$ make -f Makefile.u500nexysA7devkit uploadrom
```

Note: This flow was tested on Vivado 2019.2.
