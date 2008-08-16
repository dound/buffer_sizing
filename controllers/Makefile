# simple Makefile to build of our controllers

# build a list of all sub-directories
PROJECTS = $(shell ls -l | grep "^d" | cut -d: -f2- | cut -d\  -f2)
CLEAN_PROJECTS = $(foreach dir,$(PROJECTS),clean$(dir))
INSTALL_PROJECTS = $(foreach dir,$(PROJECTS),install$(dir))

.PHONY: all $(PROJECTS) clean $(CLEAN_PROJECTS) install $(INSTALL_PROJECTS)

# build all the projects
all:
	@$(MAKE) --no-print-directory $(PROJECTS)

# cleanup all the byproducts
clean:
	@$(MAKE) --no-print-directory $(CLEAN_PROJECTS)

# build the project in the specified directory using its default rule
$(PROJECTS):
	@$(MAKE) --no-print-directory -C $@

# cleans up the project in the specified directory using its 'clean' rule
$(CLEAN_PROJECTS):
	@$(MAKE) --no-print-directory -C `echo $@ | sed -e "s#^clean##"` clean
