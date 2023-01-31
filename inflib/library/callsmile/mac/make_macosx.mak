#**********************************************************************
# Copyright (c) 2000, 2005 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
#   
# Contributors: 
#     Kevin Cornell (Rational Software Corporation)
#     keith cascio 20060327 (UCLA Automated Reasoning Group)
#********************************************************************** 

# Makefile for creating the Carbon UPitt "SMILE" wrapper shared object JNI library.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
#
# The following environment variables can be set in "mypaths.bash" (via "build.bash" if you like)
#
# JDK14BASEPATH   - path to the base installation directory for a v4 JDK
# SMILEPATH       - path to UPitt "SMILE" header files and libraries (libsmile.a and libsmilexml.a)

# Define the object modules to be compiled and flags.
CC=g++
OBJS = callsmile.o
EXEC = $(PROGRAM_OUTPUT)
#LIBS = -framework Carbon
CFLAGS = -O -s -fno-exceptions -fno-rtti \
	-Wall \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
        -I"$(JDK14BASEPATH)/include" \
        -I"$(SMILEPATH)"

LFLAGS = -dynamiclib -static-libgcc -dynamic -L"$(SMILEPATH)"
LDLIBS = -lsmile

all: $(EXEC)

callsmile.o: ../callsmile.cpp
	$(CC) $(CFLAGS) -c ../callsmile.cpp

$(EXEC): $(OBJS)
	$(CC) -o $(EXEC) $(LFLAGS) $(OBJS) $(LDLIBS)

install: all
	cp $(EXEC) $(OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS)

clean:
	rm -f $(EXEC) $(OBJS)
