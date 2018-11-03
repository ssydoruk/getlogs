/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 *
 * @author stepansydoruk
 */
public class GetLogs {

    private static String lfmtHost = null;
    private static String hostAppFile = null;
    private static String execCmd = null;
    private static String appsOpt = null;
    private static String sshUser = null;
    private static boolean listFiles = false;

    private static final Pattern regDateTimeSpec = Pattern.compile("^[0-9\\[\\]]+$");
    private static final Pattern regDirectoryStripSlash = Pattern.compile("^(.+)/+$");
    private static String dateSpec;
    private static String timeSpec;
    private static String lfmtInstance;
    private static boolean useRSync = false;

    /**
     * @param args the command line arguments
     */
    private static String sLogDirectory;
    private static String tgtHost;
    private static Hosts hosts;
    private static GetCommand execCommand;
    private static String sGrep = null;

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        Option optHosts;

        optHosts = new Option("l", "hosts", true, "file containing binding host to genesysApplication. "
                + "\n\tSingle line contains entry <hostname>,<Genesys_application_name>");
        optHosts.setRequired(true);
        options.addOption(optHosts);

        Option optCmd = new Option("c", "command", true, "command to run.\nAccepted values are " + GetCommand.showAll());
        optCmd.setRequired(true);
        optCmd.setType(String.class);
        options.addOption(optCmd);

        Option optApp = new Option("a", "app", true, "Genesys application name. Multiple can be specified"
                + " separated by coma (no spaces)");
        optApp.setRequired(true);
        options.addOption(optApp);

        Option optIsLFMT = Option.builder("f")
                .hasArg(true)
                .required(false)
                .desc("go to LFMT host for the logs\nBy default go to the server")
                .longOpt("lfmt-host")
                .build();
        options.addOption(optIsLFMT);

        Option optLFMTInstance = Option.builder()
                .hasArg(true)
                .required(false)
                .desc("name of the LFMT instance (e.g. us_lfmt)")
                .longOpt("lfmt-instance")
                .build();
        options.addOption(optLFMTInstance);

        Option optLogLevel = Option.builder("d")
                .hasArg(true)
                .required(false)
                .desc("Debug level (OFF,FATAL,ERROR,WARN,INFO,DEBUG,TRACE,ALL)")
                .longOpt("debug")
                .build();
        options.addOption(optLogLevel);

        Option optSSHUser = Option.builder("u")
                .hasArg(true)
                .required(false)
                .desc("user to be passed to ssh")
                .longOpt("user")
                .build();
        options.addOption(optSSHUser);

        Option optLogDirectory = Option.builder("b")
                .hasArg(true)
                .required(false)
                .desc("base directory to which logs should be put")
                .longOpt("log-directory")
                .build();
        options.addOption(optLogDirectory);

        Option optListFiles = Option.builder()
                .hasArg(false)
                .required(false)
                .desc("show files")
                .longOpt("list-files")
                .build();
        options.addOption(optListFiles);

        Option optUseRSync = Option.builder()
                .hasArg(false)
                .required(false)
                .desc("use rsync for file transfer")
                .longOpt("use-rsync")
                .build();
        options.addOption(optUseRSync);

        Option optForceHost = Option.builder()
                .hasArg(true)
                .required(false)
                .desc("force host <arg> instead of looking up the file")
                .longOpt("force-host")
                .build();
        options.addOption(optForceHost);

        Option optGrep = Option.builder("g")
                .hasArg(true)
                .required(false)
                .desc("grep expression to search for <arg>")
                .longOpt("grep")
                .build();
        options.addOption(optGrep);

        Option optHelp = Option.builder("h")
                .hasArg(false)
                .required(false)
                .desc("Show help and exit")
                .longOpt("help")
                .build();
        options.addOption(optHelp);

        Option optDay = Option.builder("y")
                .hasArg(true)
                .required(false)
                .desc("Specify date for logs")
                .longOpt("date")
                .build();
        options.addOption(optDay);

        Option optTime = Option.builder("t")
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
            System.out.println(e.getMessage());
            showHelpExit(options);
        }

        if (cmd.hasOption(optHelp.getLongOpt())) {
            showHelpExit(options);
        }

        initLogger((String) cmd.getParsedOptionValue(optLogLevel.getOpt()));

        lfmtHost = (String) cmd.getParsedOptionValue(optIsLFMT.getOpt());

        hostAppFile = (String) cmd.getParsedOptionValue(optHosts.getOpt());
        execCmd = (String) cmd.getParsedOptionValue(optCmd.getOpt());

        execCommand = checkCmdCommand(execCmd, options);

        appsOpt = (String) cmd.getParsedOptionValue(optApp.getOpt());

        sshUser = (String) cmd.getParsedOptionValue(optSSHUser.getOpt());

        listFiles = cmd.hasOption(optListFiles.getLongOpt());
        useRSync = cmd.hasOption(optUseRSync.getLongOpt());
        lfmtInstance = (String) cmd.getParsedOptionValue(optLFMTInstance.getLongOpt());

        sGrep = (String) cmd.getParsedOptionValue(optGrep.getLongOpt());

        sLogDirectory = (String) cmd.getParsedOptionValue(optLogDirectory.getLongOpt());
        if (sLogDirectory == null || sLogDirectory.isEmpty()) {
            sLogDirectory = ".";
        } else {
            Matcher m;
            if ((m = regDirectoryStripSlash.matcher(sLogDirectory)).find()) {
                sLogDirectory = m.group(1);
            }
        }

        //--------------------------------------
        if (logger.isDebugEnabled()) {
            logger.debug("Current directory:" + getWD());
            Runtime.getRuntime().exec("ls -l");
        }
        hosts = new Hosts(hostAppFile);

        apps = new ArrayList<>();
        String[] split = appsOpt.split(",");
        for (String string : split) {
            apps.add(string);
        }
        tgtHost = (String) cmd.getParsedOptionValue(optForceHost.getLongOpt());
        dateSpec = (String) cmd.getParsedOptionValue(optDay.getOpt());
        timeSpec = (String) cmd.getParsedOptionValue(optTime.getOpt());

        //--------------------------------------
        for (String app : apps) {
            processApp(app, options);
        }

        System.out.println(
                "allDone");

    }

    public static void processApp(String ap, Options options) throws IOException, InterruptedException {
        if (tgtHost == null || tgtHost.isEmpty()) {
            tgtHost = (String) hosts.get(ap); // first for one application only
            if (tgtHost == null) {
                System.out.println("Host for app [" + ap + "] not found; exiting");
                return;
            }
        }
        StringBuilder logsDir = new StringBuilder();

        if (lfmtHost != null) {
            logsDir.append("/Logs/")
                    .append(lfmtInstance).append("/")
                    .append(lfmtInstance).append("_cls/")
                    .append(tgtHost) //                    .append("/")
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

        StringBuilder fileNameClause = new StringBuilder();
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

        logger.debug("fileName clause: [" + fileNameClause + "]");

        switch (execCommand) {
            case GREP:
                executeGrep(ap, logsDir, fileNameClause);
                break;

            case GET:
                executeGet(ap, logsDir, fileNameClause, useRSync);
                break;

            case LS:
                executeLS(ap, logsDir, fileNameClause);
                break;

            case GREPGET:
                executeGrepGet(ap, logsDir, fileNameClause);
                break;

        }

    }

    static ArrayList<String> apps = null;
    static Logger logger;

    public static void initLogger(String par) {

        Level level = Level.INFO;
        if (par != null) {
            level = Level.toLevel(par, Level.INFO);
        }

        ConfigurationBuilder<BuiltConfiguration> builder
                = ConfigurationBuilderFactory.newConfigurationBuilder();

        AppenderComponentBuilder console
                = builder.newAppender("stdout", "Console");

        builder.add(console);

        AppenderComponentBuilder file
                = builder.newAppender("log", "File");
        file.addAttribute("fileName", "logging.log");

        builder.add(file);

        ComponentBuilder triggeringPolicies = builder.newComponent("Policies")
                .addComponent(builder.newComponent("CronTriggeringPolicy")
                        .addAttribute("schedule", "0 0 0 * * ?"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", "100M"));

        AppenderComponentBuilder rollingFile
                = builder.newAppender("rolling", "RollingFile");
        rollingFile.addAttribute("fileName", "rolling.log");
        rollingFile.addAttribute("filePattern", "rolling-%d{MM-dd-yy}.log.gz");
        rollingFile.addComponent(triggeringPolicies);
        builder.add(rollingFile);

        FilterComponentBuilder flow = builder.newFilter(
                "MarkerFilter",
                Filter.Result.ACCEPT,
                Filter.Result.DENY);
        flow.addAttribute("marker", "FLOW");
        console.add(flow);

        LayoutComponentBuilder standard
                = builder.newLayout("PatternLayout");
        standard.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");

        console.add(standard);
        file.add(standard);
        rollingFile.add(standard);

        RootLoggerComponentBuilder rootLogger
                = builder.newRootLogger(level);
        rootLogger.add(builder.newAppenderRef("stdout"));

        builder.add(rootLogger);

        Configurator.initialize(builder.build());
        logger = LogManager.getLogger();

    }

    private static void showHelpExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("utility-name", options);

        System.exit(0);
    }

    private static void showHelpExit(String msg, Options options) {
        logger.error(msg);
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

    private static String expandPattern(String dateSpec, int max) {
        int i = countDigits(dateSpec);
        return dateSpec + StringUtils.repeat("[0-9]", max - i);

    }

    private static final Pattern regCountDigitsCovered = Pattern.compile("(\\d|\\[\\d+\\])");

    private static int countDigits(String dateSpec) {
        int ret = 0;
        Matcher matcher = regCountDigitsCovered.matcher(dateSpec);
        while (matcher.find()) {
            ret++;
        }
        return ret;
    }

    static void doLog(String s) {
        logger.info(s);
    }

    static String getWD() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }

    private static void executeGet(String ap, StringBuilder logsDir, StringBuilder fileNameClause, boolean useRSync1) throws IOException, InterruptedException {
        if (useRSync1) {
            executeRSync(ap, logsDir, rSyncAddClause(fileNameClause.toString()));
        } else {
            ArrayList<String> sshParams = new ArrayList<>();
            sshParams.add("ssh");
            if (sshUser != null) {
                sshParams.addAll(Arrays.asList(new String[]{"-l", sshUser}));
            }

            if (lfmtHost != null) {
                sshParams.add(lfmtHost);
            } else {
                sshParams.add(tgtHost);

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
            procTar.readOutputs();

            procSSH.readOutputs();
            procSSH.waitFor();
            procTar.waitFor();
        }

    }

    private static void executeLS(String ap, StringBuilder logsDir, StringBuilder fileNameClause) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (sshUser != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", sshUser}));
        }

        if (lfmtHost != null) {
            sshParams.add(lfmtHost);
        } else {
            sshParams.add(tgtHost);

        }

        StringBuilder fileClause = new StringBuilder();
        if (listFiles) {
            fileClause.append("\\( -type f ");

            if (fileNameClause.length() > 0) {
                fileClause.append("-a -name ")
                        .append(fileNameClause);
            }

            fileClause.append(" \\) ");
        }
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

        procSSH.readOutputs();

        procSSH.waitFor();

    }

    private static ArrayList<String> executeGrep(String ap, StringBuilder logsDir, StringBuilder fileNameClause) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (sshUser != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", sshUser}));
        }

        if (lfmtHost != null) {
            sshParams.add(lfmtHost);
        } else {
            sshParams.add(tgtHost);

        }

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
            for (String matchedFile : execGrep(extUnp.getKey(), extUnp.getValue(), sshParams, sshCmd)) {
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

    final static private String filePrefix = "!file!";

    private static ArrayList<String> execGrep(String ext, String unp, ArrayList<String> sshParams, StringBuilder sshCmd) throws IOException, InterruptedException {
        StringBuilder grepCmd = new StringBuilder();
        grepCmd.append(sshCmd)
                .append(" -iname ").append(ext)
                .append(" | xargs bash -c '")
                .append("echo bash is here; echo params: $*; ")
                .append("for f in $*; do if ")
                .append(unp)
                .append("| egrep -q \"").append(sGrep).append("\"; then echo ").append(filePrefix).append("${f}; fi; done' -s" + "");
        ArrayList<String> paramsRun = new ArrayList<>(sshParams);
        paramsRun.add(grepCmd.toString());
        ExtProcess procSSH = new ExtProcess(paramsRun);

        procSSH.readOutputs(true, true);
        procSSH.waitFor();
        return procSSH.getSTDOut();

    }

    private static final Pattern fileBaseName = Pattern.compile("([^\\/]+)$");

    static private String stripDir(String fileName) {
        Matcher m;
        if ((m = fileBaseName.matcher(fileName)).find()) {
            return m.group(0);
        }
        return fileName;
    }

    private static void executeGrepGet(String ap, StringBuilder logsDir, StringBuilder fileNameClause) throws IOException, InterruptedException {
        ArrayList<String> executeGrep = executeGrep(ap, logsDir, fileNameClause);
        ArrayList<String> rSyncFiles = new ArrayList<>();
        for (String fileName : executeGrep) {
            rSyncFiles.addAll(rSyncAddClause(stripDir(fileName)));
        }

        executeRSync(ap, logsDir, rSyncFiles);
    }

    private static ArrayList<String> rSyncAddClause(String fileName) {
        ArrayList<String> ret = new ArrayList<String>();
        ret.add("-f");//--filter
        ret.add("+ /*" + fileName);
        return ret;
    }

    private static void executeRSync(String ap, StringBuilder logsDir, ArrayList<String> fileNameClause) throws IOException, InterruptedException {
        ArrayList<String> rsyncParams = new ArrayList<>();
        rsyncParams.add("rsync");
        rsyncParams.add("-avz");
        rsyncParams.add("-e");
        rsyncParams.add("ssh");
        rsyncParams.addAll(fileNameClause);
        rsyncParams.add("-f");
        rsyncParams.add("- **");
        StringBuilder srcSpec = new StringBuilder();
        srcSpec.append(sshUser).append("@").append(tgtHost).append(":")
                .append(logsDir).append("/").append(ap).append("/").append("");

        rsyncParams.add(srcSpec.toString());

        StringBuilder dstSpec = new StringBuilder();
        dstSpec.append(sLogDirectory).append("/").append(ap);

        rsyncParams.add(dstSpec.toString());

        ExtProcess procRSync = new ExtProcess(rsyncParams);
        procRSync.readOutputs();
        procRSync.waitFor();
    }

}
