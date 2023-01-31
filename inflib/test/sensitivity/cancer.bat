@echo off

@rem launches sensitivity completeness/soundness test for cancer.net
@rem author keith cascio, since 20060208

vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

call testcompleteness.bat -network %NETWORKSPATH%\cancer.net -iterations 64 -soundness all,stats %*
goto :EOF

:error_missing_mypaths
  echo Error: unable to find or execute mypaths.bat
goto :EOF
