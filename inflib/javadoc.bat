@echo off

rem keith cascio 20081029

setlocal
vol >nul 2>&1
call mypaths.bat >nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set               TIGHT=INFLIB v1.0 Public API at ????-??-?? ??:??:??
set           EXEC_DATE=%EXECBINPATH%\date.exe
if not exist %EXEC_DATE% (
  set         EXEC_DATE=c:\keith\code\bin\date.exe
)
if     exist %EXEC_DATE% (
  for /f "delims=#" %%i in ('%EXEC_DATE% "+INFLIB v1.0 Public API at %%Y-%%m-%%d %%H:%%M:%%S"') do set TIGHT=%%i
)

set   LINKS=-link "http://java.sun.com/javase/6/docs/api"
set  TITLEZ=-doctitle "%TIGHT%" -windowtitle "%TIGHT%" -bottom "Copyright 2008 <a href='http://reasoning.cs.ucla.edu'>UCLA Automated Reasoning Group</a>"

set options=-author -nosince -notree -public -noqualifier all -nohelp -verbose -use %LINKS% %TITLEZ%

set dirname=docs
set cpath=%INFLIBPATH%\compiled
set jcmd="%JDK6BINPATH%\javadoc.exe" %options% -d %dirname% -sourcepath . -classpath %cpath% @files.txt @files5.txt

if not exist %dirname%\nul.ext goto direrror

:compile
echo Documenting inflib...
@echo on
%jcmd%
@rem echo %ERRORLEVEL%
@rem @if errorlevel 1 goto end
@echo off
@rem if not ERRORLEVEL 1 goto openhtml
goto openhtml
goto end

:direrror
if exist %dirname% goto remfile
echo Creating '%dirname%' directory.
mkdir %dirname%
goto compile

:remfile
echo Please remove file '%dirname%'.
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:openhtml
start %dirname%\index.html
goto end

:end
endlocal
