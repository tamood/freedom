// See LICENSE for license details.
package sifive.freedom.unleashed.nexysA7

import Chisel._
import chisel3.experimental.{withClockAndReset}

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pinctrl.{BasePin}

import sifive.fpgashells.shell.xilinx.nexysA7shell._
import sifive.fpgashells.ip.xilinx.{IOBUF}

import sifive.fpgashells.devices.xilinx.nexysA7mig._
//-------------------------------------------------------------------------
// PinGen
//-------------------------------------------------------------------------

object PinGen {
  def apply(): BasePin = {
    new BasePin()
  }
}

trait HasDDR2 { this: NexysA7Shell =>
  
  require(!p.lift(PeripheryDDRMIGKey).isEmpty)
  val ddrpads = IO(new DDRMIGPads(p(PeripheryDDRMIGKey)))
  
  def connectMIG(dut: HasDDRMIGModuleImp): Unit = {
    // Clock & Reset
    dut.ddrmigio.sys_clk_i  := clk200Mhz.asUInt
    dut.ddrmigio.aresetn    := migaresetn
    dut.ddrmigio.sys_rst    := resetn
    mig_ui_clock            := dut.ddrmigio.ui_clk
    mig_ui_reset            := dut.ddrmigio.ui_clk_sync_rst
    mig_clock_gen_locked    := dut.ddrmigio.mmcm_locked
    

    ddrpads <> dut.ddrmigio
  }
}

//-------------------------------------------------------------------------
// U500Nexys4DDRDevKitFPGAChip
//-------------------------------------------------------------------------

class FPGAChip(implicit override val p: Parameters) extends NexysA7Shell with HasDDR2 {

  withClockAndReset(dut_clock, dut_reset) {
    val coreplex = Module(LazyModule(new Subsystem).module)

    connectSPI      (coreplex)
    connectUART     (coreplex)
    connectMIG      (coreplex)

    //---------------------------------------------------------------------
    // GPIO
    //---------------------------------------------------------------------

    val gpioParams = p(PeripheryGPIOKey)
    val gpio_0_pins = Wire(new GPIOPins(() => PinGen(), gpioParams(0)))
    val gpio_1_pins = Wire(new GPIOPins(() => PinGen(), gpioParams(1)))
    val gpio_2_pins = Wire(new GPIOPins(() => PinGen(), gpioParams(2)))
    val gpio_3_pins = Wire(new GPIOPins(() => PinGen(), gpioParams(3)))

    GPIOPinsFromPort(gpio_0_pins, coreplex.gpio(0))
    GPIOPinsFromPort(gpio_1_pins, coreplex.gpio(1))
    GPIOPinsFromPort(gpio_2_pins, coreplex.gpio(2))
    GPIOPinsFromPort(gpio_3_pins, coreplex.gpio(3))
    
    val led_wires = Wire(Vec(16, Bool()))

    gpio_0_pins.pins.foreach { _.i.ival := Bool(false) }
    gpio_0_pins.pins.zipWithIndex.foreach {
      case(pin, idx) => {
				if(idx < 16)
					led_wires(idx) := pin.o.oval
				else{
					idx match {
						case 16 => rgb0_r := pin.o.oval
						case 17 => rgb0_g := pin.o.oval
						case 18 => rgb0_b := pin.o.oval
						case 19 => rgb1_r := pin.o.oval
						case 20 => rgb1_g := pin.o.oval
						case 21 => rgb1_b := pin.o.oval
					}
				}
			}
    }
    LED := led_wires.asUInt
    
    gpio_1_pins.pins.zipWithIndex.foreach {
      case(pin, idx) => {
					idx match {
						case 16 => pin.i.ival := btn_u
						case 17 => pin.i.ival := btn_d
						case 18 => pin.i.ival := btn_l
						case 19 => pin.i.ival := btn_r
						case 20 => pin.i.ival := btn_c
						case no => pin.i.ival := SW(idx)
					}
			}
    }
    
    gpio_2_pins.pins.zipWithIndex.foreach {
      case(pin, idx) => {
				if(idx < 4)
					IOBUF(JA(idx+1), pin)
				else if(idx < 8)
					IOBUF(JA(idx+3), pin)
				else if(idx < 12)
					IOBUF(JB(idx-7), pin)
				else if(idx < 16)
					IOBUF(JB(idx-5), pin)
				else if(idx < 20)
					IOBUF(JC(idx-15), pin)
				else if(idx < 24)
					IOBUF(JC(idx-13), pin)
				else if(idx < 28)
					IOBUF(JD(idx-23), pin)
				else if(idx < 32)
					IOBUF(JD(idx-21), pin)	
			}
    }
    
    val an_wires = Wire(Vec(8, Bool()))
    val ca_wires = Wire(Vec(8, Bool()))
    gpio_3_pins.pins.foreach { _.i.ival := Bool(false) }
    gpio_3_pins.pins.zipWithIndex.foreach {
      case(pin, idx) => {
				if(idx < 8)
					an_wires(idx) := pin.o.oval
				else
					ca_wires(idx-8) := pin.o.oval
      }
    }
    AN := an_wires.asUInt
    CA := ca_wires.asUInt
  }

}
