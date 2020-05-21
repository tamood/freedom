bitstream := $(BUILD_DIR)/obj/$(MODEL)
$(bitstream): $(f)
	cd $(BUILD_DIR); vivado \
		-nojournal -mode batch \
		-source $(fpga_common_script_dir)/vivado.tcl \
		-tclargs \
		-top-module "$(MODEL)" \
		-F "$(f)" \
		-ip-vivado-tcls "$(shell find '$(BUILD_DIR)' -name '*.vivado.tcl')" \
		-board "$(BOARD)"

.PHONY: bitstream
bitstream: $(bitstream)

export RISCV_PATH=/home/tams/riscv
ESDKDIR ?= $(base_dir)/freedom-e-sdk
BSPDIR = $(ESDKDIR)/bsp/$(BSP)
DTSFILE=$(BSPDIR)/core.dts

.PHONY: buildbsp
buildbsp:
	mkdir -p $(ESDKDIR)/bsp/$(BSP)
	rm -rf $(ESDKDIR)/bsp/$(BSP)/*
	cp $(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).dts $(DTSFILE)
	$(ESDKDIR)/scripts/esdk-settings-generator/generate_settings.py -d $(DTSFILE) -t arty -o $(BSPDIR)/settings.mk
	$(ESDKDIR)/scripts/openocdcfg-generator/generate_openocdcfg.py -d $(DTSFILE) -b arty -p jtag -t -o $(BSPDIR)/openocd.cfg	
		
.PHONY: buildrom
buildrom:
	make -C $(ESDKDIR) PROGRAM=$(FIRMWARE) TARGET=$(BSP) CONFIGURATION=debug software
	cp $(ESDKDIR)/software/$(FIRMWARE)/debug/$(FIRMWARE).elf $(BUILD_DIR)/metal.elf


.PHONY: updaterom
updaterom: 
	python $(base_dir)/patchMMI/patchMMI.py --i $(BUILD_DIR)/obj/$(MODEL).mmi --o $(BUILD_DIR)/obj/$(MODEL)MMI.mmi --d $(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).json
	updatemem -meminfo $(BUILD_DIR)/obj/$(MODEL)MMI.mmi -data $(BUILD_DIR)/metal.elf -proc rocketchip -bit $(BUILD_DIR)/obj/$(MODEL).bit -out $(BUILD_DIR)/obj/$(MODEL)WP.bit -force

	
.PHONY: uploadrom
uploadrom: 
	vivado -mode batch -source $(fpga_common_script_dir)/upload.tcl -tclargs $(BUILD_DIR)/obj/$(MODEL)WP.bit
	
.PHONY: debugrom
debugrom:
	make -C $(ESDKDIR) PROGRAM=$(FIRMWARE) TARGET=$(BSP) debug	
	
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


