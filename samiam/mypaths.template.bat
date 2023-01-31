@echo off
rem #####################################################################
rem #                                                                   #
rem # Note: Please DO NOT surround values with quotes.                  #
rem #       Quotes are not necessary, and they break some scripts.      #
rem #                                                                   #
rem #####################################################################
set SOURCEBASEPATH=unknown
set SAMIAMPATH=%SOURCEBASEPATH%\samiam
set INFLIBPATH=%SOURCEBASEPATH%\inflib
set PRIMULAPATH=%SOURCEBASEPATH%\primula
set BATCHPATH=%SOURCEBASEPATH%\batch
set ACEBASEPATH=unknown
set JDK14BASEPATH=unknown
set JRE14BASEPATH=%JDK14BASEPATH%\jre
set JRE14BINPATH=%JRE14BASEPATH%\bin
set JDK14BINPATH=%JDK14BASEPATH%\bin
set JRE14BOOTCLASSPATH=%JRE14BASEPATH%\lib\rt.jar
set JDK15BASEPATH=unknown
set JRE15BASEPATH=%JDK15BASEPATH%\jre
set JRE15BINPATH=%JRE15BASEPATH%\bin
set JDK15BINPATH=%JDK15BASEPATH%\bin
set JRE15BOOTCLASSPATH=%JRE15BASEPATH%\lib\rt.jar
set JDK6BASEPATH=unknown
set JRE6BASEPATH=%JDK6BASEPATH%\jre
set JRE6BINPATH=%JRE6BASEPATH%\bin
set JDK6BINPATH=%JDK6BASEPATH%\bin
set JRE6BOOTCLASSPATH=%JRE6BASEPATH%\lib\rt.jar
set JREBINPATH=%JDK14BINPATH%
set JDKBINPATH=%JREBINPATH%
set NETWORKSPATH=unknown
set SMILEPATH32=unknown
set SMILEPATH64=unknown
set HUGINAPIBASEPATH=unknown
set HUGINAPIJARPATH=%HUGINAPIBASEPATH%\Lib\hapi64.jar
set JAVACCBASEPATH=unknown
set EXECBINPATH=unknown
vol >nul 2>&1
