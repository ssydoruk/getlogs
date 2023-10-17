@ECHO On

rem set JAVA_HOME=C:\Java\jdk1.7.0_11_64
rem set JAVA_HOME=C:\Java\jdk1.6.0_20

if not defined JAVA_HOME (
	echo "Need java8 to run"
	pause
	goto DONE
)

rem basic directory for sipspan2

if not defined %1 (
  set SIPSPANDIR="C:\\Tools\\sipspan3\\GSipsTracer\\sipspan3"
) else (
  set SIPSPANDIR=%1
)

rem directory created under files directory where SIPSPAN3 would put all logs and produced files
set SIPSPAN_DIR=".sipspan3"
rem ----------------------- no config after this -------------------------------

rem swing application hungs without this option in multidisplay
set _JAVA_OPTIONS=-Dsun.java2d.d3d=false

set PATH=%JAVA_HOME%\bin;%PATH%

set SS3=%SIPSPANDIR%
set SS3LIB=%SIPSPANDIR%\\bin

rem directory for all SIPspan3 output from current directory

set SIPSPAN_DIR_OPT=-Dsipspan3.dir=%SIPSPAN_DIR%

set logoptionsind=-Dlog4j.configurationFile="file:///%SS3%/sipspan3.log4j2.indexer.xml"
set logoptionsinq=-Dlog4j.configurationFile="file:///%SS3%/sipspan3.log4j2.inquirer.xml"


set SIPSPANDB=sipspan03
set LOGDB=logdb03

set USE_DIST=true
set LOG4J=%SS3LIB%\log4j-core-2.7.jar;%SS3LIB%\log4j-api-2.7.jar
set COMM=%SS3LIB%\commons-lang3-3.5.jar
set JACOB=%SS3LIB%\jacob.jar
set JNA=%SS3LIB%\win32-x86-64.jar;%SS3LIB%\jna-platform.jar;%SS3LIB%\jna.jar
set ADDLIB=%SS3LIB%\GenericTree.jar;%SS3LIB%\LGoodDatePicker-10.2.3.jar

set SQLITE_PRAGMAS=-Dsqlite.pragma=false

set NO_TLIB_REQUESTS=-Dtlib.norequest=false

rem set JACOB_DLL_PATH=-Djacob.dll=%SS3LIB%\jacob-1.18-x64.dll


rem -------------------------------------------- parser config ----------------------------------

set backendpath="C:\Src\Java\sipspan3\sipspan2\indexer"

set backendjar=%SS3LIB%\indexer.jar
rem set backendclasspath=%backendpath%\dist\lib\sqlitejdbc-v056.jar;%backendpath%\dist\lib\JSON.jar;%LOG4J%;%COMM%;%JACOB%;%JNA%
set backendclasspath=%SS3LIB%\sqlite-jdbc-3.19.3.jar;%SS3LIB%\JSON.jar;%LOG4J%;%COMM%;%JACOB%;%JNA%

rem ******** change here to switch between production to development
rem set backendclass=%backendpath%\build\classes
set backendclass=%SS3LIB%\indexer.jar


rem set RUN_CMD_BE=-classpath %backendclasspath% -jar %backendjar%
set RUN_CMD_BE=-cp %backendclass%;%backendclasspath% %logoptionsind% %SQLITE_PRAGMAS% %SIPSPAN_DIR_OPT% sipspanindexer.Main /cfgxml %SS3%\backend.xml


rem -------------------------------------------- fe config ----------------------------------


SET outputspec=%SS3%\outputspec3.xml

set fepath="C:\Src\Java\sipspan3\sipspan2\inquirer"

set feclasspath=%SS3LIB%\jide-oss-3.6.14.jar;%SS3LIB%\commons-io-2.5.jar;%ADDLIB%;%backendclasspath%;%backendclass%


rem ******** change here to switch between production to development
rem set feclass=%fepath%\build\classes
set feclass=%SS3LIB%\inquirer.jar

rem set RUN_CMD_FE=-classpath %feclasspath% -jar %fejar%

rem used for debuging
rem set RUN_CMD_FE=-Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y -cp %feclass%;%feclasspath% inquirer.inquirer outputspec=%outputspec%

set RUN_CMD_FE=-cp %feclass%;%feclasspath% %logoptionsinq% %NO_TLIB_REQUESTS% %JACOB_DLL_PATH% %SIPSPAN_DIR_OPT% %_JAVA_OPTIONS% inquirer.inquirer outputspec=%outputspec%

:DONE