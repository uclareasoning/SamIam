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
#     keith cascio 20060315 (UCLA Automated Reasoning Group)
#*******************************************************************************

# Makefile for creating the JVMPI access JNI DLL.

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

!include <ntwin32.mak>

# Define the object modules to be compiled and flags.
OBJS   = jvm_profiler.obj ajvm_profiler.obj
LFLAGS = $(dlllflags) /BASE:0x6CFF0000
RES    = jvm_profiler.res
EXEC   = jvm_profiler.dll
DEBUG  = #$(cdebug)
acflags = -I"$(JDK15BASEPATH)\include" -I"$(JDK15BASEPATH)\include\win32" \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	/O1 \
	$(cflags)
wcflags = -DUNICODE $(acflags)

all: $(EXEC)

jvm_profiler.obj: ../jvm_profilerOS.h ../jvm_profiler.cpp
    $(cc) $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../jvm_profiler.cpp

ajvm_profiler.obj: ../jvm_profilerOS.h ../jvm_profiler.cpp
    $(cc) $(DEBUG) $(acflags) $(cvarsmt) /Foajvm_profiler.obj ../jvm_profiler.cpp

$(EXEC): $(OBJS) $(RES)
    $(link) $(LFLAGS) -out:$(PROGRAM_OUTPUT) $(OBJS) $(RES) $(LIBS)

$(RES): jvm_profiler.rc
    $(rc) -r -fo $(RES) jvm_profiler.rc

install: all
	copy $(EXEC) $(OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS) $(RES)

clean:
	del $(EXEC) $(OBJS) $(RES)
