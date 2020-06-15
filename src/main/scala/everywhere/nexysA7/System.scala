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
    with HasPeripheryMockAON {
  override lazy val module = new SubsystemModuleImp(this)
  val memDevice = new FlashDevice(tlqspi(0).device){
	  ResourceBinding { Resource(this, "exists").bind(ResourceString("yes")) }
  }
  def resetVector: BigInt = p(PeripheryMaskROMKey)(0).address
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
    {
  // Reset vector is set to the location of the mask rom
  global_reset_vector := outer.resetVector.U
}
