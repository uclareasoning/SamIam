##############################################################################
# Script to auto-detect the platform: linux, solaris or mac
# Keith Cascio 020304
# Also, call gmake with one of these symbols defined: { __linux__, __sun__, __macosx__ }
##############################################################################

STR_SOLARIS_UNAME       = SunOS
STR_LINUX_UNAME         = Linux
STR_MACOSX_UNAME	= Mac

ifndef __sun__
ifndef __linux__
ifndef __macosx__

PLATFORM = $(shell uname -a)
ifneq (,$(findstring $(STR_LINUX_UNAME),$(PLATFORM)))
$(warning Auto-detected platform: $(PLATFORM), defining __linux__ )
__linux__=1
endif
ifneq (,$(findstring $(STR_SOLARIS_UNAME),$(PLATFORM)))
$(warning Auto-detected platform: $(PLATFORM), defining __sun__ )
__sun__=1
endif
ifneq (,$(findstring $(STR_MACOSX_UNAME),$(PLATFORM)))
$(warning Auto-detected platform: $(PLATFORM), defining __macosx__ )
__macosx__=1
endif

endif
endif
endif

ifndef __sun__
ifndef __linux__
ifndef __macosx__
$(warning Warning: makefile failed to auto-detect platform )
$(error Error: you must define either __linux__, __sun__, or __macosx__.  Try calling 'make_linux' or 'make_solaris')
endif
endif
endif
