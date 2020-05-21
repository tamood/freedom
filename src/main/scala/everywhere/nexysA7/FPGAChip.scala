// See LICENSE for license details.
package sifive.freedom.everywhere.nexysA7

import Chisel._
import chisel3.withClockAndReset
import chisel3.experimental.{attach}

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.jtag.{JTAGIO}

import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.clocks._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pwm._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.i2c._
import sifive.blocks.devices.jtag._
import sifive.blocks.devices.pinctrl.{BasePin}

import sifive.fpgashells.shell.xilinx.nexysA7shell._
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.clocks._

//-------------------------------------------------------------------------
// E300NexysA7FPGAChip
//-------------------------------------------------------------------------

class FPGAChip(implicit val p: Parameters) extends NexysA7Shell {

  //-----------------------------------------------------------------------
  // Wire declrations
  //-----------------------------------------------------------------------

  val dut_clock       = Wire(Clock())
  val dut_reset       = Wire(Bool())
  val dbg_reset       = Wire(Bool())

  //-----------------------------------------------------------------------
  // Clock Generator
  //-----------------------------------------------------------------------

  val clock_gen = Module(new Series7MMCM(PLLParameters("MASTER_CLOCK_GEN",
    PLLInClockParameters(100, 50),
    Seq(
      PLLOutClockParameters(p(FPGAFrequencyKey))))))
  
  clock_gen.io.clk_in1 := IBUFG(clock)
  clock_gen.io.reset   := ~resetn
  val clock_gen_locked = clock_gen.io.locked
  val Seq(busclk, _*) = clock_gen.getClocks

  //-----------------------------------------------------------------------
  // System clock and reset
  //-----------------------------------------------------------------------

  dut_clock := busclk
  dut_reset := !clock_gen_locked 

  //-------------------------------------------------------------------
  // VGA Stub
  //-------------------------------------------------------------------
  
  val inst_vga = Module(new VGAStub)
  VGA.HSYNC := inst_vga.io.hsync
  VGA.VSYNC := inst_vga.io.vsync
  VGA.RGB := inst_vga.io.rgb
  inst_vga.io.clk := dut_clock
  inst_vga.io.rst := dut_reset
 
  withClockAndReset(dut_clock, dut_reset | dbg_reset) {
	  
    val coreplex = Module(LazyModule(new Subsystem).module)

    //---------------------------------------------------------------------
    // Peripharal connections
    //---------------------------------------------------------------------
    
    //define busses
    val uart_pins = p(PeripheryUARTKey) map { param => Wire(new UARTPins(()=> new BasePin))}
    val pwm_pins  = p(PeripheryPWMKey)  map { param => Wire(new PWMPins(() => new BasePin, param))}
    val spi_pins  = p(PeripherySPIKey)  map { param => Wire(new SPIPins(() => new BasePin, param))}
    val i2c_pins  = p(PeripheryI2CKey)  map { param => Wire(new I2CPins(() => new BasePin))}
    val gpio_pins = p(PeripheryGPIOKey) map { param => Wire(new GPIOPins(()=> new BasePin, param))}
    
    
    //connect with coreplex
    (uart_pins zip  coreplex.uart) foreach { case (p, r) => UARTPinsFromPort(p, r, clock = dut_clock, reset = dut_reset)}
    (pwm_pins  zip  coreplex.pwm)  foreach { case (p, r) => PWMPinsFromPort(p, r) }
    (spi_pins  zip  coreplex.spi)  foreach { case (p, r) => SPIPinsFromPort(p, r, clock = dut_clock, reset = dut_reset)}
    (i2c_pins  zip  coreplex.i2c)  foreach { case (p, r) => I2CPinsFromPort(p, r, clock = dut_clock, reset = dut_reset)}
    (gpio_pins zip  coreplex.gpio) foreach { case (p, r) => GPIOPinsFromPort(p, r) }
    
    //connect leds to gpio0
    GPIO.tieoff(coreplex.gpio(0))
    val led_wires = Wire(Vec(16, Bool()))
    val leds = led_wires.toSeq ++ RGB(0).getElements ++ RGB(1).getElements
    (gpio_pins(0).pins zip leds) foreach { case (pin, led) => led := pin.o.oval}
    LED := led_wires.asUInt

    //connect switches to gpio1
    val switches = SW.asBools ++ BTN.getElements
    (gpio_pins(1).pins zip switches) foreach { case (pin, switch) => pin.i.ival := switch}
    
    //connect seven segmant display to gpio3
    GPIO.tieoff(coreplex.gpio(3))
    val ca_wires = Wire(Vec(8, Bool()))
    val an_wires = Wire(Vec(8, Bool()))
    val seg7 = an_wires.toSeq ++ ca_wires.toSeq
    (gpio_pins(3).pins zip seg7) foreach { case (pin, led) => led := pin.o.oval}
    AN := an_wires.asUInt
    CA := ca_wires.asUInt
    
    //connect PMOD headers to gpio2
    val index  = (1 to 4) ++ (7 to 10)
    val pmodHdrs = (index map JA) ++ (index map JX) ++ (index map JC) ++ (index map JD)
	(gpio_pins(2).pins zip pmodHdrs) foreach { case (pin, hdr) => IOBUF(hdr, pin)} 	
	
	//connect UART
	IOBUF(UART.TX, uart_pins(0).txd)
	IOBUF(UART.RX, uart_pins(0).rxd)
	UART.RTSN := false.B	
	
	//connect Temperature Sensor
	IOBUF(TMP.SCL, i2c_pins(0).scl)
	IOBUF(TMP.SDA, i2c_pins(0).sda)	
	
	//connect Accelerometer
    IOBUF(ACL.SCLK, spi_pins(0).sck)
    IOBUF(ACL.CSN, spi_pins(0).cs(0))
    IOBUF(ACL.MISO, spi_pins(0).dq(1))
    IOBUF(ACL.MOSI, spi_pins(0).dq(0))
    
    //connect aux ports
    val iof_00 = coreplex.gpio(0).iof_0.get
    val iof_20 = coreplex.gpio(2).iof_0.get
    
    //connect pwm to audio pwm
    AUD.SDN := true.B
    IOBUF(AUD.PWM, pwm_pins(0).pwm(3))
    
    //connect pwm to rgb leds
    BasePinToIOF(pwm_pins(0).pwm(1), iof_00(16))
    BasePinToIOF(pwm_pins(0).pwm(2), iof_00(17))
    BasePinToIOF(pwm_pins(0).pwm(3), iof_00(18))
    
    //connect aux functions to gpio2
    val auxJA = List(uart_pins(1).rxd, uart_pins(1).txd, 
                       i2c_pins(1).scl, i2c_pins(1).sda,
                       spi_pins(1).cs(0), spi_pins(1).dq(1), spi_pins(1).dq(0), spi_pins(1).sck) 
    val auxList = (0 to 2).map(pwm_pins(_).pwm.toSeq).foldLeft(auxJA)((l, s) => l ++ s ++ s)
                       
    auxList.zipWithIndex.foreach {case (aux, i) => BasePinToIOF(aux, iof_20(i))}     
    
    val jtag_pins = Wire(new JTAGPins(()=> new BasePin, false))
    
    // SPI flash connectivity
    //---------------------------------------------------------------------
    
    val spif_pins  = p(PeripherySPIFlashKey)  map { param => Wire(new SPIPins(() => new BasePin, param))}
    ((spif_pins zip coreplex.qspi) zip p(PeripherySPIFlashKey)) foreach { case ((p, r), param) => SPIPinsFromPort(p, r, clock = dut_clock, reset = dut_reset, syncStages = param.defaultSampleDel)}

    val startupe2_inst = Module(new STARTUPE2)
    startupe2_inst.io.USRCCLKO := spif_pins(0).sck.o.oval.asClock

    IOBUF(QSPI_CSN,  spif_pins(0).cs(0))

    IOBUF(QSPI_DQ(0), spif_pins(0).dq(0))
    IOBUF(QSPI_DQ(1), spif_pins(0).dq(1))
    IOBUF(QSPI_DQ(2), spif_pins(0).dq(2))
    IOBUF(QSPI_DQ(3), spif_pins(0).dq(3))

    //JTAG connectivity
    
    val btunnel = Module(new BSCANTunnel())
    val int_jtag = IOBUF(JB(1))
    val iobuf_tdo = Module(new IOBUF())
    attach(iobuf_tdo.io.IO, JB(9))
    
    coreplex.debug.foreach { debugio => 
		debugio.clock := dut_clock
		debugio.reset := dut_reset
		debugio.dmactiveAck := true.B
		dbg_reset := debugio.ndreset
		
		debugio.systemjtag.foreach { sj =>
			sj.reset := dut_reset
			
			sj.mfr_id := p(JtagDTMKey).idcodeManufId.U(11.W)
			sj.part_number := p(JtagDTMKey).idcodePartNum.U(16.W)
			sj.version := p(JtagDTMKey).idcodeVersion.U(4.W)
			
			sj.jtag.TCK := BUFGMUX(int_jtag, btunnel.io.TCK, IBUFG(JB_10))
			sj.jtag.TMS := Mux(int_jtag,     btunnel.io.TMS, IOBUF(JB(7)))
			sj.jtag.TDI := Mux(int_jtag,     btunnel.io.TDI, IOBUF(JB(8)))
			
			btunnel.io.TDO := sj.jtag.TDO
			iobuf_tdo.io.T := ~(sj.jtag.TDO.driven)
			iobuf_tdo.io.I := sj.jtag.TDO.data			
		}	
	}
 }             						
  
  ElaborationArtefacts.add(
    "clockdomains.synth.tcl",
    """
    create_clock -add -name sys_clk -period 10.00 -waveform {0 5} [get_ports {clock}]
    create_clock -add -name jtagclk -period 20.00 -waveform {0 5} [get_ports {JB_10}]
    set_clock_groups -asynchronous -group [get_clocks -include_generated_clocks sys_clk]
    set_clock_groups -asynchronous -group [get_clocks clk_out1*]
    set_clock_groups -asynchronous -group [get_clocks jtagclk]
    """)

}
