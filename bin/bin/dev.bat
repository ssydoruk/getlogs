

set BASEDIR=c:\GCTI
set LIBDIR=%BASEDIR%\lib
set JAVA_HOME=C:\Tools\graalvm-ce-java8-21.1.0
set PATH=%JAVA_HOME%\bin;%PATH%


set LOGDIR=%BASEDIR%\tmp
set VARDIR=%BASEDIR%\var
set ETCDIR=%BASEDIR%\etc
#DBG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
set DBG=
set JAVA_OPTS=-Dlog4j2.configurationFile=%ETCDIR%\logdownloaderOnPrem.xml -Dlog4j.logPath=%LOGDIR%

%LIBDIR%\bin\getlogs %DBG% --gui-profile=%VARDIR%\dev.txt --hosts=%ETCDIR%\devHosts.txt --debug=TRACE --ssh-opt="-o ConnectTimeout=300" --ssh-java --prod-base-dir=/opt/genesys/logs/ --log-file=%LOGDIR%\logdown
