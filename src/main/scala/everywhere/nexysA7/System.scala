// See LICENSE for license details.
package sifive.freedom.everywhere.nexysA7

import Chisel._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._

class Subsystem(implicit p: Parameters) extends RocketSubsystem
    with HasPeripheryMaskROMSlave
    with HasPeripheryUART
    with HasPeripherySPI
    with HasPeripheryGPIO {
  override lazy val module = new SubsystemModule(this)
  val chosen = new Device {
    def describe(resources: ResourceBindings): Description = {
      val entry = resources("entry").map(_.value)
      val text = resources("text").map(_.value)
      val data = resources("data").map(_.value)
      Description("chosen", Map(
        "metal,entry" -> entry,
        "metal,rom"   -> text,
        "metal,ram"   -> data))
    }
  }
  ResourceBinding {
    Resource(chosen, "text").bind(ResourceReference(maskROMs(0).node.portParams(0).managers(0).resources(0).owner.label))
    Resource(chosen, "data").bind(ResourceReference(tiles(0).dtim_adapter.get.device.label))
    Resource(chosen, "entry").bind(ResourceReference(maskROMs(0).node.portParams(0).managers(0).resources(0).owner.label))
    Resource(chosen, "entry").bind(ResourceInt(0))
  }
}

class SubsystemModule[+L <: Subsystem](_outer: L)
  extends RocketSubsystemModuleImp(_outer)
    with HasRTCModuleImp
    with HasPeripheryUARTModuleImp
    with HasPeripherySPIModuleImp
    with HasPeripheryGPIOModuleImp
    {
  // Reset vector is set to the location of the mask rom
  val maskROMParams = p(PeripheryMaskROMKey)
  global_reset_vector := maskROMParams(0).address.U
}
