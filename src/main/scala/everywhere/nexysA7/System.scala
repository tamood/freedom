// See LICENSE for license details.
package sifive.freedom.everywhere.nexysA7

import Chisel._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._

import sifive.blocks.devices.mockaon._
import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pwm._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.i2c._
import sifive.blocks.devices.timer._

trait HasMetalLEDs { this: Subsystem =>
  (List("LD0red", "LD0green", "LD0blue") zip List(21, 20, 19)) map { case (led_name, pin) =>
    new SimpleDevice("led@" + pin, List("sifive,gpio-leds")) {
      override def parent = Some(ResourceAnchors.soc)
      override def describe(resources: ResourceBindings): Description = {
        val Description(name, mapping) = super.describe(resources)
        val extra = Map(
          "label"  -> Seq(ResourceString(led_name)),
          "gpios"  -> Seq(ResourceReference(gpios(0).device.label), ResourceInt(pin)),
          "linux,default-trigger" -> Seq(ResourceString("none")))
        Description(name, mapping ++ extra)
      }	  
      ResourceBinding { Resource(this, "exists").bind(ResourceString("yes")) }
    }
  }	
}

trait HasMetalInSPIFlash { this: Subsystem =>
  new FlashDevice(qspis(0).device){
    ResourceBinding { Resource(this, "exists").bind(ResourceString("yes")) }
  }	
}

trait HasBootROM { this: Subsystem =>
  val bootROM  = p(BootROMLocated(location)).map { BootROM.attach(_, this, CBUS) }
}

//-------------------------------------------------------------------------
// Outer Wrap
//-------------------------------------------------------------------------

class Subsystem(implicit p: Parameters) extends RocketSubsystem
    with HasPeripheryUART
    with HasPeripherySPI
    with HasPeripherySPIFlash
    with HasPeripheryGPIO
    with HasPeripheryPWM
    with HasPeripheryI2C
    with HasPeripheryTimer
    with HasPeripheryMockAON
    with HasBootROM
    with HasMetalInSPIFlash
    with HasMetalLEDs
    {

  override lazy val module = new SubsystemModuleImp(this)

}

//-------------------------------------------------------------------------
// Inner Module Implementation
//-------------------------------------------------------------------------

class SubsystemModuleImp[+L <: Subsystem](outer: L)
  extends RocketSubsystemModuleImp(outer)
    with HasPeripheryUARTModuleImp
    with HasPeripherySPIModuleImp
    with HasPeripherySPIFlashModuleImp
    with HasPeripheryGPIOModuleImp
    with HasPeripheryPWMModuleImp
    with HasPeripheryI2CModuleImp
    with HasPeripheryMockAONModuleImp
    with HasRTCModuleImp
