@echo off

setlocal
vol >nul 2>&1
call mypaths.bat >nul 2>&1
if errorlevel 1 goto error_missing_mypaths

if exist files.txt goto expand
echo Error - files.txt not found!
goto end

:expand
if exist filesexpanded.txt del filesexpanded.txt
for /F %%f in ( files.txt ) do echo %SAMIAMPATH%\%%f >> filesexpanded.txt

if exist files5.txt goto expand5
echo Error - files5.txt not found!
goto end

:expand5
if exist files5expanded.txt del files5expanded.txt
for /F %%f in ( files5.txt ) do echo %SAMIAMPATH%\%%f >> files5expanded.txt

set SPATH=%SAMIAMPATH%
set CPATH=%INFLIBPATH%\compiled
if not exist %CPATH%\nul.ext goto cpatherror

set dirname=compiled
if not exist %dirname%/nul.ext goto direrror

rem set OTHER_OPTIONS=-Xlint:unchecked -Xlint:deprecation
set j4options=-g -source 1.4 -target 1.4 -d "%dirname%" -sourcepath "%SPATH%" -classpath "%CPATH%"                       -bootclasspath "%JRE14BOOTCLASSPATH%" @filesexpanded.txt
set j5options=-g -source 1.5 -target 1.5 -d "%dirname%" -sourcepath "%SPATH%" -classpath "%CPATH%;%SAMIAMPATH%\compiled"                                       @files5expanded.txt

:compile
echo Compiling samiam...
@echo on
"%JDK6BINPATH%\javac.exe" %j4options%
@if errorlevel 1 goto end
"%JDK6BINPATH%\javac.exe" %j5options%
@echo off
goto end

:direrror
if exist %dirname% goto remfile
echo Creating '%dirname%' directory.
mkdir %dirname%
goto compile

:cpatherror
echo Please set classpath to compiled inflib files in '%0'.
goto end

:remfile
echo Please remove file '%dirname%'.
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:end
endlocal
