@echo off

rem author keith cascio, since 20060315

setlocal
vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set CPATH=%INFLIBPATH%\compiled
set CPATH=%CPATH%;%SAMIAMPATH%\compiled

rem set JAVA_OPS=-Xms64m -Xmx1024m -cp %CPATH%
set JAVA_OPS=-Xrunjvm_profiler -cp %CPATH%

@echo on
"%JRE15BINPATH%\java.exe" %JAVA_OPS% edu.ucla.util.JVMProfiler %*
@echo off
goto end

:error_missing_mypaths
  echo Error: unable to find or execute mypaths.bat
goto end

:end
endlocal
