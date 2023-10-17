#!/bin/bash

#set -x

BASEDIR=/Users/stepan_sydoruk/GCTI
LIBDIR=$BASEDIR/lib

LOGDIR=$(pwd)/tmp
VARDIR=$BASEDIR/var
ETCDIR=$BASEDIR/etc/logbrowser

#JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_201.jdk/Contents/Home
#PATH=$JAVA_HOME/bin:$PATH

LOGBRDB=logbr #default name of the database
LOGBR_TMP=".tmp"
LOGBR_TMP_OPT=-Dlogbr.dir=${LOGBR_TMP}

#DBG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"

LOG_OPTS="-Dlog4j.configurationFile=$ETCDIR/logbr.log4j2.indexer.xml -Dlog4j.logPath=$LOGDIR"
MISC_OPTIONS="-Dsun.java2d.d3d=false -Dall=1 -Xms8000m -Xmx8000m"
SQLITE_PRAGMAS="-Dsqlite.pragma=true"

NO_TLIB_REQUESTS="-Dtlib.norequest=false"
TIMEDIFF="-Dtimediff.parse=false"

#------------------ no config after this line -------------------------
export JAVA_HOME PATH

JAVA_OPTS="$DBG $LOGBR_TMP_OPT $LOG_OPTS $MISC_OPTIONS $SQLITE_PRAGMAS $NO_TLIB_REQUESTS $TIMEDIFF"
export JAVA_OPTS

SS3=/Users/stepan_sydoruk/GCTI/bin/logbrowser

if [ -z $1 ]; then
	RUN_DIR=$(pwd)
else
	RUN_DIR=$1
fi

cd ${RUN_DIR}

if [ ${machine=x} = "Win" ]; then
	DB=$(cygpath -pw ${RUN_DIR}/${LOGBRDB})
	CFG=$(cygpath -pw ${SS3}/backend.xml)
else
	DB=${RUN_DIR}/${LOGBRDB}
	CFG=${ETCDIR}/backend.xml
fi

clean_up() {

	# Perform program exit housekeeping
	kill -9 $PID
	exit

}

trap clean_up SIGHUP SIGINT SIGTERM

cd ${RUN_DIR}
(${BASEDIR}/lib/logbrowser/bin/indexer --dbname=${DB} --basedir=${RUN_DIR} --cfgxml ${CFG}) &
PID=$!

wait $PID
