#!/bin/bash

#set -x

BASEDIR=/Users/stepan_sydoruk/GCTI
#JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_201.jdk/Contents/Home
#PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

LOGDIR=$(pwd)/tmp
VARDIR=$BASEDIR/var
ETCDIR=$BASEDIR/etc/logbrowser
LOGBRDB=logbr #default name of the database
LOGBR_DIR=".tmp"
LOGBR_DIR_OPT=-Dlogbr.dir=${LOGBR_DIR}

#DBG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
LOG_OPTS="-Dlog4j.configurationFile=$ETCDIR/logbr.log4j2.inquirer.xml -Dlog4j.logPath=$LOGDIR"
MISC_OPTIONS="-Dsun.java2d.d3d=false -Dall=1 -Xms8000m -Xmx8000m"
SQLITE_PRAGMAS="-Dsqlite.pragma=true"

NO_TLIB_REQUESTS="-Dtlib.norequest=false"
TIMEDIFF="-Dtimediff.parse=false"

JAVA_OPTS="$DBG $LOGBR_DIR_OPT $LOG_OPTS $MISC_OPTIONS $SQLITE_PRAGMAS $NO_TLIB_REQUESTS $TIMEDIFF"
export JAVA_OPTS

#JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_201.jdk/Contents/Home

#comment out next line to turn on debug on FrontEnd
#------------------ no config after this line -------------------------

if [ -z $1 ]; then
	RUN_DIR=$(pwd)
else
	RUN_DIR=$1
fi

cd ${RUN_DIR}

if [ ${machine=x} = "Win" ]; then
	DB=$(cygpath -pw ${RUN_DIR}/${LOGBRDB})
	RUN_DIR=$(cygpath -pw ${RUN_DIR})
	CFG=$(cygpath -pw ${SS3}/inquirer.cfg)
else
	DB=${RUN_DIR}/${LOGBRDB}
	CFG=${ETCDIR}/inquirer.cfg.json
	OUTSPEC=${ETCDIR}/outputspec3.xml
fi

# uncomment below if database is too large
#REFERENCE=--no-ref-sync

nohup $BASEDIR/lib/logbrowser/bin/inquirer $REFERENCE \
--dbname=${DB} --config=${CFG} --basedir=${RUN_DIR} --outputspec=${OUTSPEC} 2>&1 >/dev/null &
