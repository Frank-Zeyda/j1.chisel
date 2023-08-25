################################################################################
# Makefile to automate Clafer instance and j1.chisel settings file generation  #
################################################################################
# Possible commands are: make [default|instances|settings|clean]

# Clafer file to be used for instance generation.
CLAFER_FILE = j1.clf

# Detect version of the install Clafer tool-chain.
CLAFER_VERSION := $(shell claferIG --version)

# Verify that the Clafer tools 0.4.5 are installed.
ifneq ($(CLAFER_VERSION),ClaferIG 0.4.5)
${error Please install version 0.4.5 of the Clafer tools \
(from https://gsd.uwaterloo.ca/clafer-tools-binary-distributions) \
and add in the installation location to PATH}
endif

# Include makefile defining ANSI escape sequences.
include makefile.ansi

# The default target creates instances and converts them to settings.
default: clean instances settings

# Populates the 'instances' subfolder with j1.clf.<N>.data files.
instances:
	$(call note,Generating Clafer instances from \
		$(ANSI_BLUE)$(CLAFER_FILE)$(ANSI_RESET))
	claferIG --savedir=instances --addtypes --all $(CLAFER_FILE)

# Populates the 'settings' subfolder with j1.<N>.conf files.
settings:
	${call note,Converting instance (.data) files to j1 settings (.conf) files}
	python3 scripts/mk-settings.py -c j1_inst_one

# Deletes all dynamically-generated files in 'instances' and 'settings'.
clean:
	$(call note,Removing all instance and settings files)
	rm -f instances/*.data settings/*.conf

.PHONY: default clean instances settings
