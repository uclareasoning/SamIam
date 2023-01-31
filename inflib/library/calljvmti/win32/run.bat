@echo off

setlocal
vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set CPATH=%INFLIBPATH%\compiled
set CPATH=%CPATH%;%SAMIAMPATH%\compiled

rem set JAVA_OPS=-Xms64m -Xmx1024m -cp %CPATH%
set JAVA_OPS=-Xruncalljvmti -cp %CPATH%

@echo on
"%JRE15BINPATH%\java.exe" %JAVA_OPS% edu.ucla.util.JVMTI %*
@echo off
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:end
endlocal
