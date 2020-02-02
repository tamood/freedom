// See LICENSE for license details.
package sifive.freedom.unleashed.nexysA7

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._
import freechips.rocketchip.tile._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._

import sifive.fpgashells.devices.xilinx.nexysA7mig._

class CoreConfig extends Config(
  new WithNExtTopInterrupts(0)   ++
  new WithNMemoryChannels(1) ++
  new WithExtMemSize(0x8000000L) ++
  new WithoutFPU ++
  new WithNBigCores(2)       ++
  new WithDTS("Sifive Freedom U500 dualcore", List("sifive,u500-dualcore")) ++
  new BaseConfig
)

class PeripheralsConfig extends Config((site, here, up) => {
  case PeripheryDDRMIGKey => DDRMIGParams(address = Seq(AddressSet(0x80000000L,0x8000000L-1)))
  case PeripheryUARTKey => List(
    UARTParams(address = BigInt(0x14000000L), nTxEntries = 512))
  case PeripherySPIKey => List(
    SPIParams(rAddress = BigInt(0x14001000L)))
  case PeripheryGPIOKey => List(
    GPIOParams(address = BigInt(0x14002000L), width = 22),
    GPIOParams(address = BigInt(0x14003000L), width = 21),
    GPIOParams(address = BigInt(0x14004000L), width = 32),
    GPIOParams(address = BigInt(0x14005000L), width = 16))
  case PeripheryMaskROMKey => List(
    MaskROMParams(address = 0x10000, name = "BootROM", depth = 16*1024))
})


class BoardConfig extends Config(
  new PeripheralsConfig ++
  new CoreConfig().alter((site,here,up) => {
  case RocketTilesKey => up(RocketTilesKey, site) map { r =>
    r.copy(core = r.core.copy(bootFreqHz = 50000000L))
  }
})
)
