@echo off

@rem compile sensitivity tests
@rem author keith cascio, since 20060217

vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

if exist files.txt goto expand
echo Error - files.txt not found!
goto :EOF

:expand
if exist filesexpanded.txt del filesexpanded.txt
for /F %%f in ( files.txt ) do echo %INFLIBPATH%\%%f >> filesexpanded.txt

set SPATH=%INFLIBPATH%
set CPATH=%INFLIBPATH%\compiled
rem set OTHER_OPTIONS=-g -source 1.5 -target 1.5 -Xlint:unchecked -Xlint:deprecation
set OTHER_OPTIONS=-g -source 1.5 -target 1.5

set dirname=%CPATH%
set joptions=%OTHER_OPTIONS% -d %dirname% -sourcepath %SPATH% -classpath %CPATH% @filesexpanded.txt

if not exist %cpath%\edu\ucla\belief\BeliefNetwork.class goto cpatherror
if not exist %dirname%\nul.ext goto direrror

:compile
  echo Compiling sensitivity tests...
  @echo on
  "%JDK15BINPATH%\javac.exe" %joptions%
  @echo off
goto :EOF

:direrror
  if exist %dirname% goto remfile
  echo Creating '%dirname%' directory.
  mkdir %dirname%
goto compile

:cpatherror
  echo Please compile inflib first.
goto :EOF

:remfile
  echo Please remove file '%dirname%'.
goto :EOF

:error_missing_mypaths
  echo Error: unable to find or execute mypaths.bat
goto :EOF
