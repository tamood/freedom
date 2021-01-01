.PHONY: xpmrom
xpmrom: 
	cp $(BUILD_DIR)/$(BOOTROM).hex $(BUILD_DIR)/$(BOOTROM).mem
	python3 $(base_dir)/patchMMI/vlsi_rom_gen $(ROMCONF) $(BUILD_DIR)/$(BOOTROM).mem > $(romgen)

export RISCV_PATH=$(RISCV)
export FREEDOM_E_SDK_VENV_PATH=$(base_dir)/venv
ESDKDIR ?= $(base_dir)/freedom-e-sdk
BSPDIR = $(ESDKDIR)/bsp/$(BSP)
DTSFILE=$(BSPDIR)/core.dts

ZEPHYR_BASE ?= $(base_dir)/zephyr
ZEPHYR_ELF ?= $(ZEPHYR_BASE)/build/zephyr/zephyr.elf
OPENOCD ?= openocd

.PHONY: buildbsp
buildbsp:
	python3 -m venv venv
	mkdir -p $(ESDKDIR)/bsp/$(BSP)
	rm -rf $(ESDKDIR)/bsp/$(BSP)/*
	cp $(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).dts $(DTSFILE)
	make -C $(ESDKDIR) -f scripts/virtualenv.mk virtualenv
	. $(FREEDOM_E_SDK_VENV_PATH)/bin/activate && $(ESDKDIR)/scripts/esdk-settings-generator/generate_settings.py -d $(DTSFILE) -t arty -o $(BSPDIR)/settings.mk
	make -C $(ESDKDIR) TARGET=$(BSP) metal-bsp
	. $(FREEDOM_E_SDK_VENV_PATH)/bin/activate && $(ESDKDIR)/scripts/openocdcfg-generator/generate_openocdcfg.py -d $(BSPDIR)/design.dts -b arty -p jtag -t -o $(BSPDIR)/openocd.cfg
		
.PHONY: buildmetal
buildmetal:
	make -C $(ESDKDIR) PROGRAM=$(FIRMWARE) TARGET=$(BSP) CONFIGURATION=debug software
	cp $(ESDKDIR)/software/$(FIRMWARE)/debug/$(FIRMWARE).elf $(BUILD_DIR)/metal.elf

.PHONY: patchbootrom
patchbootrom: 
	python $(base_dir)/patchMMI/patchMMI.py --i $(BUILD_DIR)/obj/$(MODEL).mmi --o $(BUILD_DIR)/obj/$(MODEL)MMI.mmi --d $(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).json
	updatemem -meminfo $(BUILD_DIR)/obj/$(MODEL)MMI.mmi -data $(BUILD_DIR)/$(BOOTROM).elf -proc rocketchip -bit $(BUILD_DIR)/obj/$(MODEL).bit -out $(BUILD_DIR)/obj/$(MODEL).bit -force

.PHONY: configbit
configbit: 
	vivado -mode batch -source $(fpga_common_script_dir)/upload.tcl -tclargs $(BUILD_DIR)/obj/$(MODEL).bit
	
.PHONY: configbit_ocd
configbit_ocd:
	$(OPENOCD) -c "adapter_khz 5000" -f "interface/ftdi/arty-onboard-ftdi.cfg" -f "cpld/xilinx-xc7.cfg" -c "init; pld load 0 $(BUILD_DIR)/obj/$(MODEL).bit; shutdown"	

.PHONY: flashbit
flashbit: 
	cd $(BUILD_DIR); vivado -nojournal -mode batch -source $(fpga_common_script_dir)/memconfig.tcl -tclargs $(BUILD_DIR)/obj/$(MODEL).mcs
	
.PHONY: flashmetal
flashmetal:
	make -C $(ESDKDIR) PROGRAM=$(FIRMWARE) TARGET=$(BSP) upload		
	
.PHONY: debugmetal
debugmetal:
	make -C $(ESDKDIR) PROGRAM=$(FIRMWARE) TARGET=$(BSP) debug	
		
.PHONY: flashzephyr
flashzephyr:
	$(ESDKDIR)/scripts/upload --elf $(ZEPHYR_ELF) --openocd $(OPENOCD) --openocd-config $(BSPDIR)/openocd.cfg --gdb $(RISCV)/bin/riscv64-unknown-elf-gdb 	

.PHONY: debugzephyr
debugzephyr:
	$(ESDKDIR)/scripts/debug  --elf $(ZEPHYR_ELF) --openocd $(OPENOCD) --openocd-config $(BSPDIR)/openocd.cfg --gdb $(RISCV)/bin/riscv64-unknown-elf-gdb 	


.PHONY: chipclean
chipclean:
	rm -rf $(base_dir)/target
	rm -rf $(base_dir)/project/*
	rm -rf $(base_dir)/fpga-shells/target
	rm -rf $(base_dir)/sifive-blocks/target
	rm -rf $(base_dir)/nvidia-dla-blocks/target
	rm -rf $(base_dir)/rocket-chip/target
	rm -rf $(base_dir)/rocket-chip/project/target
	rm -rf $(base_dir)/rocket-chip/project/project
	rm -rf $(base_dir)/rocket-chip/macros/target
	rm -rf $(base_dir)/rocket-chip/hardfloat/target
	rm -rf $(base_dir)/rocket-chip/firrtl/target
	rm -rf $(base_dir)/rocket-chip/firrtl/project/target
	rm -rf $(base_dir)/rocket-chip/firrtl/project/project
	rm -rf $(base_dir)/rocket-chip/chisel3/target
	rm -rf $(base_dir)/rocket-chip/chisel3/coreMacros/target
	rm -rf $(base_dir)/rocket-chip/chisel3/chiselFrontend/target
	rm -rf $(base_dir)/rocket-chip/lib
	rm -rf $(base_dir)/rocket-chip/chisel3/lib
	rm -f $(base_dir)/rocket-chip/firrtl/utils/bin/firrtl.jar


