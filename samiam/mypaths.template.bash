#!/bin/env bash

##############################################################################
#
# Define paths to the location of source code on your system.
#
#   If you checked out the inflib and samiam cvs modules
#     (and batchtool and/or primula) into a common directory /a/b/c, you can
#     simply define SOURCEBASEPATH=/a/b/c
#
##############################################################################
SOURCEBASEPATH=/unknown_please_set_me
    INFLIBPATH=${SOURCEBASEPATH}/inflib
    SAMIAMPATH=${SOURCEBASEPATH}/samiam
     BATCHPATH=${SOURCEBASEPATH}/batch
   PRIMULAPATH=${SOURCEBASEPATH}/primula

##############################################################################
#
# Define paths to the locations of installed software and data.
#
##############################################################################
     ACEBASEPATH=/unknown_please_set_me
    NETWORKSPATH=unknown_please_set_me
HUGINAPIBASEPATH=unknown_please_set_me
 HUGINAPIJARPATH=${HUGINAPIBASEPATH}/Lib/hapi64.jar

##############################################################################
#
# Define paths to the locations of the Java 4 JRE and JDK on your system.
#
#     The defaults provided here are likely guesses for Linux/Solaris.
#       Read below for Mac suggestions.
#
#     On Linux/Solaris, the JDK 4 install directory
#       is likely similar to:
#
#           JDK14BASEPATH=/usr/local/j2sdk1.4.2_06
#
#       On Mac, it is likely similar to:
#
#           JDK14BASEPATH=/System/Library/Frameworks/JavaVM.framework/Versions/1.4.2
#
#     It is unlikely, but possible that your system provides a JRE,
#       but no JDK.  If that is the case, you will not be able to
#       compile or bundle code until you install a JDK.
#       You can only run Java programs.
#
#     The defaults provided here assume a JRE bundled with the JDK.
#       However, if you have a more recent version of the JRE installed,
#       but not of the entire JDK, it would be useful to define a JRE
#       path completely unrelated to the JDK path.
#
#     Variable JRE14BOOTCLASSPATH is provided to assist scripts
#       that cross-compile, i.e. use javac 1.5 to create version 1.4 code.
#       On Linux/Solaris, use:
#
#           JRE14BOOTCLASSPATH=${JRE14BASEPATH}/lib/rt.jar
#
#       On Mac, use:
#
#           JRE14BOOTCLASSPATH=${JRE14BASEPATH}/classes/classes.jar
#
##############################################################################
JDK14BASEPATH=/usr/local/j2sdk1.4.2
JRE14BASEPATH=${JDK14BASEPATH}/jre
JDK14BINPATH=${JDK14BASEPATH}/bin
JRE14BINPATH=${JRE14BASEPATH}/bin
JRE14BOOTCLASSPATH=${JRE14BASEPATH}/lib/rt.jar

##############################################################################
#
# Define paths to the locations of the Java 5 JRE and JDK on your system.
#
##############################################################################
JDK15BASEPATH=/usr/local/jdk1.5.0
JRE15BASEPATH=${JDK15BASEPATH}/jre
JDK15BINPATH=${JDK15BASEPATH}/bin
JRE15BINPATH=${JRE15BASEPATH}/bin
JRE15BOOTCLASSPATH=${JRE15BASEPATH}/lib/rt.jar

##############################################################################
#
# Define paths to the locations of the Java 6 JRE and JDK on your system.
#
##############################################################################
JDK6BASEPATH=/usr/local/jdk1.6.0
JRE6BASEPATH=${JDK6BASEPATH}/jre
JDK6BINPATH=${JDK6BASEPATH}/bin
JRE6BINPATH=${JRE6BASEPATH}/bin
JRE6BOOTCLASSPATH=${JRE6BASEPATH}/lib/rt.jar

##############################################################################
#
# Define default paths for scripts that are not sensitive to the version of
#  of the JRE/JDK.
#
##############################################################################
JDKBINPATH=${JDK14BINPATH}
JREBINPATH=${JDKBINPATH}
