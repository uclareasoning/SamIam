@echo off

@rem launches inference test against hugin for cancer.net
@rem author keith cascio, since 20060319

vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

call testagainsthugin.bat -network "%NETWORKSPATH%\cancer.oobn" -iterations 8 %*
goto :EOF

:error_missing_mypaths
  echo Error: unable to find or execute mypaths.bat
goto :EOF
