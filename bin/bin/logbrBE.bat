echo on
set BASEDIR=C:\gcti

set LIBDIR=%BASEDIR%\lib

set LOGDIR=$(pwd)\tmp

set VARDIR=%BASEDIR%\var

set ETCDIR=%BASEDIR%\etc\logbrowser

set JAVA_HOME=C:\Tools\graalvm-ce-java8-21.1.0

set PATH=%JAVA_HOME%\bin;%PATH%

rem default name of the database

set LOGBRDB=logbr

set LOGBR_TMP=.tmp

set LOGBR_TMP_OPT=-Dlogbr.dir=%LOGBR_TMP%

rem DBG="-Xdebug -Xrunjdwp:transport=dt_socket, address=8000, server=y, suspend=y"™
set LOG_OPTS=-Dlog4j.configurationFile=%ETCDIR%\logbr.log4j2.indexer.xml -DlogPath=%LOGDIR%
set MISC_OPTIONS=-Dsun.java2d.d3d=false -Dall=1 -Xms4G -Xmx10G

set SQLITE_PRAGMAS=-Dsqlite.pragma=true

set NO_TLIB_REQUESTS=-Dtlib.norequest=false

set TIMEDIFF=-Dtimediff.parse=false

set SIP_LINES=-DSIPLINES=1

rem ------------------ no config after this line -------------------------

set JAVA_OPTS=%DBG% %LOGBR_TMP_OPT% %LOG_OPTS% %MISC_OPTIONS% %SQLITE_PRAGMAS% %NO_TLIB_REQUESTS% %TIMEDIFF% %SIP_LINES%
if [%1]==[] (
set RUN_DIR=%CD%
) else (
set RUN_DIR=%1
)

cd %RUN_DIR%

set DB=%RUN_DIR%\%LOGBRDB%

set CFG=%ETCDIR%\backend.xml

%LIBDIR%\bin\indexer.bat --dbname=%DB% --basedir=%RUN_DIR% --cfgxml=%CFG% --logbr.dir=%LOGBR_TMP%

