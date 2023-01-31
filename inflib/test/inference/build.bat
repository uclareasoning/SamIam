@echo off

rem author keith cascio, since 20060705

rem build win32 hugin calculator, single and double precision versions
rem
rem build requires ported win32 versions of getopt.h, getopt.c
rem build requires export libraries hugincpp.lib (single precision) hugincpp2.lib (double precision)
rem executable program hugindouble.exe requires hugincpp2.dll
rem executable program huginsingle.exe requires hugincpp.dll

echo.
echo hugin calculator win32 build script by keith cascio

vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

if not defined HUGINCPPINCLUDEPATH goto error_missing_variables

:compile
echo.
echo compiling source files...
cl -nologo /EHsc -I"." -c getopt.c
if errorlevel 1 goto :EOF
echo success, compiled getopt.c
cl -nologo /EHsc -I"." -I"%HUGINCPPINCLUDEPATH%" -DH_DOUBLE /FoAgainstHuginDouble.obj -c AgainstHugin.cpp
if errorlevel 1 goto :EOF
echo success, compiled AgainstHugin.cpp, double-precision
cl -nologo /EHsc -I"." -I"%HUGINCPPINCLUDEPATH%"            /FoAgainstHuginSingle.obj -c AgainstHugin.cpp
if errorlevel 1 goto :EOF
echo success, compiled AgainstHugin.cpp, single-precision

:link
echo.
echo linking double-precision win32 hugin calculator...
link /NOLOGO /INCREMENTAL:NO /LIBPATH:"%HUGINCPPLIBPATH%" /OUT:hugindouble.exe hugincpp2.lib getopt.obj AgainstHuginDouble.obj
if errorlevel 1 goto :EOF
echo success, linked hugindouble.exe
echo linking single-precision win32 hugin calculator...
link /NOLOGO /INCREMENTAL:NO /LIBPATH:"%HUGINCPPLIBPATH%" /OUT:huginsingle.exe hugincpp.lib  getopt.obj AgainstHuginSingle.obj
if errorlevel 1 goto :EOF
echo success, linked huginsingle.exe
goto :EOF

:error_missing_mypaths
  echo.
  echo Error: unable to find or execute mypaths.bat
goto :EOF

:error_missing_variables
  echo.
  echo Error: env var HUGINCPPINCLUDEPATH must be defined in mypaths.bat
goto :EOF
