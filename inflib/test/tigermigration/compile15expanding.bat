@echo off

setlocal
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set dirname=compiled
set wdir=.
set SPATH=%wdir%
set DEST=%wdir%\%dirname%
set CPATH=%wdir%\%dirname%
set CPATH=%CPATH%;%JDK15BASEPATH%\lib\tools.jar
set FILES=%wdir%\files.txt
set EXPANDED=%wdir%\filesexpanded.txt

if exist %FILES% goto expand
echo Error - %FILES% not found!
goto end

:expand
if exist %EXPANDED% del %EXPANDED%
for /F %%f in ( %FILES% ) do echo %wdir%\%%f >> %EXPANDED%

rem set OTHER_OPTIONS=-g -source 1.4 -target 1.4 -bootclasspath "%JRE14BOOTCLASSPATH%"
set OTHER_OPTIONS=-g -source 1.5 -target 1.5 -Xlint:unchecked

if not exist %CPATH%/nul.ext goto direrror

set joptions=%OTHER_OPTIONS% -d "%DEST%" -sourcepath "%SPATH%" -classpath "%CPATH%" "@%EXPANDED%"

:compile
echo Compiling structure subset of inflib, target 5...
@echo on
"%JDK15BINPATH%\javac.exe" %joptions%
@echo off
goto end

:direrror
if exist %CPATH% goto remfile
echo Creating '%CPATH%' directory.
mkdir %CPATH%
goto compile

:remfile
echo Please remove file '%CPATH%'.
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:end
endlocal
