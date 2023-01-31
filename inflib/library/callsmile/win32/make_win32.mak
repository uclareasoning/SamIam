#*******************************************************************************
# Copyright (c) 2000, 2005 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
#     keith cascio 20060316 (UCLA Automated Reasoning Group)
#*******************************************************************************

# Makefile for creating the UPitt "SMILE" wrapper JNI DLL.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
#
# The following environment variables can be set in "mypaths.bat" (via "build.bat" if you like)
#
# JDK15BASEPATH   - path to the base installation directory for a v5 JDK
# SMILEPATH       - path to UPitt "SMILE" header files and libraries (smile.lib and smilexml.lib)

!include <ntwin32.mak>

!if ("$(CPU)" == "AMD64")
SMILEPATH = "$(SMILEPATH64)"
!else
SMILEPATH = "$(SMILEPATH32)"
!endif

# Define the object modules to be compiled and flags.
OBJS   = callsmile.obj acallsmile.obj
LFLAGS = $(dlllflags) /LIBPATH:"$(SMILEPATH)" /BASE:0x6CF40000
RES    = callsmile.res
EXEC   = callsmile.dll
DEBUG  = $(cdebug)
acflags = -I"$(JDK15BASEPATH)\include" -I"$(JDK15BASEPATH)\include\win32" \
          -D_SECURE_SCL=0  \
          -I"$(SMILEPATH)" \
          -DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
          -DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
          -DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
          /O1 /EHsc \
          $(cflags)
wcflags = -DUNICODE $(acflags)

all: $(EXEC)

callsmile.obj: ../callsmileOS.h ../edu_ucla_belief_io_dsl_SMILEReader.h ../callsmile.cpp
    $(cc) $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../callsmile.cpp

acallsmile.obj: ../callsmileOS.h ../edu_ucla_belief_io_dsl_SMILEReader.h ../callsmile.cpp
    $(cc) $(DEBUG) $(acflags) $(cvarsmt) /Foacallsmile.obj ../callsmile.cpp

$(EXEC): $(OBJS) $(RES)
    $(link) $(LFLAGS) -out:$(PROGRAM_OUTPUT) $(OBJS) $(RES) $(LIBS)

$(RES): callsmile.rc
    $(rc) -r -fo $(RES) callsmile.rc

install: all
	copy $(EXEC) $(OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS) $(RES)

clean:
	del $(EXEC) $(OBJS) $(RES)
