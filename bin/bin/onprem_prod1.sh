#/bin/bash

set -x

BASEDIR=/home/stepan_sydoruk/IdeaProjects/install
#JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_201.jdk/Contents/Home
#PATH=$JAVA_HOME/bin:$PATH
#export JAVA_HOME PATH

LOGDIR=$BASEDIR/tmp
VARDIR=$BASEDIR/var
ETCDIR=$BASEDIR/etc
#DBG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
DBG=
LOG_OPTS="-Dlog4j2.configurationFile=${ETCDIR}/logdownloaderOnPrem.xml -DlogPath=${BASEDIR}/tmp"

#set DBG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
set DBG=

MISC_OPTIONS="-Dsun.java2d.d3d=false -Dall=1 -Xms4G -Xmx10G"
SQLITE_PRAGMAS=-Dsqlite.pragma=true
NO_TLIB_REQUESTS=-Dtlib.norequest=false
TIMEDIFF=-Dtimediff.parse=false
SIP_LINES=-DSIPLINES=1


export JAVA_OPTS="$LOG_OPTS $DBG $MISC_OPTIONS $SQLITE_PRAGMAS $NO_TLIB_REQUESTS $TIMEDIFF $SIP_LINES"


$BASEDIR/bin/getlogs --gui-profile=$VARDIR/onprem.txt \
	--hosts=$ETCDIR/onprem_prodHosts.txt --debug=TRACE --ssh-opt="-l stepan_sydoruk -o ConnectTimeout=300"  \
	--rsync-username=svc-gsys \
	--prod-base-dir=/applog/gcti \
	--log-file=$LOGDIR/logdown \
	--cfgxml=$ETCDIR/logbrowser/backend.xml \
	--sqlite.pragma 2>&1 >/dev/null &
