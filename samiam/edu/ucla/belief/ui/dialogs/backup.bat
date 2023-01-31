@echo off

rem author: keith cascio, since: Thursday, December 06, 2007, 10:41:51 AM
rem
rem make intermediate backups of probability rewrite/visibility files between cvs commits
rem

call mypaths.bat

for /f %%i in ('"%EXECBINPATH%\date.exe" +%%Y%%m%%d_%%H%%M%%S_ProbabilityRewrite_Definitions.zipp') do set FNAME=%%i

rem echo %FNAME%
rem goto :eof

"%JDK6BINPATH%\jar.exe" cf "%FNAME%" ProbabilityRewrite.java Definitions.java LabelConstrained.java EnumModels.java VisibilityAdapter.java Menus.java

attrib +R "%FNAME%"
