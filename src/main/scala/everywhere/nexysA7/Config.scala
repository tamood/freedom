// See LICENSE for license details.
package sifive.freedom.everywhere.nexysA7

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._
import freechips.rocketchip.tile._

import sifive.blocks.devices.mockaon._
import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pwm._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.i2c._
import sifive.blocks.devices.timer._

case object FPGAFrequencyKey extends Field[Double](100.0)

class AtMHz(MHz: Double) extends Config((site, here, up) => {
  case FPGAFrequencyKey => MHz
  case PeripheryBusKey => up(PeripheryBusKey).copy(dtsFrequency = Some(BigDecimal(1000000*MHz).setScale(0, BigDecimal.RoundingMode.HALF_UP).toBigInt))
  case RocketTilesKey => up(RocketTilesKey) map { r =>
    r.copy(core = r.core.copy(bootFreqHz = BigDecimal(1000000*MHz).setScale(0, BigDecimal.RoundingMode.HALF_UP).toBigInt))
  }
})

class CoreConfig extends Config(
  new WithNExtTopInterrupts(0)   ++
  new WithNBreakpoints(2)        ++
  new WithTimebase(32768)        ++
  new WithJtagDTM                ++
  new TinyConfig
)

class PeripheralsConfig extends Config((site, here, up) => {
  case PeripheryTimerKey => List(
    TimerParams(address = 0x10060000),
    TimerParams(address = 0x10061000))
  case PeripheryPWMKey => List(
    PWMParams(address = 0x10020000),
    PWMParams(address = 0x10021000),
    PWMParams(address = 0x10022000))
  case PeripheryUARTKey => List(
    UARTParams(address = BigInt(0x10050000), stopBits = 1),
    UARTParams(address = BigInt(0x10051000), stopBits = 1))
  case PeripherySPIKey => List(
    SPIParams(rAddress = BigInt(0x10040000)),
    SPIParams(rAddress = BigInt(0x10041000)))
  case PeripherySPIFlashKey => List(
    SPIFlashParams(
      fAddress = 0x20000000,
      fSize = 0x1000000,
      rAddress = 0x10042000,
      defaultSampleDel = 3)) 
  case PeripheryI2CKey => List(
    I2CParams(address = BigInt(0x10030000)),
    I2CParams(address = BigInt(0x10031000)))  
  case PeripheryGPIOKey => List(
    GPIOParams(address = BigInt(0x10010000), width = 22, includeIOF = true),
    GPIOParams(address = BigInt(0x10011000), width = 21),
    GPIOParams(address = BigInt(0x10012000), width = 32, includeIOF = true),
    GPIOParams(address = BigInt(0x10013000), width = 16))
  case PeripheryMockAONKey =>
    MockAONParams(address = 0x10000000)
  case PeripheryMaskROMKey => List(
    MaskROMParams(address = 0x10000, name = "BootROM", depth = 2*1024))
})


class BoardConfig extends Config(
  new AtMHz(80) ++
  new PeripheralsConfig ++
  new CoreConfig().alter((site,here,up) => {
    case PLICKey => Some(PLICParams(intStages = 1))
    case JtagDTMKey => new JtagDTMConfig (
      idcodeVersion = 2,
      idcodePartNum = 0x123,
      idcodeManufId = 0x489,
      debugIdleCycles = 5)
  })
)

