@echo off

rem launches sensitivity completeness/soundness test
rem author keith cascio, since 20060208

vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set INFLIBCOMPILED=%INFLIBPATH%\compiled
set CPATH=%INFLIBCOMPILED%

set JAVAEX="%JRE6BINPATH%\java.exe"

@echo on
%JAVAEX% -Xms64m -Xmx512m -classpath %CPATH% edu.ucla.belief.sensitivity.TestCompleteness %*
@echo off
goto :EOF

:error_missing_mypaths
  echo Error: unable to find or execute mypaths.bat
goto :EOF
