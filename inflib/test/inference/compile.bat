@echo off

@rem compile inference test
@rem author keith cascio, since 20060319

vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

if exist files.txt goto expand
echo Error - files.txt not found!
goto :EOF

:expand
rem if exist filesexpanded.txt del filesexpanded.txt
rem for /F %%f in ( files.txt ) do echo %INFLIBPATH%\%%f >> filesexpanded.txt

set SPATH=%INFLIBPATH%;.
set dirname=%INFLIBPATH%\compiled
set CPATH=%dirname%;%HUGINAPIJARPATH%
rem set OTHER_OPTIONS=-g -source 1.5 -target 1.5 -Xlint:unchecked -Xlint:deprecation
set OTHER_OPTIONS=-g -source 1.5 -target 1.5

set joptions=%OTHER_OPTIONS% -sourcepath "%SPATH%" -d "%dirname%" -classpath "%CPATH%" @files.txt

if not exist %cpath%\edu\ucla\belief\BeliefNetwork.class goto cpatherror
if not exist %dirname%\nul.ext goto direrror

:compile
  echo Compiling inference tests...
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
