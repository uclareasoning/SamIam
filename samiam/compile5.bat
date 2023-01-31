@echo off
setlocal
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set SPATH=%SAMIAMPATH%
set CPATH=%INFLIBPATH%\compiled
rem set OTHER_OPTIONS=-verbose
rem set OTHER_OPTIONS=-g -deprecation
rem set OTHER_OPTIONS=-g -source 1.4 -Xlint:unchecked -Xlint:deprecation
set OTHER_OPTIONS=-g

set dirname=compiled
set joptions=%OTHER_OPTIONS% -d "%dirname%" -sourcepath "%SPATH%" -classpath "%CPATH%" @files.txt @files5.txt

if not exist %cpath%\nul.ext goto cpatherror
if not exist %dirname%\nul.ext goto direrror

:compile
echo Compiling samiam...
@echo on
"%JDK6BINPATH%\javac.exe" %joptions%
@if not ERRORLEVEL 1 call makejar5.bat
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
