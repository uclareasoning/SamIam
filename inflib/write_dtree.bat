@echo off
rem MS DOS Batch file for writing dtree
rem by Keith Cascio

rem usage: write_dtree -i<input path> [-o<output path>] [-m<memory limit in MB>]

rem example: write_dtree -ic:\networks\barley.net -oc:\dtrees\barley_dtree.dat -m20.1

rem NOTE: This program allows no white space between each command line flag and its argument.
rem e.g.   correct usage: "write_dtree -ic:\networks\cancer.net"
rem e.g. incorrect usage: "write_dtree -i c:\networks\cancer.net"

rem NOTE: The argument to -m must be a non-negative floating point number.  e.g. -m20, -m0.001, -m1024.5

rem Explanation of command-line flags:
rem
rem -Xms8m: Specify the initial size, in bytes, of the java virtual machine memory allocation pool = 8 Megs.
rem
rem -Xmx512m: Specify the maximum size, in bytes, of the java virtual machine memory allocation pool = 512 Megs.

java.exe -Xms8m -Xmx512m -classpath inflib.jar edu.ucla.belief.inference.RCInfo %*
