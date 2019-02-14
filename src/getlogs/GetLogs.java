/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.ScreenInfo;
import Utils.UnixProcess.ExtProcess;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.jidesoft.dialog.StandardDialog;
import static com.jidesoft.dialog.StandardDialog.RESULT_CANCELLED;
import getlogs.LogFiles.LogFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.io.IoBuilder;

/**
 *
 * @author stepansydoruk
 */
public class GetLogs {

    private static String lfmtHost = null;
    private static String hostAppFile = null;
    private static String execCmd = null;
    private static String appsOpt = null;
    public static String sshOptions = null;
    private static boolean listFiles = false;

    public static final Pattern regDateTimeSpec = Pattern.compile("^[0-9\\[\\]\\-]+$");
    private static final Pattern regDirectoryStripSlash = Pattern.compile("^(.+)/+$");
    private static String dateSpec;
    private static String timeSpec;
    private static String lfmtInstance;
    public static boolean useRSync = false;

    /**
     * @param args the command line arguments
     */
    private static String sLogDirectory;
    public static String appHost;
    private static String sLoaderLog;
    private static Option optHosts;
    private static Option optCmd;
    private static Option optApp;
    private static Option optIsLFMT;
    private static Option optLogLevel;
    private static Option optLFMTInstance;
    private static Option optSSHOptions;
    private static Option optLogDirectory;
    private static Option optLoaderLog;
    private static Option optGrep;
    private static Option optForceHost;
    private static Option optHelp;
    private static Option optDay;
    private static Option optArchives;
    private static Option optGUIProfile;
    private static Option optIsCloud;
    private static Option optListFiles;
    private static Option optUseRSync;
    private static Option optTime;
    private static Option optProdBaseDir;
    private static String prodBaseDir;
    private static Option optUserName;
    private static String sUserName;

    public static Hosts getHosts() {
        return hosts;
    }
    private static Hosts hosts;
    public static GetCommand execCommand;
    private static String sGrep = null;
    private static String sArchives;
    private static LogFiles logFiles = null;
    private static boolean bIsCloud = false;
    private static String sGUIProfile;

    public static String getsGUIProfile() {
        return sGUIProfile;
    }
    private static Options options;

    public static void main(String[] args) throws Exception {
// set the name of the application menu item
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "WikiTeX");

        options = new Options();

        optHosts = new Option("l", "hosts", true, "file containing binding host to genesysApplication. "
                + "\n\tSingle line contains entry <hostname>,<Genesys_application_name>");
        optHosts.setRequired(true);
        options.addOption(optHosts);

        optCmd = new Option("c", "command", true, "command to run.\nAccepted values are " + GetCommand.showAll());
        optCmd.setRequired(false);
        optCmd.setType(String.class);
        options.addOption(optCmd);

        optApp = new Option("a", "app", true, "Genesys application name. Multiple can be specified"
                + " separated by coma (no spaces)");
        optApp.setRequired(false);
        options.addOption(optApp);

        optIsLFMT = Option.builder("f")
                .hasArg(true)
                .required(false)
                .desc("go to LFMT host for the logs\nBy default go to the server")
                .longOpt("lfmt-host")
                .build();
        options.addOption(optIsLFMT);

        optGUIProfile = Option.builder("f")
                .hasArg(true)
                .required(false)
                .desc("Path to GUI configured storage (JSON)."
                        + "If specified, GUI configurator will be called")
                .longOpt("gui-profile")
                .build();
        options.addOption(optGUIProfile);

        optIsCloud = Option.builder()
                .hasArg(false)
                .required(false)
                .desc("if mentioned, format of file name is cloud like instead of standard Genesys log file naming")
                .longOpt("is-cloud")
                .build();
        options.addOption(optIsCloud);

        optLFMTInstance = Option.builder()
                .hasArg(true)
                .required(false)
                .desc("name of the LFMT instance (e.g. us_lfmt)")
                .longOpt("lfmt-instance")
                .build();
        options.addOption(optLFMTInstance);

        optLogLevel = Option.builder("d")
                .hasArg(true)
                .required(false)
                .desc("Debug level (OFF,FATAL,ERROR,WARN,INFO,DEBUG,TRACE,ALL)")
                .longOpt("debug")
                .build();
        options.addOption(optLogLevel);

        optProdBaseDir = Option.builder()
                .hasArg(true)
                .required(false)
                .desc("basic directory for prod logs (e.g. /AppLogs/GCTI)Ï")
                .longOpt("prod-base-dir")
                .build();
        options.addOption(optProdBaseDir);

        optSSHOptions = Option.builder()
                .hasArg(true)
                .required(false)
                .desc("options to ssh")
                .longOpt("ssh-opt")
                .build();
        options.addOption(optSSHOptions);

        optUserName = Option.builder("u")
                .hasArg(true)
                .required(false)
                .desc("remote user name")
                .longOpt("username")
                .build();
        options.addOption(optUserName);

        optLogDirectory = Option.builder("b")
                .hasArg(true)
                .required(false)
                .desc("base directory to which logs should be put")
                .longOpt("log-directory")
                .build();
        options.addOption(optLogDirectory);

        optLoaderLog = Option.builder()
                .hasArg(true)
                .required(false)
                .desc("Path to utility log directory and/or file")
                .longOpt("log-file")
                .build();
        options.addOption(optLoaderLog);

        optListFiles = Option.builder()
                .hasArg(false)
                .required(false)
                .desc("show files")
                .longOpt("list-files")
                .build();
        options.addOption(optListFiles);

        optUseRSync = Option.builder()
                .hasArg(false)
                .required(false)
                .desc("use rsync for file transfer")
                .longOpt("use-rsync")
                .build();
        options.addOption(optUseRSync);

        optForceHost = Option.builder()
                .hasArg(true)
                .required(false)
                .desc("force host <arg> instead of looking up the file")
                .longOpt("force-host")
                .build();
        options.addOption(optForceHost);

        optGrep = Option.builder("g")
                .hasArg(true)
                .required(false)
                .desc("grep expression to search for <arg>")
                .longOpt("grep")
                .build();
        options.addOption(optGrep);

        optHelp = Option.builder("h")
                .hasArg(false)
                .required(false)
                .desc("Show help and exit")
                .longOpt("help")
                .build();
        options.addOption(optHelp);

        optDay = Option.builder("y")
                .hasArg(true)
                .required(false)
                .desc("Specify date for logs")
                .longOpt("date")
                .build();
        options.addOption(optDay);

        optArchives = Option.builder()
                .hasArg(true)
                .required(false)
                .desc("file containing list of log files on LFMT hosts (one file per line)\n"
                        + "If specified, downloader is forced to get those files")
                .longOpt("log-files")
                .build();
        options.addOption(optArchives);

        optTime = Option.builder("t")
                .hasArg(true)
                .required(false)
                .desc("Specify time")
                .longOpt("time")
                .build();
        options.addOption(optTime);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LogManager.getLogger().error(e);
            showHelpExit(e.getMessage(), options);
        }

//<editor-fold defaultstate="collapsed" desc="common options">
        if (cmd.hasOption(optHelp.getLongOpt())) {
            showHelpExit(options);
        }

        sLoaderLog = (String) cmd.getParsedOptionValue(optLoaderLog.getLongOpt());
        initLogger((String) cmd.getParsedOptionValue(optLogLevel.getOpt()), sLoaderLog);
        logger.info("logger inited" + " level: " + logger.getLevel());
        if (logger.isDebugEnabled()) {
            StringBuilder s = new StringBuilder();
            for (String arg : args) {
                if (s.length() > 0) {
                    s.append(" ");
                }
                s.append(arg);
            }
            logger.debug("Command line: " + s);
        }

        hostAppFile = (String) cmd.getParsedOptionValue(optHosts.getOpt());
        if (hostAppFile == null || hostAppFile.trim().isEmpty()) {
            showHelpExit("Option " + optHosts.getOpt() + " (" + optHosts.getLongOpt() + ") is mandatory", options);
        }

        hosts = new Hosts(hostAppFile);

        sLogDirectory = (String) cmd.getParsedOptionValue(optLogDirectory.getLongOpt());
        if (sLogDirectory == null || sLogDirectory.isEmpty()) {
            sLogDirectory = ".";
        } else {
            Matcher m;
            if ((m = regDirectoryStripSlash.matcher(sLogDirectory)).find()) {
                sLogDirectory = m.group(1);
            }
        }
        sshOptions = (String) cmd.getParsedOptionValue(optSSHOptions.getLongOpt());
        sUserName = (String) cmd.getParsedOptionValue(optUserName.getLongOpt());

        prodBaseDir = (String) cmd.getParsedOptionValue(optProdBaseDir.getLongOpt());

//</editor-fold>
        //--------------------------------------
        if (logger.isDebugEnabled()) {
            logger.debug("Current directory:" + getWD());
//            Runtime.getRuntime().exec("ls -l");
        }

        sGUIProfile = (String) cmd.getParsedOptionValue(optGUIProfile.getLongOpt());

        if (sGUIProfile != null && !sGUIProfile.isEmpty()) {
            processGUI(options);
        } else {
            processCMDLine(options, cmd);
        }

        logger.info("allDone");

    }

    public static String getSshOptions() {
        if (sshOptions != null && !sshOptions.isEmpty()) {
            return sshOptions;
        } else {
            return "";
        }
    }

    public static String getsUserName() {
        return sUserName;
    }

    public static void processApp(String ap, Options options) throws IOException, InterruptedException {
        String theAppHost;
        if (appHost == null || appHost.isEmpty()) {
            theAppHost = (String) hosts.get(ap); // first for one application only
            if (theAppHost == null) {
                System.out.println("Host for app [" + ap + "] not found; exiting");
                return;
            }
        } else {
            theAppHost = appHost;
        }

        StringBuilder logsDir = new StringBuilder();

        if (lfmtHost != null) {
            logsDir.append("/Logs/")
                    .append(lfmtInstance).append("/")
                    .append(lfmtInstance).append("_cls/")
                    .append(theAppHost) //                    .append("/")
                    //                    .append(ap)
                    ;
        } else {
            logsDir.append("/AppLog/GCTI");

        }

        logger.debug("logsDir clause: [" + logsDir + "]");

        if (dateSpec != null && !dateSpec.isEmpty() && !regDateTimeSpec.matcher(dateSpec).matches()) {
            showHelpExit("Date is specified but the format is incorrect", options);
        }
        if (timeSpec != null && !timeSpec.isEmpty() && !regDateTimeSpec.matcher(timeSpec).matches()) {
            showHelpExit("Time is specified but the format is incorrect", options);
        }

        StringBuilder fileNameClause = getFileNameClause(options);

        switch (execCommand) {
            case GREP:
                executeGrep(ap, theAppHost, logsDir, fileNameClause);
                break;

            case GET:
                executeGet(ap, theAppHost, logsDir, fileNameClause, useRSync);
                break;

            case LS:
                executeLS(ap, theAppHost, logsDir, fileNameClause);
                break;

            case GREPGET:
                executeGrepGet(ap, theAppHost, logsDir, fileNameClause);
                break;

        }

    }

    static ArrayList<String> apps = null;
    static Logger logger;

    public static void initLogger(String par, String sLoaderLog1) {
        Level level = Level.INFO;
        if (par != null) {
            level = Level.toLevel(par, Level.INFO);
        }
        if (sLoaderLog1 == null || sLoaderLog1.isEmpty()) {
            sLoaderLog1 = "./applog";
        } else {
            System.setProperty("logPath", sLoaderLog1);

        }
        System.setProperty("log4j2.saveDirectory", "true");
        String s = System.getProperty("log4j.configurationFile");
        if (s != null && !s.isEmpty()) {
            s = System.getProperty("program.name") + ".xml";
            logger = LogManager.getLogger("logdownloader");
        } else {

            ConfigurationBuilder<BuiltConfiguration> builder
                    = ConfigurationBuilderFactory.newConfigurationBuilder();

            builder.addProperty("LogFileName", sLoaderLog1);

            AppenderComponentBuilder console
                    = builder.newAppender("stdout", "Console");

            ComponentBuilder triggeringPolicies = builder.newComponent("Policies")
                    .addComponent(builder.newComponent("OnStartupTriggeringPolicy"))
                    .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                            .addAttribute("size", "20M"));

            AppenderComponentBuilder rollingFile
                    = builder.newAppender("rolling", "RollingFile");
            rollingFile.addAttribute("fileName", "${LogFileName}.log");
            rollingFile.addAttribute("filePattern", "${LogFileName}-%d{yyyyMMdd-HHmmss_SSS}.log");
            rollingFile.addComponent(triggeringPolicies);

//        FilterComponentBuilder flow = builder.newFilter(
//                "MarkerFilter",
//                Filter.Result.ACCEPT,
//                Filter.Result.DENY);
//        flow.addAttribute("marker", "FLOW");
//        console.add(flow);
            LayoutComponentBuilder standard
                    = builder.newLayout("PatternLayout");
            standard.addAttribute("pattern", "%d %5.5p %30.30C [%t] %m%n");

            console.add(standard);
            rollingFile.add(standard);

            builder.add(console);
            builder.add(rollingFile);
//        Appender appe = MyCustomAppenderImpl.createAppender("appe1", null, null, null);
//        AppenderComponentBuilder newAppender = builder.newAppender("appe", "appe1");
//        builder.add(appe);

            RootLoggerComponentBuilder rootLogger
                    = builder.newRootLogger(level);
            rootLogger.add(builder.newAppenderRef("stdout"));
            rootLogger.add(builder.newAppenderRef("rolling"));
            builder.add(rootLogger);

            Configurator.initialize(builder.build());
//        System.out.println(builder.toXmlConfiguration());
            logger = LogManager.getLogger();
            logger.info("log initialized");
        }
//        LogWindow.initCustomLogger();

    }

    private static void showHelpExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("utility-name", options);

        System.exit(0);
    }

    private static void showHelpExit(String msg, Options options) {
        if (msg != null && !msg.isEmpty()) {
            LogManager.getLogger().error(msg);
        }
        showHelpExit(options);
    }

    /**
     * Check value of the command parameter
     *
     * @param execCmd
     * @param options
     * @return 'true' if it is 'ls'
     */
    private static GetCommand checkCmdCommand(String execCmd, Options options) {
        GetCommand ret = GetCommand.Unknown;
        if (execCmd != null && !execCmd.isEmpty()) {

            ret = GetCommand.get(execCmd);
        }
        if (ret == GetCommand.Unknown) {
            logger.error("Incorrect value for command - [" + execCmd + "]");
            showHelpExit(options);
        }
        return ret; // never reached
    }

    public static String expandPattern(String dateSpec, int max) {
        int i = countDigits(dateSpec);
        return dateSpec + StringUtils.repeat("[0-9]", max - i);

    }

    private static final Pattern regCountDigitsCovered = Pattern.compile("(\\d|\\[[\\d\\-]+\\])");

    private static int countDigits(String dateSpec1) {

        int pos = 0;
        int datePos = 0;
        Matcher m;
        while ((m = regRegDigits.matcher(dateSpec1)).find(pos)) {
            datePos++;
            pos = m.end();
        }

        return datePos;
    }

    static void doLog(String s) {
        logger.info(s);
    }

    static String getWD() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }

    private static void executeGet(String ap, String theAppHost, StringBuilder logsDir, StringBuilder fileNameClause, boolean useRSync1) throws IOException, InterruptedException {
        if (useRSync1) {
            executeRSync(ap, theAppHost, logsDir, rSyncAddClause(fileNameClause.toString()));
        } else {
            ArrayList<String> sshParams = new ArrayList<>();
            sshParams.add("ssh");
            if (sshOptions != null) {
                sshParams.addAll(Arrays.asList(new String[]{"-l", sshOptions}));
            }

            if (lfmtHost != null) {
                sshParams.add(lfmtHost);
            } else {
                sshParams.add(theAppHost);

            }

            StringBuilder fileClause = new StringBuilder();
            if (fileNameClause.length() > 0) {
                fileClause.append("\\( -type f ");

                fileClause.append("-a -name ")
                        .append(fileNameClause);

                fileClause.append(" \\) ");
            }
            StringBuilder sshCmd = new StringBuilder();

            sshCmd.append("cd ").append(logsDir).append("; ");
            sshCmd.append("find ")
                    .append(ap)
                    .append(" ")
                    .append(fileClause);
            sshCmd.append(" -exec ");
            sshCmd.append("tar -");
            if (lfmtHost == null) {
                sshCmd.append("z");
            }
            sshCmd.append("cvf - ")
                    .append("{} +");
            sshParams.add(sshCmd.toString());
            ExtProcess procSSH = new ExtProcess(sshParams);

            ExtProcess procTar = null;
            ArrayList<String> tarParams = new ArrayList<>();
            tarParams.add("tar");
            tarParams.add("-x");
            tarParams.add("-f");
            tarParams.add("-");

            procTar = new ExtProcess(tarParams, procSSH);
            procTar.startProcess();

            procSSH.startProcess();
            procSSH.waitFor();
            procTar.waitFor();
        }

    }

    private static void executeLS(String ap, String theAppHost, StringBuilder logsDir, StringBuilder fileNameClause) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (sshOptions != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", sshOptions}));
        }

        sshParams.add(((lfmtHost != null)) ? lfmtHost : theAppHost);

        StringBuilder fileClause = new StringBuilder();
        fileClause.append("\\( -type f ");

        if (fileNameClause.length() > 0) {
            fileClause.append("-a -name ")
                    .append(fileNameClause);
        }

        fileClause.append(" \\) ");
        StringBuilder sshCmd = new StringBuilder();

        sshCmd.append("cd ").append(logsDir).append("; ");
        sshCmd.append("find ")
                .append(ap)
                .append(" ")
                .append(fileClause);
        if (!listFiles) {
            sshCmd.append(" -o -type d ");
        }
        sshCmd.append("-print | sort");
        sshParams.add(sshCmd.toString());
        ExtProcess procSSH = new ExtProcess(sshParams);

        procSSH.startProcess();

        procSSH.waitFor();

    }

    private static ArrayList<String> executeGrep(String ap, String appHost1, StringBuilder logsDir, StringBuilder fileNameClause) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (sshOptions != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", sshOptions}));
        }
        sshParams.add(((lfmtHost != null)) ? lfmtHost : appHost1);

        StringBuilder fileClause = new StringBuilder();
//        fileClause.append("\\("); 

        if (fileNameClause.length() > 0) {
            fileClause.append(" -name ")
                    .append(fileNameClause);
        }

//        fileClause.append(" \\) ");
        StringBuilder sshCmd = new StringBuilder();

        sshCmd.append("cd ").append(logsDir).append("; ");
        sshCmd.append("find ")
                .append(ap)
                .append(" ")
                .append(fileClause);
        sshCmd.append(" ");
//        sshCmd.append("\\( ")
//                .append(" -iname *.log -execdir grep Trc {} \\; -true ");
//        sshCmd.append("\\)");
//        sshCmd.append(" -o ");
        ArrayList<String> matchedFiles = new ArrayList<>();
        for (Map.Entry<String, String> extUnp : extUnpacker.entrySet()) {
            for (String matchedFile : execGrep(extUnp.getKey(), extUnp.getValue(), sshParams, sshCmd, sGrep)) {
                if (matchedFile.startsWith(filePrefix)) {
                    matchedFiles.add(matchedFile.substring(filePrefix.length()));
                } else {
                    logger.error("Not file name: [" + matchedFile + "]Ï");
                }
            }

        }
        return matchedFiles;
    }

    public static final HashMap<String, String> extUnpacker = getextUnpacker();

    private static HashMap<String, String> getextUnpacker() {
        HashMap<String, String> ret = new HashMap<>();
        ret.put("*.zip", "unzip -p ${f}");
        ret.put("*.log", "cat ${f}");
        return ret;
    }

    final static public String filePrefix = "!file!";

    public static ArrayList<String> execGrep(String ext, String unp, ArrayList<String> sshParams, StringBuilder sshCmd,
            String regex) throws IOException, InterruptedException {
        StringBuilder grepCmd = new StringBuilder();
        grepCmd.append(sshCmd)
                .append(" -iname ").append(ext)
                .append(" | xargs bash -c '")
                //                .append("echo bash is here; echo params: $*; ")
                .append("for f in $*; do if ")
                .append(unp)
                .append("| egrep -q \"").append(regex).append("\"; then echo ").append(filePrefix).append("${f}; fi; done'" + "");
        ArrayList<String> paramsRun = new ArrayList<>(sshParams);
        paramsRun.add(grepCmd.toString());
        ExtProcess procSSH = new ExtProcess(paramsRun);

        procSSH.startProcess(true, true);
        return procSSH.getSTDOut();

    }

    private static void executeGrepGet(String ap, String theAppHost, StringBuilder logsDir, StringBuilder fileNameClause) throws IOException, InterruptedException {
        ArrayList<String> executeGrep = executeGrep(ap, theAppHost, logsDir, fileNameClause);
        ArrayList<String> rSyncFiles = new ArrayList<>();
        for (String fileName : executeGrep) {
            rSyncFiles.addAll(rSyncAddClause(Utils.Util.stripDir(fileName)));
        }

        executeRSync(ap, theAppHost, logsDir, rSyncFiles);
    }

    public static ArrayList<String> rSyncAddClause(String fileName) {
        ArrayList<String> ret = new ArrayList<String>();
        ret.add("-f");//--filter
        ret.add("+ /*" + fileName);
        return ret;
    }

    private static void executeRSync(String ap, String theAppHost, StringBuilder logsDir, ArrayList<String> fileNameClause) throws IOException, InterruptedException {
        ArrayList<String> rsyncParams = new ArrayList<>();
        rsyncParams.add("rsync");
        rsyncParams.add("-avz");
        rsyncParams.add("-e");
        rsyncParams.add("ssh");
        rsyncParams.addAll(fileNameClause);
        rsyncParams.add("-f");
        rsyncParams.add("- **");
        StringBuilder srcSpec = new StringBuilder();
        srcSpec.append(sshOptions).append("@").append((lfmtHost != null ? lfmtHost : theAppHost)).append(":")
                .append(logsDir).append("/").append(ap).append("/").append("");

        rsyncParams.add(srcSpec.toString());

        StringBuilder dstSpec = new StringBuilder();
        dstSpec.append(sLogDirectory).append("/").append(ap);

        rsyncParams.add(dstSpec.toString());

        ExtProcess procRSync = new ExtProcess(rsyncParams);
        procRSync.startProcess();
        procRSync.waitFor();
    }

    private static void processLogFiles(String app, ArrayList<LogFile> listLogFiles) throws IOException, InterruptedException {
        if (!listLogFiles.isEmpty()) {
            ArrayList<String> rsyncParams = new ArrayList<>();
            rsyncParams.add("/usr/local/bin/rsync");
//            rsyncParams.add("--dry-run");
            rsyncParams.add("-avz");
            rsyncParams.add("-e");
            rsyncParams.add("ssh");
//        rsyncParams.add("--files-from");
//        rsyncParams.add(sArchives);
//        rsyncParams.add("-f");
//        rsyncParams.add("- **");
            StringBuilder buf = new StringBuilder();
            buf.append(sshOptions).append("@").append((lfmtHost != null ? lfmtHost : "")).append(":")
                    .append(listLogFiles.get(0).getLfmtName()) //                .append(" :").append(logFiles.get(1));
                    //                .append("/*")
                    ;
            rsyncParams.add(buf.toString());

            for (int i = 1; i < listLogFiles.size(); i++) {
                buf = new StringBuilder();
                buf.append(":").append(listLogFiles.get(i).getLfmtName());
                rsyncParams.add(buf.toString());
            }

            buf = new StringBuilder();
            buf.append(sLogDirectory).append(File.separator).append(app);

            rsyncParams.add(buf.toString());

            ExtProcess procRSync = new ExtProcess(rsyncParams);
            procRSync.startProcess();
            procRSync.waitFor();
        }
    }

    private static StringBuilder getFileNameClause(Options options) {
        StringBuilder fileNameClause = new StringBuilder();

        if (bIsCloud) {
            String backSlash = "";
            if (!useRSync) {
                backSlash = "\\";
                fileNameClause.append("").append(backSlash).append("*").append(backSlash).append(".");
            } else {
                fileNameClause.append("*cloud*").append("-");
            }
            if (!regDateTimeSpec.matcher(dateSpec).matches()) {
                showHelpExit("Date is specified but the format is incorrect", options);
            } else {
                fileNameClause.append(cloudDatePattern(dateSpec, timeSpec, SettingsPanel.TimeProfile.REGEX));
            }

        } else {
            String backSlash = "";
            if (!useRSync) {
                backSlash = "\\";
            }
            fileNameClause.append("").append(backSlash).append("*").append(backSlash).append(".");
            if (dateSpec != null && !dateSpec.isEmpty()) {
                if (!regDateTimeSpec.matcher(dateSpec).matches()) {
                    showHelpExit("Date is specified but the format is incorrect", options);
                } else {
                    fileNameClause.append(expandPattern(dateSpec, 8));
                }
            } else {
                fileNameClause.append(StringUtils.repeat("[0-9]", 8));
            }
            fileNameClause.append("_");

            if (timeSpec != null && !timeSpec.isEmpty()) {

                if (!regDateTimeSpec.matcher(timeSpec).matches()) {
                    showHelpExit("Time is specified but the format is incorrect", options);
                } else {
                    fileNameClause.append(expandPattern(timeSpec, 6));
                }
            } else {
                fileNameClause.append(StringUtils.repeat("[0-9]", 6));
            }
            fileNameClause.append("_");

            fileNameClause.append(StringUtils.repeat("[0-9]", 3))
                    .append("").append(backSlash).append(".").append(backSlash).append("*");

        }
        logger.debug("fileName clause: [" + fileNameClause + "]");
        return fileNameClause;
    }

    public static final Pattern regRegDigits = Pattern.compile("(\\d|\\[[\\-\\d]*\\])");

    public static String cloudDatePattern(String dateSpec1, String timeSpec1, SettingsPanel.TimeProfile tp) {

        StringBuilder ret = new StringBuilder();
        Matcher m;
//        int digitsSpecified = countDigits(dateSpec);
        int pos = 0;
        int datePos = 0;
        if (tp == SettingsPanel.TimeProfile.REGEX && dateSpec1 != null && !dateSpec1.isEmpty()) {
            while ((m = regRegDigits.matcher(dateSpec1)).find(pos)) {
                if (shouldAddDash(datePos)) {
                    ret.append("-");
                }
                ret.append(m.group(1));
                datePos++;
                pos = m.end();
            }
        }
        while (datePos < 8) {
            if (shouldAddDash(datePos)) {
                ret.append("-");
            }
            ret.append("[0-9]");
            datePos++;
        }
        if (tp == SettingsPanel.TimeProfile.REGEX && timeSpec1 != null && !timeSpec1.isEmpty()) {
            ret.append("-");
            pos = 0;
            datePos = 0;
            while ((m = regRegDigits.matcher(timeSpec1)).find(pos)) {
                ret.append(m.group(1));
                pos = m.end();
                datePos++;
                if (pos > 1) {
                    break;
                }
            }
            if (datePos == 1) {
                ret.append("[0-9]");
            }
        } else {
            ret.append("-");
        }
        ret.append("*.log*");
        logger.debug(ret);
//        System.exit(0);
        return ret.toString();

    }

    private static boolean shouldAddDash(int datePos) {
        return (datePos == 4 || datePos == 6);
    }

    private static void processGUI(Options options) throws IOException, InterruptedException, InvocationTargetException {
        File f = new File(sGUIProfile);
        DownloadSettings ds = null;
        if (f.exists()) {
//                Gson gson = new Gson();

            Gson gson = new GsonBuilder()
                    .enableComplexMapKeySerialization()
                    .serializeNulls()
                    .setDateFormat(DateFormat.LONG)
                    .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                    .setPrettyPrinting()
                    .create();

            try {
                InputStreamReader reader = new InputStreamReader(new FileInputStream(f));
                ds = gson.fromJson(reader, DownloadSettings.class);
                reader.close();
                ds.checkLFMT();
            } catch (JsonSyntaxException | JsonIOException | IOException ex) {
                LogManager.getLogger().error(ex);
            }
        } else {
            ds = new DownloadSettings();
            HashSet<DownloadSettings.App> apps = ds.addProfile("SIP").getApps();
            apps.add(new DownloadSettings.App("sip1"));
            apps.add(new DownloadSettings.App("sip2"));

            apps = ds.addProfile("Routing").getApps();
            apps.add(new DownloadSettings.App("URS1"));
            apps.add(new DownloadSettings.App("ORS2"));
        }
        showGui(ds);
    }

    static void showGui(DownloadSettings ds) throws InterruptedException, InvocationTargetException {

        java.awt.EventQueue.invokeAndWait(new Runnable() {
            public void run() {

                SettingsDialog dlg = new SettingsDialog(ds);
                dlg.getCe().setSettingsFile(sGUIProfile);
                dlg.pack();
                dlg.invalidate();
                ScreenInfo.CenterWindow(dlg);
                dlg.setVisible(true);
            }
        });
    }

    private static void processCMDLine(Options options, CommandLine cmd) throws IOException, InterruptedException, ParseException {
        sArchives = (String) cmd.getParsedOptionValue(optArchives.getLongOpt());
        if (sArchives != null && !sArchives.isEmpty()) {
            logFiles = new LogFiles(sArchives);
        }
        apps = new ArrayList<>();
        String[] split = appsOpt.split(",");
        for (String string : split) {
            apps.add(string);
        }

        bIsCloud = cmd.hasOption(optIsCloud.getLongOpt());

        appHost = (String) cmd.getParsedOptionValue(optForceHost.getLongOpt());
        dateSpec = (String) cmd.getParsedOptionValue(optDay.getOpt());
        timeSpec = (String) cmd.getParsedOptionValue(optTime.getOpt());

        execCmd = (String) cmd.getParsedOptionValue(optCmd.getOpt());

        execCommand = checkCmdCommand(execCmd, options);

        appsOpt = (String) cmd.getParsedOptionValue(optApp.getOpt());

        listFiles = cmd.hasOption(optListFiles.getLongOpt());
        useRSync = cmd.hasOption(optUseRSync.getLongOpt());

        lfmtInstance = (String) cmd.getParsedOptionValue(optLFMTInstance.getLongOpt());
        lfmtHost = (String) cmd.getParsedOptionValue(optIsLFMT.getOpt());

        if ((lfmtInstance != null && !lfmtInstance.isEmpty()
                && (lfmtHost == null || lfmtHost.isEmpty()))
                || ((lfmtHost != null && !lfmtHost.isEmpty())
                && (lfmtInstance == null || lfmtInstance.isEmpty()))) {
            logger.error("If LFMT to be used, then both options " + optLFMTInstance.getLongOpt() + " and " + optIsLFMT.getLongOpt()
                    + " are to be specified");
            System.exit(1);
        }

        sGrep = (String) cmd.getParsedOptionValue(optGrep.getLongOpt());

        //---------------------parameters processing done-----------------
        if (logFiles != null) {
            for (Map.Entry<String, ArrayList<LogFiles.LogFile>> object : logFiles.entrySet()) {
                processLogFiles(object.getKey(), object.getValue());
            }
        } else {
            DownloadSettings ds = new DownloadSettings();
            DownloadSettings.AppProfile addProfile = ds.addProfile("default");
            DownloadSettings.LFMTHostInstance theLFMT = new DownloadSettings.LFMTHostInstance(lfmtHost, lfmtInstance, "/Logs");
            ds.setOutputDir(sLogDirectory);
            ds.setProd((lfmtHost == null));
            ds.setLfmt((lfmtHost != null));
            ds.setCMDDate(dateSpec);
            ds.setCMDTime(timeSpec);

            addProfile.setIsGenesysName(!bIsCloud);
            addProfile.setLFMT(theLFMT);
            for (String app : apps) {
                DownloadSettings.App theApp = addProfile.addApp(app);
                theApp.setChecked(true);
                processApp(app, options);
            }
            CommandExecutor ce = new CommandExecutor(false, ds);

            ce.executeCmd(null);
        }

    }

    static void exitHelp(String string) {
        showHelpExit(string, options);
    }

    static String getProdBaseDir() {
        if (prodBaseDir == null || prodBaseDir.isEmpty()) {
            return "/AppLog/GCTI";
        } else {
            return prodBaseDir;
        }
    }

}
