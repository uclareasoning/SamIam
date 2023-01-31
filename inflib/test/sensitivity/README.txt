author: keith cascio
since: 20060217

Running Sensitivity Tests
------------------------------------------------------------------------------------------------

I. SETUP

In order to run the sensitivity tests located here, you must compile and run them.
The compile/run scripts depend on the configuration file "mypaths"
(mypaths.bat on Windows, mypaths.bash on UNIX).

To configure mypaths:

1. Create a single file named mypaths.*** based on "mypaths.template.***" from the inflib module.
2. Put mypaths in a directory in your search path (e.g. ~/bin) so scripts can find it.
3. Edit mypaths, substituting the paths as they exist on your system.

II. BUILD

The source code files for the java classes that implement the sensitivity tests
are located in the inflib module, but they are not compiled when you compile
inflib.  At the time of this writing, inflib requires Java 4 whereas the test
classes require Java 5.  Compile them using the compile script in this directory:
compile.bat on Windows or compile.bash on UNIX.

III. RUN

The test programs are designed to be self-documenting.
If you run a test program with no options, it will print usage instructions.

You can find several example run scripts in this directory.

  a. Completeness/soundness test:

       i.  primary run script: testcompleteness.bat/testcompleteness.bash

       ii. example for cancer.net: cancer.bat/cancer.bash
             NOTE: This script relies on a new mypaths variable "NETWORKSPATH"
                   to find cancer.net!  If you want to run cancer.bat/bash,
                   make sure your local version of mypaths defines NETWORKSPATH.
