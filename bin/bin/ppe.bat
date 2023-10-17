

set BASEDIR=c:\GCTI
set LIBDIR=%BASEDIR%\lib

set JAVA_HOME=C:\Tools\graalvm-ce-java8-21.1.0
set PATH=%JAVA_HOME%\bin;%PATH%


set LOGDIR=%BASEDIR%\tmp
set VARDIR=%BASEDIR%\var
set ETCDIR=%BASEDIR%\etc

set LOG_OPTS=-Dlog4j2.configurationFile=%ETCDIR%\logdownloaderOnPrem.xml -Dlog4j.logPath=%LOGDIR%

#DBG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
DBG=

set MISC_OPTIONS=-Dsun.java2d.d3d=false -Dall=1 -Xms4G -Xmx10G
set SQLITE_PRAGMAS=-Dsqlite.pragma=true
set NO_TLIB_REQUESTS=-Dtlib.norequest=false
set TIMEDIFF=-Dtimediff.parse=false
set SIP_LINES=-DSIPLINES=1


set JAVA_OPTS=%LOG_OPTS% %MISC_OPTIONS% %SQLITE_PRAGMAS% %NO_TLIB_REQUESTS% %TIMEDIFF% %SIP_LINES%

%LIBDIR%\bin\getlogs %DBG% --gui-profile=%VARDIR%\ppe.txt --hosts=%ETCDIR%\ppeHosts.txt --debug=TRACE --ssh-opt="-o ConnectTimeout=300" --ssh-java --prod-base-dir=/opt/genesys/logs/ --log-file=%LOGDIR%\logdown --cfgxml=C:\GCTI\etc\logbrowser\backend.xml
