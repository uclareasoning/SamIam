@echo off
REM Keith Cascio 103102
REM Sean Soria 052103
echo Make sure the Java 1.4 SDK is in your path.

set dirname=compiled
if not exist %dirname%/nul.ext goto direrror

:compile
echo Compiling inflib...
javac -d %dirname% @files.txt
if not ERRORLEVEL 1 call make_jar
goto end

:direrror
if exist %dirname% goto remfile
echo Creating '%dirname%' directory.
mkdir %dirname%
goto compile

:remfile
echo Please remove file '%dirname%'.

:end