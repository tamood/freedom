bit := $(BUILD_DIR)/obj/$(MODEL).bit
$(bit): $(f)
	cd $(BUILD_DIR); vivado \
		-nojournal -mode batch \
		-source $(fpga_common_script_dir)/vivado.tcl \
		-tclargs \
		-top-module "$(MODEL)" \
		-F "$(f)" \
		-ip-vivado-tcls "$(shell find '$(BUILD_DIR)' -name '*.vivado.tcl')" \
		-board "$(BOARD)"

.PHONY: bit
bit: $(bit)

export RISCV_PATH=/home/tams/riscv
ESDKDIR ?= $(base_dir)/freedom-e-sdk
BSPDIR = $(ESDKDIR)/bsp/$(BSP)
DTSFILE=$(BSPDIR)/design.dts
DTBFILE=$(BSPDIR)/design.dtb


.PHONY: buildbsp
buildbsp:
	mkdir -p $(ESDKDIR)/bsp/$(BSP)
	cp $(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).dts $(DTSFILE)
	dtc -I dts -O dtb -o $(ESDKDIR)/bsp/$(BSP)/design.dtb $(ESDKDIR)/bsp/$(BSP)/design.dts
	$(ESDKDIR)/freedom-devicetree-tools/freedom-makeattributes-generator -d $(DTBFILE) -b rtl -o $(BSPDIR)/settings.mk
	$(ESDKDIR)/freedom-devicetree-tools/freedom-bare_header-generator -d $(DTBFILE) -o $(BSPDIR)/metal-platform.h
	$(ESDKDIR)/freedom-devicetree-tools/freedom-metal_header-generator -d $(DTBFILE) -o $(BSPDIR)/metal.h
	$(ESDKDIR)/freedom-devicetree-tools/freedom-ldscript-generator -d $(DTBFILE) -l $(BSPDIR)/metal.default.lds
	
.PHONY: buildrom
buildrom:
	make -C $(ESDKDIR) PROGRAM=$(FIRMWARE) TARGET=$(BSP) CONFIGURATION=release software
	cp $(ESDKDIR)/software/$(FIRMWARE)/release/$(FIRMWARE).elf $(BUILD_DIR)/metal.elf

.PHONY: updaterom
updaterom: 
	python $(FPGA_DIR)/common/utils/patchMMI.py --i $(BUILD_DIR)/obj/$(MODEL).mmi --o $(BUILD_DIR)/obj/$(MODEL)MMI.mmi --d $(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).json
	updatemem -meminfo $(BUILD_DIR)/obj/$(MODEL)MMI.mmi -data $(BUILD_DIR)/metal.elf -proc rocketchip -bit $(BUILD_DIR)/obj/$(MODEL).bit -out $(BUILD_DIR)/obj/$(MODEL)WP.bit -force

	
.PHONY: uploadrom
uploadrom: 
	vivado -mode batch -source $(fpga_common_script_dir)/upload.tcl -tclargs $(BUILD_DIR)/obj/$(MODEL)WP.bit
	
# ChipClean
.PHONY: chipclean
chipclean:
	rm -rf $(base_dir)/target
	rm -rf $(base_dir)/project
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


