@echo off

rem launches inference test against hugin
rem author keith cascio, since 20060319

vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set INFLIBCOMPILED=%INFLIBPATH%\compiled
set CPATH=%INFLIBCOMPILED%;%HUGINAPIJARPATH%

set JAVAEX="%JRE15BINPATH%\java.exe"

@echo on
%JAVAEX% -D"java.library.path=.;%HUGINAPIBASEPATH%\Bin" -Xms256m -Xmx800m -classpath "%CPATH%" AgainstHugin %*
@echo off
goto :EOF

:error_missing_mypaths
  echo Error: unable to find or execute mypaths.bat
goto :EOF
