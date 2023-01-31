@echo off
REM Keith Cascio 103102
REM Sean Soria 052103

REM INSERT YOUR CLASSPATH TO INFLIB CLASS FILES HERE
set cpath=..\inflib\compiled
set dirname=compiled
set joptions=-d %dirname% -classpath %cpath% @files.txt

if not exist %cpath%\nul.ext goto cpatherror
if not exist %dirname%\nul.ext goto direrror

:compile
echo Compiling samiam...
javac %joptions%
if not ERRORLEVEL 1 call make_jar
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

:end