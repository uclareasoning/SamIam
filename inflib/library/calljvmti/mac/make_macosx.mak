#**********************************************************************
# Copyright (c) 2000, 2005 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Kevin Cornell (Rational Software Corporation)
#     keith cascio 20060509 (UCLA Automated Reasoning Group)
#**********************************************************************

# Makefile for creating the Carbon JVMTI access shared object JNI library.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
#
# The following environment variables can be set in "mypaths.bash" (via "build.bash" if you like)
#
# JDK15BASEPATH   - path to the base installation directory for a v5 JDK

# Define the object modules to be compiled and flags.
CC=gcc
OBJS = calljvmti.o
EXEC = $(PROGRAM_OUTPUT)
#LIBS = -framework Carbon
CFLAGS = -O -s \
	-Wall \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I"$(JDK15BASEPATH)/include"

LFLAGS = -dynamiclib -static-libgcc -dynamic

all: $(EXEC)

calljvmti.o: ../calljvmti.c
	$(CC) $(CFLAGS) -c ../calljvmti.c

$(EXEC): $(OBJS)
	$(CC) -o $(EXEC) $(LFLAGS) $(OBJS) $(LDLIBS)

install: all
	cp $(EXEC) $(OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS)

clean:
	rm -f $(EXEC) $(OBJS)
