echo on


if [%1]==[] (
set RUN_DIR="%CD%"
) else (
set RUN_DIR=%1
)

cd %RUN_DIR%

rem ---------------------------------------------
set BASEDIR=C:\Users\A793320\GCTI

set LIBDIR=%BASEDIR%\lib

set LOGDIR=%RUN_DIR%\.tmp

set VARDIR=%BASEDIR%\var

set ETCDIR=%BASEDIR%\etc\logbrowser

set JAVA_HOME='C:\Program Files\Amazon Corretto\jdk21.0.5_11'

set PATH=%JAVA_HOME%\bin;%PATH%

rem default name of the database

set LOGBRDB=logbr

rem set DBG=-Xdebug -Xrunjdwp:transport=dt_socket,address=0.0.0.0:8000,server=y,suspend=y
set LOG_OPTS=-Dlog4j.configurationFile=%ETCDIR%\logbr.log4j2.indexer.xml -DlogPath=%LOGDIR%
set MISC_OPTIONS=-Dsun.java2d.d3d=false -Dall=1 -Xms4G -Xmx16G

set SQLITE_PRAGMAS=-Dsqlite.pragma=true

set NO_TLIB_REQUESTS=-Dtlib.norequest=false

set TIMEDIFF=-Dtimediff.parse=false

rem set SIP_LINES=-DSIPLINES=1

rem ------------------ no config after this line -------------------------

set JAVA_OPTS=%DBG% %LOG_OPTS% %MISC_OPTIONS% %SQLITE_PRAGMAS% %NO_TLIB_REQUESTS% %TIMEDIFF% %SIP_LINES%

set DB=%RUN_DIR%\%LOGBRDB%

set CFG=%ETCDIR%\backend.xml

%BASEDIR%\bin\indexer.bat --dbname=%DB% --basedir=%RUN_DIR% --cfgxml=%CFG% --logbr.dir=%LOGDIR%

