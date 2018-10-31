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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static String dateSpec;
    private static String timeSpec;
    private static String lfmtInstance;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        Options options = new Options();
        Option optHosts;

        optHosts = new Option("l", "hosts", true, "file containing binding host to genesysApplication. "
                + "\n\tSingle line contains entry <hostname>,<Genesys_application_name>");
        optHosts.setRequired(true);
        options.addOption(optHosts);

        Option optCmd = new Option("c", "command", true, "command to run.\nAccepted values are ls, get");
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

        Option optListFiles = Option.builder()
                .hasArg(false)
                .required(false)
                .desc("show files")
                .longOpt("list-files")
                .build();
        options.addOption(optListFiles);

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

        boolean isLs = checkCmdCommand(execCmd, options);

        appsOpt = (String) cmd.getParsedOptionValue(optApp.getOpt());

        sshUser = (String) cmd.getParsedOptionValue(optSSHUser.getOpt());

        listFiles = cmd.hasOption(optListFiles.getLongOpt());
        lfmtInstance = (String) cmd.getParsedOptionValue(optLFMTInstance.getLongOpt());

        //--------------------------------------
        Hosts hosts = new Hosts(hostAppFile);

        apps = new ArrayList<>();
        String[] split = appsOpt.split(",");
        for (String string : split) {
            apps.add(string);
        }
        //--------------------------------------

        String ap = apps.get(0);
        String tgtHost = (String) hosts.get(ap); // first for one application only
        if (tgtHost == null) {
            System.out.println("Host for app [" + ap + "] not found; exiting");
            return;
        }

        dateSpec = (String) cmd.getParsedOptionValue(optDay.getOpt());
        timeSpec = (String) cmd.getParsedOptionValue(optTime.getOpt());

        if (dateSpec != null && !dateSpec.isEmpty() && !regDateTimeSpec.matcher(dateSpec).matches()) {
            showHelpExit("Date is specified but the format is incorrect", options);
        }
        if (timeSpec != null && !timeSpec.isEmpty() && !regDateTimeSpec.matcher(timeSpec).matches()) {
            showHelpExit("Time is specified but the format is incorrect", options);
        }

        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (sshUser != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", sshUser}));
        }

        StringBuilder logsDir = new StringBuilder();

        if (lfmtHost != null) {
            sshParams.add(lfmtHost);
            logsDir.append("/Logs/")
                    .append(lfmtInstance).append("/")
                    .append(lfmtInstance).append("_cls/")
                    .append(tgtHost) //                    .append("/")
                    //                    .append(ap)
                    ;
        } else {
            sshParams.add(tgtHost);
            logsDir.append("/AppLog/GCTI");

        }

        StringBuilder fileNameClause = new StringBuilder();
        fileNameClause.append("\\*\\.");
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
                .append("\\.\\*");

        StringBuilder fileClause = new StringBuilder();
        if (listFiles || !isLs) {
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
        if (!listFiles && isLs) {
            sshCmd.append(" -o -type d ");
        }
        if (isLs) {
            sshCmd.append("-print | sort");
        } else {
//            sshCmd.append(" -print ");
            sshCmd.append(" -exec ");
            sshCmd.append("tar -");
            if (lfmtHost == null) {
                sshCmd.append("z");
            }
            sshCmd.append("cf - ")
                    .append("{} +");
        }
        sshParams.add(sshCmd.toString());
        ProcessBuilder pbSSH = getProcessBuilder(sshParams);
        File f = new File("/Users/stepansydoruk/NetBeansProjects/getLogs/aa.txt");
//            pbSSH.redirectOutput(f);
        Process procSSH = pbSSH.start();

        ProcessBuilder pbTar = null;
        Process procTar = null;
        if (!isLs) {
            ArrayList<String> tarParams = new ArrayList<>();
            tarParams.add("tar");
            tarParams.add("-xv");
            tarParams.add("-f");
            tarParams.add("-");

            pbTar = getProcessBuilder(tarParams);
            pbTar.redirectInput(pbSSH.redirectOutput());
//            pbTar.redirectOutput(f);

            procTar = pbTar.start();
            PipeConnector pc = new PipeConnector(procSSH.getInputStream(), procTar.getOutputStream());
            pc.run();
        }

        BufferedReader stdInput = null;
        if (isLs) {
            stdInput = new BufferedReader(new InputStreamReader(procSSH.getInputStream()));
        }
        BufferedReader stdError = new BufferedReader(new InputStreamReader(procSSH.getErrorStream()));
        String s = null;

        // read the output from the command
        if (stdInput != null) {
            System.out.println(
                    "Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
        }

        // read any errors from the attempted command
        System.out.println(
                "Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
        if (procTar != null) {
            stdInput = new BufferedReader(new InputStreamReader(procTar.getInputStream()));
            stdError = new BufferedReader(new InputStreamReader(procTar.getErrorStream()));

            // read the output from the command
            System.out.println(
                    "Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // read any errors from the attempted command
            System.out.println(
                    "Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        }

        procSSH.waitFor();
        if (procTar != null) {
            procTar.waitFor();
        }

        System.out.println(
                "allDone");

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
    private static boolean checkCmdCommand(String execCmd, Options options) {
        if (execCmd != null && !execCmd.isEmpty()) {
            if (execCmd.equalsIgnoreCase("ls")) {
                return true;
            } else if (execCmd.equalsIgnoreCase("get")) {
                return false;
            }
        }
        logger.error("Incorrect value for command - [" + execCmd + "]");
        showHelpExit(options);
        return true; // never reached
    }

    private static ProcessBuilder getProcessBuilder(ArrayList<String> sshParameters) throws IOException {
        if (logger.isDebugEnabled()) {
            StringBuilder l = new StringBuilder();
            for (String sshParameter : sshParameters) {
                if (l.length() > 0) {
                    l.append(" ");
                }
                l.append(sshParameter);
            }
            logger.debug("Executing: [" + l + "]");
        }

        return new ProcessBuilder(sshParameters);

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

}
