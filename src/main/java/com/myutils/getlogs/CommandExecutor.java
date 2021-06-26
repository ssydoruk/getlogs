/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import static Utils.SystemClipboard.getSystemClipboard;

import Utils.*;
import Utils.UnixProcess.*;

import static Utils.Util.rSyncAddClause;

import com.jidesoft.dialog.*;

import static com.myutils.getlogs.GetLogs.logger;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.table.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

/**
 * @author stepan_sydoruk
 */
public final class CommandExecutor {

    // public static void main(String[] args) throws Exception {
    // System.out.println(cloudPattern("bb{YYYY}_{MM}_{DD}_{HH}aa.log", "20190",
    // "1"));
    // System.out.println(cloudPattern("bb{YYYY}_{MM}_{DD}_{HH}aa.log", "201901",
    // "1"));
    // System.out.println(cloudPattern("bb{YYYY}_{MM}_{DD}_{HH}aa.log", "2019012",
    // "1"));
    // System.out.println(cloudPattern("bb{YYYY}_{MM}_{DD}_{HH}aa.log", "20190124",
    // "1"));
    // }
    private static final Pattern regVariable = Pattern.compile("\\{([A-Z]{2,4}|NAME)\\}");
    private static final Pattern regPostAction = Pattern.compile("\\{(NAME|OUTDIR)\\}");
    private static final Pattern ptFullFileName = Pattern.compile("([^/]+)/([^/]+)$");
    private static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private static String cloudPattern(String fileNameRegex, String datePattern, String timePattern, App ap) {
        logger.debug(fileNameRegex + "-" + datePattern + "-" + timePattern);
        int pos = 0;
        Matcher m;
        StringBuilder ret = new StringBuilder();
        while ((m = regVariable.matcher(fileNameRegex)).find(pos)) {
            ret.append(fileNameRegex, pos, m.start());
            switch (m.group(1)) {
                case "YYYY":
                    ret.append(fillPattern(datePattern, 0, 4));
                    break;
                case "MM":
                    ret.append(fillPattern(datePattern, 4, 2));
                    break;
                case "DD":
                    ret.append(fillPattern(datePattern, 6, 2));
                    break;
                case "HH":
                    ret.append(fillPattern(timePattern, 0, 2));
                    break;
                case "MI":
                    ret.append(fillPattern(timePattern, 2, 2));
                    break;
                case "SS":
                    ret.append(fillPattern(timePattern, 4, 4));
                    break;
                case "NAME":
                    ret.append(ap.getName());
                    break;
                default:
                    SettingsDialog.error("Incorrect specification [" + m.group(1) + "] " + "in pattern ["
                            + fileNameRegex + "]. Allowed: YYYY MM DD HH MI SS");
                    return null;
            }

            // ret.append(StringUtils.repeat("^", m.end() - m.start()));
            pos = m.end();
        }
        if (pos < fileNameRegex.length()) {
            ret.append(fileNameRegex.substring(pos));
        }
        return ret.toString();

    }

    private static StringBuilder fillPattern(String datePattern, int start, int count) {
        StringBuilder ret1 = new StringBuilder();
        int pos = 0;
        int cnt;
        Matcher m;
        int filled = 0;
        if (datePattern != null && !datePattern.isEmpty()) {
            for (cnt = 0; cnt < start; cnt++) {// skipping to the start
                if ((m = GetLogs.regRegDigits.matcher(datePattern)).find(pos)) {
                    pos = m.end();
                } else {
                    break;
                }
            }
            // pos is position of first character to fill
            if (cnt == start) {
                for (cnt = 0; cnt < count; cnt++) {// skipping to the start
                    if ((m = GetLogs.regRegDigits.matcher(datePattern)).find(pos)) {
                        ret1.append(datePattern, m.start(), m.end());
                        pos = m.end();
                        filled++;
                    }
                }
            }
        }
        ret1.append(StringUtils.repeat("[0-9]", count - filled));

        return ret1;
    }

    private static void cancelExecutor() {
        logger.debug("Cancelling...");
        synchronized (executor) {
            boolean terminatedOK = true;
            List<Runnable> shutdownNow = executor.shutdownNow();
            if (shutdownNow != null && !shutdownNow.isEmpty()) {
                try {
                    if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                        terminatedOK = false;
                        logger.error("Not all thread terminated after timeout");
                    }
                } catch (InterruptedException ex) {
                    logger.error(ex);
                }
            }
            if (terminatedOK) {
                executor.purge();
                executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            }
            if (executor.isTerminated()) {
                logger.error("executor terminated");

            }
        }
    }

    private DownloadSettings ds;

    private final boolean isText;
    private Window parent;
    final private ArrayList<JTableFileEntry> lsFilesAll = new ArrayList<>();
    private ArrayList<JTableFileEntry> lsFilesLast = new ArrayList<>();
    private RequestProgress rp = null;
    private final ExtProcessManager extProcessManager;
    private final int ret = StandardDialog.RESULT_CANCELLED;
    InfoPanel lsOutput;
    InfoPanel lsPasteOutput;
    final ArrayList<SavedSearchStorage> savedSearch = new ArrayList<>();
    JTableFileList lsTab = new JTableFileList();
    JTablePasteFileList lsGeneralTab = null;
    SSHClientWrapper sshClient = null;

    public CommandExecutor(boolean isText) {
        this.isText = isText;
        extProcessManager = new ExtProcessManager();
        if (GetLogs.isbIsSSHJava()) {
            // todo: make defaultTimeoutSeconds configurable
            sshClient = new SSHClientWrapper(20000);
        }
    }

    public CommandExecutor(Window p) {
        this(false);
        parent = p;
    }

    CommandExecutor(boolean b, DownloadSettings ds) {
        this(b);
        this.ds = ds;
    }

    public DownloadSettings getDs() {
        return ds;
    }

    public void setDs(DownloadSettings ds) {
        this.ds = ds;
    }

    public void executeCmd(java.awt.Window parent) throws IOException, InterruptedException {
        if (!ds.isAppLogs() && !ds.isLcaLogs()) {
            JOptionPane.showMessageDialog(parent, "Either Application or LCA checkbox needs to be checked",
                    "Cannot continue", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SettingsDialog.info("* " + ds.getActionCommand() + " started");
        FileUtils.forceMkdir(new File(ds.getOutputDir()));

        if (parent != null) {
            doExecuteCmd(parent, this);
        } else {
            if (!isText) {
                if (lsTab == null) {
                    lsTab = new JTableFileList();
                }
                lsTab.clearTable();

            }
            lsFilesAll.clear();

        }
    }

    @SuppressWarnings("null")
    private String getLogDir(AppProfile appProfile, App ap, boolean isLFMT, String h) {
        StringBuilder logsDir = new StringBuilder();

        if (isLFMT) {
            DownloadSettings.LFMTHostInstance lfmtHostInstance = appProfile.getLFMT();
            if (lfmtHostInstance == null || lfmtHostInstance.getHost() == null) {
                GetLogs.exitHelp("LFMT not configured properly for app " + ap);
            }
            logsDir.append(lfmtHostInstance.getBaseDir());
            String instance = lfmtHostInstance.getInstance();
            if (instance != null && !instance.isEmpty()) {
                logsDir.append(lfmtHostInstance.getInstance()).append("/").append(lfmtHostInstance.getInstance())
                        .append("_cls");
            }

            logsDir.append("/").append(h) // .append("/")
            // .append(ap)
            ;
        } else {
            logsDir.append(GetLogs.getProdBaseDir());

        }
        return logsDir.toString();
    }

    protected void executeCmd(AppProfile appProfile, App ap, boolean isLFMT) throws IOException, InterruptedException {
        SettingsDialog.info("executing for profile [" + appProfile.toString() + "] ap[" + ap + "] lfmt:" + isLFMT);
        HostAppdir theAppHost;
        if (GetLogs.appHost == null || GetLogs.appHost.isEmpty()) {
            theAppHost = GetLogs.getHosts().get(ap.getName()); // first for one application only
            if (theAppHost == null) {
                GetLogs.exitHelp("Host for app [" + ap + "] not found; exiting");
                return;
            }
        } else {
            theAppHost = new HostAppdir(GetLogs.appHost, null);
        }

        String logsDir = getLogDir(appProfile, ap, isLFMT, theAppHost.toString());

        if (ds.isAppLogs()) {
            ArrayList<String> fileNameClause = getFileNameClause(appProfile, ap, false);
            ArrayList<JTableFileEntry> lsFiles = executeCmd(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT,
                    false);
            if (lsFiles != null && !lsFiles.isEmpty()) {
                synchronized (lsFilesAll) {
                    lsFilesAll.addAll(lsFiles);
                }
                if (ds.getActionCommand() == GetCommand.GET) {
                    executeRSync(lsFiles);
                }
            }

        }
        if (ds.isLcaLogs()) {
            ArrayList<String> fileNameClause = getFileNameClause(appProfile, ap, true);
            ArrayList<JTableFileEntry> lsFiles = executeCmd(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT,
                    true);
            if (lsFiles != null && !lsFiles.isEmpty()) {
                synchronized (lsFilesAll) {
                    lsFilesAll.addAll(lsFiles);
                }
                if (ds.getActionCommand() == GetCommand.GET) {
                    executeRSync(lsFiles);
                }
            }
        }

    }

    protected SavedSearchStorage getStorage(AppProfile appProfile, App ap, HostAppdir theAppHost,
            String logsDir, boolean lfmt, boolean lcaLog) {
        SavedSearchStorage _ret = new SavedSearchStorage(appProfile, ap, theAppHost, logsDir, lfmt, lcaLog);

        synchronized (savedSearch) {
            for (SavedSearchStorage savedSearchStorage : savedSearch) {
                if (savedSearchStorage.equals(_ret)) {
                    return savedSearchStorage;
                }
            }
            savedSearch.add(_ret);
        }

        return _ret;

    }

    private String quoteDouble() {
        return "\"";
    }

    private String remoteSSHCmd(AppProfile appProfile, App ap, String logsDir, ArrayList<String> fileNameClause,
            boolean lcaLog, boolean isLFMT) {
        StringBuilder sshCmd = new StringBuilder();

        // this is to play with permissions.
        // sshCmd.append("sudo -u svc-gsys -s bash -c \"");
        String u = GetLogs.getsRSyncUserName();
        if (StringUtils.isBlank(u)) {
            u = GetLogs.getsUserName();
        }
        if (StringUtils.isNotBlank(u)) {
            sshCmd.append("sudo -u ").append(u).append(" -s ");
        }

        sshCmd.append("bash -c ").append(quoteDouble());

        sshCmd.append("cd ").append((isLFMT) ? logsDir : appProfile.getLogDirectory()).append("; ");
        // sshCmd.append(" declare -a arr=( \\\"-001\\\" \\\"-768\\\" ); for ext in
        // \\\"\\${arr[@]}\\\"; do");
        sshCmd.append(" declare -a arr=(");
        sshCmd.append(" \\").append(quoteDouble());
        for (int i = 0; i < fileNameClause.size(); i++) {
            String s = fileNameClause.get(i);
            if (i > 0) {
                sshCmd.append("\\").append(quoteDouble()).append(" ").append("\\").append(quoteDouble());

            }
            sshCmd.append(s);
        }

        sshCmd.append("\\").append(quoteDouble()).append(" ");

        sshCmd.append(" ); for ext in ").append(quoteDouble()).append("\\${arr[@]}").append(quoteDouble())
                .append("; do");

        // sshCmd.append(" echo ").append("\\${ext}").append(" ; ");
        // this is used for testing
        // sshCmd.append(" pwd; echo \\$ext; echo \\${ext}; ");
        sshCmd.append(" find ").append((lcaLog) ? "lca" : ap.getAppDir()).append(" -maxdepth 1 ")
                .append("-name \\${ext} ");
        // sshCmd.append(" -a -type f ");
        if (appProfile.isIsGenesysName()) {
            sshCmd.append(" -a ! \\( -name \\*snapshot.log \\) ");
        }
        sshCmd.append(" ");

        if (!ds.isListFiles()) {
            sshCmd.append(" -o -type d ");
        }

        // below is if need to change permissions on the fly. DO not like the idea
        // though
        // sshCmd.append(" -execdir chmod g+r,a+r {} \\; -print | sort -r");
        sshCmd.append(" -print | sort -r");
        if (ds.getTimeProfile() == SettingsPanel.TimeProfile.VALUE_FILES) {
            sshCmd.append(" | head -").append(ds.getHours());
        }

        sshCmd.append("| xargs stat -c \\\"%n %s\\\" ; done");

        sshCmd.append(quoteDouble());
        logMessage(Level.INFO, sshCmd.toString(), appProfile, ap);
        return sshCmd.toString();
    }

    private ArrayList<JTableFileEntry> executeLS(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
            ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        if (ap.isIsWindows()) {
            return executeLSWin(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        } else {
            return executeLSLinux(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        }

    }

    private String connectWindowsShare(AppProfile appProfile, App ap) {
        Pair<String, String> winPath = getWinDrive(ap.getAppDir());
        String logPath = "\\\\" + ap.getHost() + "\\" + winPath.getKey();
        String ret = logPath + winPath.getValue();
        if (shareConnected(logPath, appProfile, ap)) {
            return ret;
        }
        StringBuilder cmd = new StringBuilder("net use ");
        cmd.append(logPath).append(" /user:").append(GetLogs.getSshUser()).append(" ").append(GetLogs.getSshPassword());
        Pair<ArrayList<String>, ArrayList<String>> ret1 = null;
        try {
            ret1 = executeCommand(cmd.toString(), true, true, appProfile, ap);
            if (ret1 != null) { // success
                ArrayList<String> stdOut = ret1.getKey();
                if (!stdOut.isEmpty() && stdOut.size() > 1
                        && stdOut.get(0).equals("The command completed successfully.")) {
                    return ret;
                }
                logMessage(Level.ERROR,
                        (new StringBuilder(getLogPrefix(appProfile, ap))).append("\n\tstdout [")
                                .append(StringUtils.join(stdOut, "\n")).append("]\n\t").append("\n\tstderr [")
                                .append(StringUtils.join(ret1.getValue(), "\n")).append("]\n\t").toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * executes 'net use' to check if shared path is already mounted
     *
     * @param logPath
     * @return
     */
    private boolean shareConnected(String logPath, AppProfile appProfile, App ap) {
        Pair<ArrayList<String>, ArrayList<String>> ret1 = null;
        String logPathLower = logPath.toLowerCase();
        try {
            ret1 = executeCommand("net use", true, true, appProfile, ap);
            if (ret1 != null) { // success
                for (String s : ret1.getKey()) {
                    String[] s1 = StringUtils.split(s, " ", 3);
                    if (s1.length >= 3 && s1[0].equals("OK") && s1[1].toLowerCase().equals(logPathLower))
                        return true;
                }
            }

        } catch (IOException e) {
            logMessage("Not able to check connected share", e, appProfile, ap);
        } catch (InterruptedException e) {
            logMessage("Not able to check connected share", e, appProfile, ap);
        }
        return false;
    }

    /**
     * Checks if first two letters of 'logDir' is windows drive if so, returns By
     * default returns d$
     *
     * @param logDir
     * @return Pair of [1st letter to lower case with $ attached], [Path without
     *         dir]
     */
    private Pair<String, String> getWinDrive(String logDir) {
        if (StringUtils.isNotEmpty(logDir) && logDir.length() > 1
                && (logDir.charAt(1) == ':' || logDir.charAt(1) == '$')) {
            return new Pair(
                    ((new StringBuilder()).append(Character.toLowerCase(logDir.charAt(0))).append('$')).toString(),
                    logDir.substring(2));
        } else {
            return new Pair<>("d$", StringUtils.isNotBlank(logDir) ? logDir : "");
        }
    }

    private ArrayList<JTableFileEntry> executeLSWin(AppProfile appProfile, App ap, HostAppdir theAppHost,
            String logsDir, ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog)
            throws IOException, InterruptedException {

        String logPath = connectWindowsShare(appProfile, ap);
        if (logPath != null) { // success

            HashMap<String, ArrayList<Pair<File, BasicFileAttributes>>> nameSuffixes = new HashMap<>();
            HashMap<String, Boolean> profileNameSuffixes = appProfile.getNameSuffixes();
            if (profileNameSuffixes != null) {
                for (Map.Entry<String, Boolean> entry : profileNameSuffixes.entrySet()) {
                    if (entry.getValue()) {
                        nameSuffixes.put(entry.getKey(), new ArrayList<>());
                    }
                }
            } else {
                nameSuffixes.put("", new ArrayList<>());
            }
            if (nameSuffixes.isEmpty())
                return null;

            ArrayList<ArrayList<Pair<File, BasicFileAttributes>>> filesByType = new ArrayList<>();
            ArrayList<Pair<File, BasicFileAttributes>> allFiles = new ArrayList<>();
            Matcher rxDateTime = null;
            if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX) {
                rxDateTime = Pattern.compile(getFileRegexMatch(ds.getDateSpec(), ds.getTimeSpec())).matcher("");
            }
            Collection<File> filesList = FileUtils.listFiles(new File(logPath), null, true);
            for (File f : filesList) {
                String fileName = f.getName().toLowerCase();
                if (!f.getName().toLowerCase().contains("snapshot.log")) {
                    for (Map.Entry<String, ArrayList<Pair<File, BasicFileAttributes>>> entryNameSuffix : nameSuffixes
                            .entrySet()) {

                        if (entryNameSuffix.getKey().isEmpty()
                                || (entryNameSuffix.getKey().equals(".")
                                        || fileName.contains(ap.getName().toLowerCase()))
                                || (fileName.contains(entryNameSuffix.getKey().toLowerCase()))) {
                            if (rxDateTime == null || rxDateTime.reset(fileName).find())
                                entryNameSuffix.getValue().add(
                                        new Pair<>(f, Files.readAttributes(f.toPath(), BasicFileAttributes.class)));
                            break;
                        }
                    }
                }
            }
            for (ArrayList<Pair<File, BasicFileAttributes>> files : nameSuffixes.values()) {
                files.sort(new Comparator<Pair<File, BasicFileAttributes>>() {
                    @Override
                    public int compare(Pair<File, BasicFileAttributes> o1, Pair<File, BasicFileAttributes> o2) {
                        return o2.getValue().creationTime().compareTo(o1.getValue().creationTime());
                    }
                });
            }

            ArrayList<JTableFileEntry> lsFiles = new ArrayList<JTableFileEntry>();
            if (ds.getTimeProfile() == SettingsPanel.TimeProfile.VALUE_FILES) {
                int cnt = 0;
                int max = Integer.parseInt(ds.getHours());
                if (max <= 0)
                    max = 99999999;
                for (ArrayList<Pair<File, BasicFileAttributes>> files : nameSuffixes.values()) {
                    for (Pair<File, BasicFileAttributes> f : files) {
                        if (cnt++ >= max) {
                            break;
                        }
                        String string = f.getKey().getAbsoluteFile().getAbsolutePath();
                        lsFiles.add(new JTableFileEntry(appProfile,
                                getStorage(appProfile, ap, theAppHost, logsDir, isLFMT, lcaLog), string));
                    }
                }
            } else if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX) {
                for (ArrayList<Pair<File, BasicFileAttributes>> files : nameSuffixes.values()) {
                    for (Pair<File, BasicFileAttributes> f : files) {
                        String string = f.getKey().getAbsoluteFile().getAbsolutePath();
                        lsFiles.add(new JTableFileEntry(appProfile,
                                getStorage(appProfile, ap, theAppHost, logsDir, isLFMT, lcaLog), string));
                    }
                }

            }
            ((AbstractTableModel) lsTab.getModel()).fireTableDataChanged();
            return lsFiles;
        } else {
            return null;
        }
    }

    private String getFileRegexMatch(String dateSpec, String hours) {
        StringBuilder rx = new StringBuilder("\\.");
        if (StringUtils.isNotBlank(dateSpec)) {
            rx.append(dateSpec).append("\\d*");
        } else {
            rx.append("\\d{8}");
        }
        rx.append("_");
        if (StringUtils.isNotBlank(hours)) {
            rx.append(hours).append("\\d*");
        } else {
            rx.append("\\d{6}");
        }
        rx.append("_\\d{3}\\.");
        return rx.toString();
    }

    private ArrayList<JTableFileEntry> executeLSLinux(AppProfile appProfile, App ap, HostAppdir theAppHost,
            String logsDir, ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog)
            throws IOException, InterruptedException {

        String remoteCmd = remoteSSHCmd(appProfile, ap, logsDir, fileNameClause, lcaLog, isLFMT);
        int executionResult;
        List<String> stdout;
        List<String> stderr;

        if (GetLogs.isbIsSSHJava()) {
            RemoteExecutionResult ret1 = sshClient.executeRemoteCommand(GetLogs.getSshUser(), GetLogs.getSshPassword(),
                    theAppHost.getHost(), 22, remoteCmd);
            executionResult = ret1.getRetCode();
            stderr = ret1.getStderr();
            stdout = ret1.getStdout();
        } else {
            ArrayList<String> sshParams = new ArrayList<>();
            HashMap<String, Boolean> nameSuffixes = appProfile.getNameSuffixes();

            sshParams.add("ssh");
            if (GetLogs.sshOptions != null) {
                sshParams.addAll(Arrays.asList(StringUtils.split(GetLogs.sshOptions)));
            }
            sshParams.addAll(Arrays.asList("-o", "StrictHostKeyChecking no"));

            if (isLFMT) {
                sshParams.add(appProfile.getLFMT().getHost());
            } else {
                sshParams.add(theAppHost.getHost());
            }

            sshParams.add(remoteCmd);

            ExtProcess procSSH;
            procSSH = extProcessManager.addProcess(new ExtProcessApp(appProfile, ap, sshParams, false, true));
            procSSH.startProcess(true, true);

            executionResult = procSSH.waitFor();
            stderr = procSSH.getErrBuf();
            stdout = procSSH.getSTDOut();
            logger.debug("process terminated, result: " + executionResult);
            extProcessManager.doneProcess(procSSH);
        }
        if (executionResult != 0) {
            SettingsDialog.error("LS failed, error code: " + executionResult);
            if (stderr != null && !stderr.isEmpty()) {
                logMessage(Level.ERROR, StringUtils.join(stderr, " | "));
            }
        } else {
            if (stdout != null) {
                ArrayList<JTableFileEntry> lsFiles = null;

                lsFiles = new ArrayList<>();
                boolean startFound = false;
                String savedPrefix = null;
                for (String stdoutLine : stdout) {
                    // lsTab.addRow(appProfile, ap, theAppHost, string, logsDir.toString(), isLFMT,
                    // lcaLog);
                    String[] split = StringUtils.split(stdoutLine);
                    String fileName = split[0];
                    if (ds.getTimeProfile() == SettingsPanel.TimeProfile.RANGE) {
                        Pair<Long, String> utcTime = appProfile.getFileNameTime(fileName);
                        boolean shouldAdd = true;
                        UTCTimeRange timeRange = ds.getTimeRange();
                        if (utcTime != null) { // was able to parse time name
                            if (utcTime.getKey() > timeRange.getEnd()) {
                                shouldAdd = false;
                            } else {
                                if ((utcTime.getKey() > timeRange.getStart())) {
                                    shouldAdd = true;
                                } else {
                                    /* so the assumption is that files are always sorted */
                                    if (startFound == false) {
                                        startFound = true;
                                        savedPrefix = utcTime.getValue();
                                        shouldAdd = true;
                                    } else {
                                        if (savedPrefix == null || !savedPrefix.equals(utcTime.getValue())) {
                                            startFound = false;
                                        }
                                        shouldAdd = false;
                                    }
                                }
                            }
                        }
                        logger.debug("file [" + fileName + "] range: " + timeRange.toString()
                        // +" utcTime:" + utcTime + "timeRange:" + timeRange + "(utcTime >
                        // timeRange.getStart()): " + (utcTime > timeRange.getStart()) + " (utcTime <
                        // timeRange.getEnd()):" + (utcTime < timeRange.getEnd())
                                + " shouldadd: " + shouldAdd);
                        if (shouldAdd) {
                            lsFiles.add(new JTableFileEntry(appProfile,
                                    getStorage(appProfile, ap, theAppHost, logsDir, isLFMT, lcaLog), fileName));
                        }

                    } else {
                        lsFiles.add(new JTableFileEntry(appProfile,
                                getStorage(appProfile, ap, theAppHost, logsDir, isLFMT, lcaLog), fileName));
                    }
                }
                logMessage("ls successful " + ((isLFMT) ? " on LFMT" : "") + " : got " + lsFiles.size() + " files",
                        appProfile, ap);
                if (stderr != null && !stderr.isEmpty()) {
                    logMessage(Level.ERROR, StringUtils.join(stderr, " | "));
                    // lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap,
                    // theAppHost, null, logsDir, isLFMT, lcaLog), StringUtils.join(errBuf, " |
                    // ")));
                    // lsTab.addRow(appProfile, ap, theAppHost, null, logsDir.toString(), isLFMT,
                    // lcaLog, StringUtils.join(errBuf, " | "));

                }

                ((AbstractTableModel) lsTab.getModel()).fireTableDataChanged();
                return lsFiles;
            } else {
                SettingsDialog.error("LS failed, error code: " + executionResult);
                if (stderr != null && !stderr.isEmpty()) {
                    logMessage(Level.ERROR, StringUtils.join(stderr, " | "));
                }
                String s = (stderr != null && stderr.size() > 0) ? "\n" + StringUtils.join(stderr, "\n") : "<Empty>";
                logMessage(Level.ERROR, "Command successful but stdout is empty. Stderr: " + s);
            }
        }
        return null;

    }

    private String osSpecificPath(String outputDir) {
        // if (Utils.Util.getOS() == Util.OS.WINDOWS) {
        //
        // return "/cygdrive/" + StringUtils.replaceChars(outputDir.replace(":", ""),
        // "\\", "/");
        // } else {
        return outputDir;
        // }
    }

    Pair<ArrayList<String>, ArrayList<String>> uncheckNonPrimary() {
        Collection<App> checkedApps = ds.getCheckedApps();
        if (checkedApps != null) {
            ArrayList<String> appNames = new ArrayList<>(checkedApps.size());
            for (App checkedApp : checkedApps) {
                appNames.add("\"" + checkedApp.getName() + "\"");
            }
            try {
                Pair<ArrayList<String>, ArrayList<String>> cmdOuts = executeCommand(
                        StringUtils.join(new String[] { ds.getStatusScript(), StringUtils.join(appNames, " ") }, " "),
                        true, true);
                logger.log(Level.INFO, StringUtils.join(cmdOuts));
                if (cmdOuts != null) {
                    for (String string : cmdOuts.getKey()) {
                        String[] split = StringUtils.split(string, ",", 3);
                        logger.log(Level.INFO, StringUtils.join(split, " - "));

                    }
                }
                return cmdOuts;
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(CommandExecutor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    private void lsPastedFiles(HashMap<FilesToGet, ArrayList<String>> r) {
        QueryTaskBase tsk;
        lsFilesAll.clear();
        if (!isText) {
            if (lsTab == null) {
                lsTab = new JTableFileList();
            }
            lsTab.clearTable();

        }

        tsk = new QueryThreadingTask(this, new IThreadingSubTask() {
            @Override
            public ArrayList<ISubTask> task() throws InterruptedException, IOException {

                ArrayList<ISubTask> ret1 = new ArrayList<>();

                for (Map.Entry<FilesToGet, ArrayList<String>> entry : r.entrySet()) {
                    FilesToGet key = entry.getKey();
                    ArrayList<String> value1 = entry.getValue();
                    ArrayList<String> value = new ArrayList<>();
                    for (String string : value1) {
                        value.add(string);

                    }

                    if (getDs().isLfmt()) {
                        ret1.add(new ISubTask() {
                            @Override
                            public void task() throws InterruptedException, IOException {
                                HostAppdir hh = GetLogs.getHosts().lookupHost(key.getApp().getName());

                                ArrayList<JTableFileEntry> lsFiles = executeLS(key.getProfile(), key.getApp(), hh,
                                        getLogDir(key.getProfile(), key.getApp(), true, hh.toString()), value, true,
                                        false);
                                if (lsFiles != null && !lsFiles.isEmpty()) {
                                    synchronized (lsFilesAll) {
                                        lsFilesAll.addAll(lsFiles);
                                    }
                                }
                            }
                        });
                    }
                    if (getDs().isProd()) {
                        ret1.add(new ISubTask() {
                            @Override
                            public void task() throws InterruptedException, IOException {
                                HostAppdir hh = GetLogs.getHosts().lookupHost(key.getApp().getName());

                                ArrayList<JTableFileEntry> lsFiles = executeLS(key.getProfile(), key.getApp(), hh,
                                        getLogDir(key.getProfile(), key.getApp(), false, hh.toString()), value, false,
                                        false);
                                if (lsFiles != null && !lsFiles.isEmpty()) {
                                    synchronized (lsFilesAll) {
                                        lsFilesAll.addAll(lsFiles);
                                    }
                                }
                            }
                        });
                    }
                }
                return ret1;
            }
        });
        if (rp == null) {
            rp = new RequestProgress(parent, false, tsk);
        }
        tsk.setRp(rp);
        tsk.execute();
    }

    private void executeGrepGet(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
            ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        ArrayList<String> foundFiles = executeGrep(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        if (foundFiles != null) {
            executeDownload(appProfile, ap, theAppHost, logsDir, foundFiles, isLFMT, lcaLog);
        }
    }

    private ArrayList<String> executeGrep(AppProfile appProfile, App ap, HostAppdir appHost1, String logsDir,
            ArrayList<String> fileNameClause, boolean isLFMT, boolean isLCA) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (GetLogs.getsUserName() != null) {
            sshParams.addAll(Arrays.asList("-l", GetLogs.getsUserName()));
        }
        if (isLFMT) {
            DownloadSettings.LFMTHostInstance lfmt1 = appProfile.getLFMT();
            if (lfmt1 == null) {
                return null;
            }
            sshParams.add(lfmt1.getHost());
        } else {
            sshParams.add(appHost1.getHost());
        }

        StringBuilder fileClause = new StringBuilder();
        // fileClause.append("\\(");

        fileClause.append(sshNameClause(fileNameClause));

        // fileClause.append(" \\) ");
        StringBuilder sshCmd = new StringBuilder();

        sshCmd.append("cd ").append(logsDir).append("; ");
        sshCmd.append("find ").append((isLCA) ? "lca" : ap).append(" -maxdepth 1 ").append(fileClause);
        sshCmd.append(" ");
        // sshCmd.append("\\( ")
        // .append(" -iname *.log -execdir grep Trc {} \\; -true ");
        // sshCmd.append("\\)");
        // sshCmd.append(" -o ");
        ArrayList<String> matchedFiles = new ArrayList<>();
        for (Map.Entry<String, String> extUnp : GetLogs.extUnpacker.entrySet()) {
            for (String matchedFile : GetLogs.execGrep(extUnp.getKey(), extUnp.getValue(), sshParams, sshCmd,
                    ds.getGrepText())) {
                if (matchedFile.startsWith(GetLogs.filePrefix)) {
                    matchedFiles.add(matchedFile.substring(GetLogs.filePrefix.length()));
                } else {
                    GetLogs.logger.error("Not file name: [" + matchedFile + "]Ï");
                }
            }

        }
        return matchedFiles;
    }

    private String replacePostActionVars(String word) {
        int pos = 0;
        Matcher m;
        StringBuilder _ret = new StringBuilder();
        while ((m = regPostAction.matcher(word)).find(pos)) {
            _ret.append(word, pos, m.start());
            if (m.group(1).equals("OUTDIR")) {
                _ret.append(osSpecificPath(ds.getOutputDir()));

            } else {
                SettingsDialog.error("Incorrect specification [" + m.group(1) + "] " + "in pattern [" + word
                        + "]. Allowed: {OUTDIR}");
                return null;
            }

            // ret.append(StringUtils.repeat("^", m.end() - m.start()));
            pos = m.end();
        }
        if (pos < word.length()) {
            _ret.append(word.substring(pos));
        }
        return _ret.toString();

    }

    private String getLogPrefix(AppProfile appProfile, App ap) {
        return new StringBuilder("[").append(appProfile.getName()).append("]/[").append(ap.getName()).append("]")
                .append(ap.isIsWindows() ? "win" : "lin").append(":").toString();
    }

    private Pair<ArrayList<String>, ArrayList<String>> executeCommand(String key, boolean saveStdOut,
            boolean saveStdErr, AppProfile appProfile, App ap) throws IOException, InterruptedException {
        return executeCommand(key, saveStdOut, saveStdErr, getLogPrefix(appProfile, ap));
    }

    private Pair<ArrayList<String>, ArrayList<String>> executeCommand(String key, boolean saveStdOut,
            boolean saveStdErr) throws IOException, InterruptedException {
        return executeCommand(key, saveStdOut, saveStdErr, "");
    }

    private Pair<ArrayList<String>, ArrayList<String>> executeCommand(String key, boolean saveStdOut,
            boolean saveStdErr, String logPrefix) throws IOException, InterruptedException {
        ArrayList<String> cmdParams = new ArrayList<>();
        String[] split = StringUtils.split(key);
        for (String string : split) {
            cmdParams.add(replacePostActionVars(string));
        }
        logMessage(Level.INFO, new StringBuilder(logPrefix).append(" Executing [")
                .append(StringUtils.join(cmdParams, " ")).append("]").toString());
        // logger.trace("executing: " + rsyncParams);
        ExtProcess proc = extProcessManager.addProcess(new ExtProcessFinishing(cmdParams, false, true, logPrefix));
        proc.startProcess(saveStdOut, saveStdErr);
        int waitFor = proc.waitFor();
        logger.debug(logPrefix + " process terminated, result: " + waitFor);

        extProcessManager.doneProcess(proc);
        return (proc.getExitCode() != 255 && (saveStdOut || saveStdErr)) ? new Pair(proc.getSTDOut(), proc.getErrBuf())
                : null;

    }

    private Pair<ArrayList<String>, ArrayList<String>> executeCommand(String key)
            throws IOException, InterruptedException {
        return executeCommand(key, false, false);

    }

    private void executeSSHDownload(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
            ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) {
        // ssh -c "cd <dest>; find . -name <app_start> -print|xargs tar -cz" | tar zx
        // tar -C <dest directory> -cz file1,file2,,, | tar zx
        // https://mkyong.com/java/how-to-create-tar-gz-in-java/

        StringBuilder remoteCmd = new StringBuilder().append("tar -C ").append(appProfile.getLogDirectory())
                .append(" -cz ").append(StringUtils.join(fileNameClause, " "));
        try {
            ThreadedUnTarGZ stdoutReader = new ThreadedUnTarGZ(FilenameUtils.concat(ds.getOutputDir(), ap.getName()));
            stdoutReader.setProgressProc(new IProcessOutputRead() {
                @Override
                public void lineRead(String s) {
                    SettingsDialog.info(s);
                }
            });
            RemoteExecutionResult ret1 = sshClient.executePipedRemoteCommand(GetLogs.getSshUser(),
                    GetLogs.getSshPassword(), theAppHost.getHost(), 22, remoteCmd.toString(), stdoutReader);
        } catch (IOException e) {
            SettingsDialog.error("Exception while executing remote command: " + e.getMessage());
        }
        // executionResult = ret1.getRetCode();
        // stderr = ret1.getStderr();
        // stdout = ret1.getStdout();
    }

    private void executeWinDownload(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
            ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) {
        String outDir = FilenameUtils.concat(getDs().getOutputDir(), ap.getName());
        try {
            FileUtils.forceMkdir(new File(outDir));
        } catch (IOException e) {
            logMessage(Level.ERROR, "Not created output dir: " + e.getMessage());
        }
        for (String file : fileNameClause) {
            try {
                FileUtils.copyFileToDirectory(new File(file), new File(outDir), true);
            } catch (IOException e) {
                logMessage(Level.ERROR, "Failed to copy [" + file + "] to dir [" + outDir + "]: " + e.getMessage());
            }
        }
    }

    private void executeDownload(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
            ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        if (ap.isIsWindows()) {
            executeWinDownload(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        } else if (GetLogs.isbIsSSHJava()) {
            executeSSHDownload(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        } else {
            executeRSync(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        }
    }

    private StringBuilder makeRemoteDir(String logsDir, App ap, HostAppdir theAppHost, boolean lcaLog) {
        return new StringBuilder().append(logsDir).append("/")
                .append((lcaLog) ? "lca" : (theAppHost.getAppDir() != null ? theAppHost.getAppDir() : ap)).append("/");
    }

    /**
     * @param appProfile
     * @param ap
     * @param theAppHost
     * @param logsDir
     * @param fileNames  - expected to contain only list of file names, no path
     * @param isLFMT
     * @param lcaLog
     * @throws IOException
     * @throws InterruptedException
     */
    private void executeRSync(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
            ArrayList<String> fileNames, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {

        ArrayList<String> rsyncParams = new ArrayList<>();
        rsyncParams.add("rsync");

        String u = GetLogs.getsRSyncUserName();
        if (StringUtils.isBlank(u)) {
            u = GetLogs.getsUserName();
        }
        if (StringUtils.isNotBlank(u)) {
            rsyncParams.add("--rsync-path");
            rsyncParams.add("sudo -u " + u + " rsync");
        }

        rsyncParams.add("-avz");
        // rsyncParams.add("--compress-level=8");
        rsyncParams.add("-e");
        rsyncParams.add("ssh " + GetLogs.getSshOptions());
        // rsyncParams.add(GetLogs.getSshOptions());

        // rSyncFiles.addAll(rSyncAddClause(FilenameUtils.getName(row.getFileName())));
        // rsyncParams.addAll(fileNameClause);
        for (String file : fileNames) {
            rsyncParams.addAll(rSyncAddClause(FilenameUtils.getName(file)));
        }

        rsyncParams.add("-f");
        rsyncParams.add("- **");
        StringBuilder srcSpec = new StringBuilder();
        String lfmtHost = null;

        if (StringUtils.isNotBlank(u)) {
            srcSpec.append(u).append("@");
        }
        if (isLFMT) {
            DownloadSettings.LFMTHostInstance lfmtHostInstance = appProfile.getLFMT();
            if (lfmtHostInstance == null) {
                return;
            }
            srcSpec.append(lfmtHostInstance.getHost()).append(":")
                    .append(makeRemoteDir(logsDir, ap, theAppHost, lcaLog));
        } else {
            srcSpec.append(theAppHost.getHost()).append(":").append(makeRemoteDir(logsDir, ap, theAppHost, lcaLog));
        }

        rsyncParams.add(srcSpec.toString());

        StringBuilder dstSpec = new StringBuilder();
        dstSpec.append(osSpecificPath(ds.getOutputDir())).append("/");
        if (lcaLog) {
            dstSpec.append(theAppHost.getHost()).append("/").append("lca");
        } else {
            dstSpec.append(ap.getName());
        }
        Utils.FileUtils.mkDir(dstSpec.toString());

        rsyncParams.add(dstSpec.toString());
        // logger.trace("executing: " + rsyncParams);
        ExtProcessApp procRSync = (ExtProcessApp) extProcessManager
                .addProcess(new ExtProcessApp(appProfile, ap, rsyncParams, true, true));
        procRSync.startProcess();
        int waitFor = procRSync.waitFor();
        logger.debug("process terminated, result: " + waitFor);

        extProcessManager.doneProcess(procRSync);
    }

    private void executeGet(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
            ArrayList<String> fileNameClause, boolean useRSync1, boolean isLFMT, boolean lcaLog)
            throws IOException, InterruptedException {
        if (useRSync1) {
            executeDownload(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        } else {
            Utils.FileUtils.setCurrentDirectory(ds.getOutputDir());
            ArrayList<String> sshParams = new ArrayList<>();
            sshParams.add("ssh");
            if (GetLogs.sshOptions != null) {
                sshParams.addAll(Arrays.asList(StringUtils.split(GetLogs.sshOptions)));
            }

            if (isLFMT) {
                DownloadSettings.LFMTHostInstance lfmtHostInstance = appProfile.getLFMT();
                if (lfmtHostInstance == null) {
                    return;

                }
                sshParams.add(lfmtHostInstance.getHost());

            } else {
                sshParams.add(theAppHost.getHost());

            }

            StringBuilder fileClause = new StringBuilder();
            if (fileNameClause != null && fileNameClause.size() > 0) {
                fileClause.append("\\( -type f ");

                // fileClause.append("-a -name ")
                // .append(fileNameClause);
                fileClause.append("-a ").append(sshNameClause(fileNameClause));

                fileClause.append(" \\) ");

            }

            StringBuilder sshCmd = new StringBuilder();

            sshCmd.append("cd ").append(logsDir).append("; ");
            sshCmd.append("find ").append((lcaLog) ? "lca" : ap).append(" -maxdepth 1 ").append(fileClause);
            sshCmd.append(" -exec ");
            sshCmd.append("tar -");
            if (ds.isProd()) {
                sshCmd.append("z");
            }
            sshCmd.append("cvf - ").append("{} +");
            sshParams.add(sshCmd.toString());
            ExtProcess procSSH = extProcessManager.addProcess(new ExtProcessApp(sshParams, false, true));

            ExtProcess procTar;
            ArrayList<String> tarParams = new ArrayList<>();
            tarParams.add("tar");
            tarParams.add("-x");
            tarParams.add("-f");
            tarParams.add("-");

            procSSH.startProcess();
            procTar = extProcessManager.addProcess(new ExtProcessApp(tarParams, procSSH));
            procTar.startProcess();

            procSSH.waitFor();
            procTar.waitFor();
            extProcessManager.doneProcess(procSSH);
            extProcessManager.doneProcess(procTar);
        }

    }

    private String appPrefix(App ap, String suffix) {
        return ((StringUtils.isBlank(ap.getAppPrefix())) ? ap.getName() : ap.getAppPrefix())
                + (StringUtils.isNotBlank(suffix) ? suffix : "");
    }

    private String getGenesysNameClause(AppProfile appProfile, App ap, boolean lca, String suffix) {
        StringBuilder fileNameClause = new StringBuilder();
        String backSlash;
        if (!ds.isUseRSync()) {
            backSlash = "";
        } else {
            backSlash = "";
        }
        fileNameClause.append(backSlash).append("*");

        if (suffix != null) {
            fileNameClause.append(suffix);
        }

        fileNameClause.append(backSlash).append(".");
        if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX && ds.getDateSpec() != null
                && !ds.getDateSpec().isEmpty()) {
            fileNameClause.append(GetLogs.expandPattern(ds.getDateSpec(), 8));
        } else {
            fileNameClause.append(StringUtils.repeat("[0-9]", 8));
        }
        fileNameClause.append("_");

        if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX && ds.getTimeSpec() != null
                && !ds.getTimeSpec().isEmpty()) {
            fileNameClause.append(GetLogs.expandPattern(ds.getTimeSpec(), 6));
        } else {
            fileNameClause.append(StringUtils.repeat("[0-9]", 6));
        }
        fileNameClause.append("_");

        fileNameClause.append(StringUtils.repeat("[0-9]", 3)).append(backSlash).append(".").append(backSlash)
                .append("*");

        return fileNameClause.toString();
    }

    private ArrayList<String> getFileNameClause(AppProfile appProfile, App ap, boolean isLCA) {
        ArrayList<String> ret1 = new ArrayList<>();
        HashMap<String, Boolean> nameSuffixes = appProfile.getNameSuffixes();

        StringBuilder fileNameClause = new StringBuilder();

        if (!isLCA && !appProfile.isIsGenesysName()) {
            if (nameSuffixes != null && !nameSuffixes.isEmpty()) {
                for (Map.Entry<String, Boolean> entry : nameSuffixes.entrySet()) {
                    String suffix = entry.getKey();
                    Boolean isSelected = entry.getValue();
                    if (isSelected) {
                        if (suffix.trim().equals(".")) {
                            ret1.addAll(cloudStandardNames());
                        } else {
                            String datePattern = null;
                            String timePattern = null;
                            if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX) {
                                datePattern = ds.getDateSpec();
                                timePattern = ds.getTimeSpec();
                            }
                            ret1.add(cloudPattern(suffix, datePattern, timePattern, ap));
                        }
                    }
                }
            } else {
                ret1.addAll(cloudStandardNames());
            }

        } else {
            if (!isLCA && nameSuffixes != null && !nameSuffixes.isEmpty()) {
                for (Map.Entry<String, Boolean> entry : nameSuffixes.entrySet()) {
                    String suffix = entry.getKey();
                    Boolean isSelected = entry.getValue();
                    if (isSelected) {
                        ret1.add(getGenesysNameClause(appProfile, ap, isLCA,
                                appPrefix(ap, (suffix.trim().equals(".") ? null : suffix))));

                    }
                }
            } else {
                ret1.add(getGenesysNameClause(appProfile, ap, isLCA, appPrefix(ap, null)));
            }

        }
        GetLogs.logger.trace("fileName clause: [" + ret1 + "]");
        return ret1;
    }

    private ArrayList<JTableFileEntry> executeCmd(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
            ArrayList<String> fileNameClause, boolean isLFMT, boolean isLCALog) {
        try {
            switch (ds.getActionCommand()) {
                case GREP:
                    executeGrep(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    break;

                case GET:
                    HashMap<String, Boolean> suff = appProfile.getNameSuffixes();
                    if (!isLCALog && (suff == null || suff.isEmpty())
                            && ds.getTimeProfile() != SettingsPanel.TimeProfile.VALUE_FILES) {
                        executeGet(appProfile, ap, theAppHost, logsDir, fileNameClause, ds.isUseRSync(), isLFMT,
                                isLCALog);
                    } else {
                        /*
                         * this may be confusing and is not very good design. So if there are suffixes,
                         * we execute ls instead. Get will be called at very top level after application
                         * is processed. this way for suffixes we first get list of files and then
                         * execute rsync on files received. This may be ugly, but is effective
                         */
                        return executeLS(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    }
                    break;

                case LS:
                    return executeLS(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);

                case GREPGET:
                    executeGrepGet(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);

            }
        } catch (IOException e) {
            SettingsDialog.error(e + ": " + StringUtils.join(e.getStackTrace(), ";"));
        } catch (InterruptedException e) {
            // SettingsDialog.error(e.toString() + ": " +
            // StringUtils.join(e.getStackTrace(), ";"));
        }
        return null;
    }

    void setSettingsFile(String sGUIProfile) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change
        // body of generated methods, choose Tools | Templates.
    }

    void pasteFiles(java.awt.Window parent, DownloadSettings ds) {
        Clipboard clipboard = getSystemClipboard();
        String data = null;
        boolean errorReading = false;
        FilesToDownload ftd = new FilesToDownload();
        lsFilesAll.clear();
        ds.setActionCommand(GetCommand.LS);
        try {
            data = (String) clipboard.getData(DataFlavor.stringFlavor);
            // data = "# _sourcename _count \n"
            // + "1
            // /applog/gcti/esv1_sip_agent_1_p/esv1_sip_agent_1_p_sip-001.20200429_172154_086.log
            // 2\n"
            // + "2
            // /applog/gcti/esv1_sip_agent_1_p/esv1_sip_agent_1_p_sip-001.20200429_171214_695.log
            // 55\n"
            // + "3
            // /applog/gcti/edn1_sip_routing_1_p/edn1_sip_routing_1_p_sip-001.20200429_163320_805.log\n"
            // + "";
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(CommandExecutor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            errorReading = true;
        }
        if (errorReading) {
            JOptionPane.showMessageDialog(parent, "Error reading clipboard", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (data == null) {
            JOptionPane.showMessageDialog(parent, "Nothing in clipboard", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (String wrd : StringUtils.split(data)) {
            Matcher m;
            String app = null;
            String file = null;
            if ((m = ptFullFileName.matcher(wrd)).find()) {
                app = m.group(1);
                file = m.group(2);
            }
            if (file != null) {
                Pair<AppProfile, App> findAppProfile = ds.findAppProfile(app, file, wrd);
                if (findAppProfile != null) {
                    ftd.addDownloadFile(findAppProfile, file);
                }
            }
        }
        if (!ftd.isEmpty()) {
            if (lsGeneralTab == null) {
                lsGeneralTab = new JTablePasteFileList();
            }
            if (lsPasteOutput == null) {
                lsPasteOutput = new InfoPanel(parent, "List of pasted", lsGeneralTab, "Download %d files");
            }
            ArrayList<JTableFileEntryGeneral> lsPasteFiles = new ArrayList<>();
            HashSet<String> sortedFiles = new HashSet<>();
            for (FilesToGet filesToGet : ftd) {
                for (String fileName : filesToGet.getFileNames()) {
                    if (!sortedFiles.contains(fileName)) {
                        lsPasteFiles.add(new JTableFileEntryGeneral(filesToGet, fileName));
                        sortedFiles.add(fileName);
                    }
                }
            }
            lsGeneralTab.setFiles(lsPasteFiles);
            lsPasteOutput.doShow();
            if (lsPasteOutput.getCloseCause() == JOptionPane.OK_OPTION) {
                if (lsGeneralTab.getSelectedFiles().size() > 0) {

                    HashMap<FilesToGet, ArrayList<String>> r = new HashMap<>();

                    for (JTableFileEntryGeneral row : lsGeneralTab.getSelectedFiles()) {
                        FilesToGet fToGet = row.getFilesToGet();
                        ArrayList<String> rSyncFiles = r.get(fToGet);
                        if (rSyncFiles == null) {
                            rSyncFiles = new ArrayList<>();
                            r.put(fToGet, rSyncFiles);
                        }

                        rSyncFiles.add(row.getFileName() + "*");
                    }
                    SettingsDialog.info("About to download " + lsGeneralTab.getSelectedFiles().size() + " files ("
                            + r.size() + " threads)");

                    lsPastedFiles(r);

                    // CountDownLatch latch = new CountDownLatch(r.size());
                    //
                    // doExecuteCmd(parent, this,
                    // latch,
                    // new ISubTask() {
                    // @Override
                    // public void task() throws InterruptedException, IOException {
                    // executeRSyncFilesToGet(r, latch);
                    // }
                    // }
                    // );
                }
            }

        }
        logger.debug(ftd);
    }

    void showRecent() throws IOException, InterruptedException {
        if (lsFilesLast != null && !lsFilesLast.isEmpty()) {
            if (lsTab == null) {
                lsTab = new JTableFileList();
            }
            if (lsOutput == null) {
                lsOutput = new InfoPanel(parent, "List of files", lsTab, "Download %d files");
            }
            lsTab.setFiles(lsFilesLast);
            lsOutput.doShow();

            if (lsOutput.getCloseCause() == JOptionPane.OK_OPTION) {
                if (lsTab.getSelectedFiles().size() > 0) {

                    HashMap<SavedSearchStorage, ArrayList<String>> r = new HashMap<>();

                    for (JTableFileEntry row : lsTab.getSelectedFiles()) {
                        SavedSearchStorage storage = row.getStorage();
                        ArrayList<String> rSyncFiles = r.get(storage);
                        if (rSyncFiles == null) {
                            rSyncFiles = new ArrayList<>();
                            r.put(storage, rSyncFiles);
                        }

                        rSyncFiles.add(row.getFileName());
                    }
                    SettingsDialog.info("About to download " + lsTab.getSelectedFiles().size() + " files (" + r.size()
                            + " threads)");

                    CountDownLatch latch = new CountDownLatch(r.size());

                    doExecuteCmd(parent, this, latch, new ISubTask() {
                        @Override
                        public void task() throws InterruptedException, IOException {
                            executeRSync(r, latch);
                        }
                    });

                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "No files to display", "Info", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ArrayList<String> getActions(ArrayList<Pair<String, Boolean>> acts) {
        ArrayList<String> r = new ArrayList<>();
        if (acts != null && !acts.isEmpty()) {
            for (Pair<String, Boolean> entry : acts) {
                if (entry.getValue()) {
                    r.add(entry.getKey());
                }
            }
        }
        if (r.isEmpty()) {
            return null;
        } else {
            return r;
        }
    }

    private ArrayList<String> getBeforeActions() {
        return getActions(ds.getBeforeActions());

    }

    private ArrayList<String> getAfterActions() {
        return getActions(ds.getAfterActions());

    }

    private void executeRSync(HashMap<SavedSearchStorage, ArrayList<String>> r, CountDownLatch latch) {

        for (Map.Entry<SavedSearchStorage, ArrayList<String>> entry : r.entrySet()) {
            SavedSearchStorage key = entry.getKey();
            ArrayList<String> value = entry.getValue();
            executor.execute(new CallbackThreadLatched(latch, new ISubTask() {
                @Override
                public void task() throws InterruptedException, IOException {
                    executeDownload(key.getAppProfile(), key.getAp(), key.getAppHost(), key.getLogsDir(), value,
                            key.isLfmt(), key.isLcaLog());
                }
            }));

        }

    }

    private void executeRSyncFilesToGet(HashMap<FilesToGet, ArrayList<String>> r, CountDownLatch latch) {

        for (Map.Entry<FilesToGet, ArrayList<String>> entry : r.entrySet()) {
            FilesToGet key = entry.getKey();
            ArrayList<String> value1 = entry.getValue();
            ArrayList<String> value = new ArrayList<>();
            for (String string : value1) {
                value.add(string);

            }
            executor.execute(new CallbackThreadLatched(latch, new ISubTask() {
                @Override
                public void task() throws InterruptedException, IOException {
                    HostAppdir hh = GetLogs.getHosts().lookupHost(key.getApp().getName());

                    ArrayList<JTableFileEntry> lsFiles = executeLS(key.getProfile(), key.getApp(), hh,
                            getLogDir(key.getProfile(), key.getApp(), getDs().isLfmt(), hh.toString()), value,
                            getDs().isLfmt(), false);

                    if (lsFiles != null && !lsFiles.isEmpty()) {
                        synchronized (lsFilesAll) {
                            lsFilesAll.addAll(lsFiles);
                        }
                    }

                }
            }));

        }

    }

    private void executeRSync(ArrayList<JTableFileEntry> selectedRows) {
        if (selectedRows == null || selectedRows.isEmpty()) {
            SettingsDialog.info("No rows selected");
        } else {
            HashMap<SavedSearchStorage, ArrayList<String>> r = new HashMap<>();

            for (JTableFileEntry row : selectedRows) {
                SavedSearchStorage storage = row.getStorage();
                ArrayList<String> rSyncFiles = r.get(storage);
                if (rSyncFiles == null) {
                    rSyncFiles = new ArrayList<>();
                    r.put(storage, rSyncFiles);
                }

                rSyncFiles.add(row.getFileName());
            }
            if (r.size() > 0) {
                for (Map.Entry<SavedSearchStorage, ArrayList<String>> entry : r.entrySet()) {
                    SavedSearchStorage key = entry.getKey();
                    ArrayList<String> value = entry.getValue();
                    try {
                        executeDownload(key.getAppProfile(), key.getAp(), key.getAppHost(), key.getLogsDir(), value,
                                key.isLfmt(), key.isLcaLog());
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(CommandExecutor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);

                    }

                }
            }

        }
    }

    private ArrayList<JTableFileEntry> saveLS(ArrayList<JTableFileEntry> lsFiles) {
        if (lsFiles != null && !lsFiles.isEmpty()) {
            ArrayList<JTableFileEntry> ret1 = new ArrayList<>(lsFiles.size());
            for (JTableFileEntry lsFile : lsFiles) {
                ret1.add(lsFile);
            }
            return ret1;
        } else {
            return null;
        }
    }

    private String sshNameClause(ArrayList<String> fileNameClause) {
        if (fileNameClause != null && !fileNameClause.isEmpty()) {
            StringBuilder ret1 = new StringBuilder();
            if (fileNameClause.size() > 1) {
                ret1.append(("\\( "));
            }
            for (int i = 0; i < fileNameClause.size(); i++) {
                String s = fileNameClause.get(i);
                if (i > 0) {
                    ret1.append(" -o ");
                }
                ret1.append("-name ").append(s);

            }
            if (fileNameClause.size() > 1) {
                ret1.append((" \\)"));
            }
            return ret1.toString();
        } else {
            return null;
        }

    }

    private void logMessage(String str, Exception e, AppProfile appProfile, App ap) {
        logMessage(getLogPrefix(appProfile, ap) + str, e);
    }

    private void logMessage(String str, Exception e) {
        logMessage(Level.ERROR, (new StringBuilder(str)).append(", Exception: ")
                .append(StringUtils.join(e.getStackTrace(), "\n")).toString());
    }

    private void logMessage(String str, AppProfile appProfile, App ap) {
        logMessage(Level.INFO, getLogPrefix(appProfile, ap) + str);
    }

    private void logMessage(Level lvl, String str, AppProfile appProfile, App ap) {
        logMessage(lvl, getLogPrefix(appProfile, ap) + str);
    }

    private void logMessage(Level lvl, String str) {
        if (lvl == Level.INFO) {
            SettingsDialog.info(str);
        } else if (lvl == Level.ERROR) {
            SettingsDialog.error(str);
        }
    }

    private ArrayList<String> cloudStandardNames() {
        StringBuilder fileNameClause = new StringBuilder();
        if (!ds.isUseRSync()) {
            String backSlash = "\\";
            fileNameClause.append(backSlash).append("*").append(backSlash).append(".");
        } else {
            fileNameClause.append("*cloud*").append("-");
        }
        fileNameClause.append(GetLogs.cloudDatePattern(ds.getDateSpec(), ds.getTimeSpec(), ds.getTimeProfile()));
        ArrayList<String> ret1 = new ArrayList<>();
        ret1.add(fileNameClause.toString());
        ret1.add("*cloud.log*");
        return ret1;
    }

    private void cancel() {
        extProcessManager.cancelAll();

    }

    private void doExecuteCmd(java.awt.Window parent1, CommandExecutor aThis) {
        QueryTaskBase tsk;

        tsk = new QueryThreadingTask(aThis, new IThreadingSubTask() {
            @Override
            public ArrayList<ISubTask> task() throws InterruptedException, IOException {
                lsFilesAll.clear();
                if (!isText) {
                    if (lsTab == null) {
                        lsTab = new JTableFileList();
                    }
                    lsTab.clearTable();

                }

                ArrayList<ISubTask> ret1 = new ArrayList<>();
                for (AppProfile appProfile : ds.getAppProfiles()) {
                    if (appProfile.isSelected()) {
                        GetLogs.logger.debug("processing command for profile " + appProfile);
                        for (App app : appProfile.getApps()) {
                            GetLogs.logger.debug("processing app  " + app + ": " + app.isChecked());
                            if (app.isChecked()) {
                                if (ds.isProd()) {
                                    ret1.add(new ISubTask() {
                                        @Override
                                        public void task() throws InterruptedException, IOException {
                                            executeCmd(appProfile, app, false);
                                        }
                                    });
                                }
                                if (ds.isLfmt()) {
                                    ret1.add(new ISubTask() {
                                        @Override
                                        public void task() throws InterruptedException, IOException {
                                            executeCmd(appProfile, app, true);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
                return ret1;
            }
        });
        if (rp == null) {
            rp = new RequestProgress(parent1, false, tsk);
        }
        tsk.setRp(rp);
        if (ds.getActionCommand() == GetCommand.GET || ds.getActionCommand() == GetCommand.GREPGET) {
            tsk.setAfterActions(getAfterActions());

            tsk.setBeforeActions(getBeforeActions());

        }
        tsk.execute();
    }

    private void doExecuteCmd(Window parent1, CommandExecutor aThis, CountDownLatch latch, ISubTask subTask) {

        QueryTaskBase tsk = new QueryTask(aThis, latch, subTask);
        if (rp == null) {
            rp = new RequestProgress(parent1, true, tsk);
        }

        tsk.setRp(rp);
        tsk.setBeforeActions(getBeforeActions());
        tsk.setAfterActions(getAfterActions());

        tsk.execute();

    }

    private class ExtProcessManager {

        HashSet<ExtProcess> processes = new HashSet<>(2);

        private ExtProcess addProcess(ExtProcess extProcess) {
            synchronized (this) {
                processes.add(extProcess);
                return extProcess;
            }
        }

        private void doneProcess(ExtProcess procSSH) {
            synchronized (this) {
                processes.remove(procSSH);
            }
        }

        private void cancelAll() {
            synchronized (this) {
                for (ExtProcess processe : processes) {
                    processe.cancel();
                }
            }

        }

    }

    class ExtProcessApp extends ExtProcess {

        private AppProfile profile = null;
        private App app = null;
        private String msgPrefix = null;

        private ExtProcessApp(AppProfile appProfile, App ap, ArrayList<String> sshParams, boolean b, boolean b0)
                throws IOException {
            this(sshParams, b, b0);
            this.profile = appProfile;
            this.app = ap;
        }

        public ExtProcessApp(ArrayList<String> tarParams, ExtProcess procSSH) throws IOException {
            super(tarParams, procSSH);
            initLogBack(false, false);
        }

        public ExtProcessApp(List<String> tarParams, boolean logStdin, boolean logStdout) throws IOException {
            super(tarParams);
            initLogBack(logStdin, logStdout);
        }

        private String genLogMsg(String s, boolean isErr) {
            if (msgPrefix == null) {
                StringBuilder msg = new StringBuilder();
                if (profile != null) {
                    msg.append("[").append(profile).append("]");
                }
                if (app != null) {

                    msg.append("/[").append(app.getName()).append("(")
                            .append(GetLogs.getHosts().lookupHost(app.getName())).append(")] - ");

                }
                msgPrefix = msg.toString();
            }
            StringBuilder ret = new StringBuilder(msgPrefix);
            if (isErr) {
                ret.append(" !ERR! ");
            }
            ret.append(s);
            return ret.toString();

        }

        private void initLogBack(boolean logStdin, boolean logStdout) {
            if (logStdin) {
                setStdinReadProc(new IProcessOutputRead() {
                    @Override
                    public void lineRead(String s) {
                        logMessage(Level.INFO, genLogMsg(s, false));
                    }
                });
            }

            if (logStdout) {
                setStderrReadProc(new IProcessOutputRead() {
                    @Override
                    public void lineRead(String s) {

                        logMessage(Level.ERROR, genLogMsg(s, true));

                    }

                });
            }
        }
    }

    class ExtProcessFinishing extends ExtProcess {

        private String logPrefix = "";

        public ExtProcessFinishing(ArrayList<String> tarParams, ExtProcess procSSH) throws IOException {
            super(tarParams, procSSH);
            initLogBack(false, false);
        }

        public ExtProcessFinishing(List<String> tarParams, boolean logStdin, boolean logStdout) throws IOException {
            this(tarParams, logStdin, logStdout, "");
        }

        public ExtProcessFinishing(List<String> tarParams, boolean logStdin, boolean logStdout, String logPrefix)
                throws IOException {
            super(tarParams);
            initLogBack(logStdin, logStdout);
            this.logPrefix = logPrefix;
        }

        private void initLogBack(boolean logStdin, boolean logStdout) {
            if (logStdin) {
                setStdinReadProc(new IProcessOutputRead() {
                    @Override
                    public void lineRead(String s) {
                        logMessage(Level.INFO, logPrefix + s);
                    }
                });
            }

            if (logStdout) {
                setStderrReadProc(new IProcessOutputRead() {
                    @Override
                    public void lineRead(String s) {
                        StringBuilder msg = new StringBuilder(logPrefix);
                        msg.append("! ").append(s);
                        logMessage(Level.ERROR, msg.toString());

                    }
                });
            }
        }
    }

    private class FilesToDownload extends ArrayList<FilesToGet> {

        public FilesToDownload() {
            super();
        }

        private void addDownloadFile(Pair<AppProfile, App> findAppProfile, String file) {
            for (FilesToGet ftg : this) {
                if (ftg.getProfile().equals(findAppProfile.getKey())
                        && ftg.getApp().equals(findAppProfile.getValue())) {
                    ftg.addFile(file);
                    return;
                }
            }
            add(new FilesToGet(findAppProfile.getKey(), findAppProfile.getValue(), file));
        }

    }

    public final class QueryTask extends QueryTaskBase {

        private final CountDownLatch latch;
        private final ISubTask subTask;

        private QueryTask(CommandExecutor aThis, CountDownLatch latch, ISubTask subTask) {
            super(aThis);
            this.latch = latch;
            this.subTask = subTask;
        }

        private QueryTask(CommandExecutor aThis, CountDownLatch latch, ISubTask subTask, CountDownLatch finishLatch,
                ISubTask finishingTask) {
            this(aThis, latch, subTask);
            if (finishingTask != null) {
                setFinishingTask(finishingTask);
            }
            if (finishLatch != null) {
                setFinishLatch(finishLatch);
            }
        }

        @Override
        void onBackground() throws InterruptedException, IOException {
            subTask.task();
            if (latch != null) {
                latch.await();
            }

        }

        @Override
        void onDone() {

        }

        @Override
        void onCancel() {
            cancelExecutor();
        }

    }

    public class QueryThreadingTask extends QueryTaskBase {

        private final IThreadingSubTask threadingSubTask;
        CountDownLatch latch = null;

        public QueryThreadingTask(CommandExecutor ce, IThreadingSubTask threadingSubTask) {
            super(ce);
            this.threadingSubTask = threadingSubTask;
        }

        // boolean myCancel(boolean mayInterruptIfRunning) {
        // ce.cancel();
        // cancel(mayInterruptIfRunning);
        // if (isDone()) {
        // return true;
        // }
        //
        // try {
        // Thread.sleep(150);
        //
        // /*
        // may consider implementing this
        //
        // from
        // https://stackoverflow.com/questions/671049/how-do-you-kill-a-thread-in-java
        //
        // Thread f = <A thread to be stopped>
        // Method m = Thread.class.getDeclaredMethod( "stop0" , new
        // Class[]{Object.class} );
        // m.setAccessible( true );
        // m.invoke( f , new ThreadDeath() );
        //
        // */
        // if (!isDone()) {
        // SettingsDialog.info("Thread not done; killing");
        // Thread.currentThread().stop();
        // }
        //
        // } catch (InterruptedException ex) {
        // logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        // }
        // return true;
        // }
        @Override
        void onDone() {
            if (ds.getActionCommand() == GetCommand.LS || ds.getActionCommand() == GetCommand.GREP) {
                lsFilesLast = saveLS(lsFilesAll);
                try {
                    showRecent();
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(CommandExecutor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
            }
        }

        @Override
        void onCancel() {
            cancelExecutor();
        }

        @Override
        void onBackground() throws InterruptedException, IOException {
            if (threadingSubTask != null) {
                ArrayList<ISubTask> tasks = threadingSubTask.task();
                if (tasks != null && !tasks.isEmpty()) {
                    latch = new CountDownLatch(tasks.size());
                    for (ISubTask task : tasks) {
                        try {
                            executor.execute(new CallbackThreadLatched(latch, task));
                        } catch (Exception e) {
                            logger.error("Not possible to submit thread", e);
                        }
                    }
                    latch.await();
                } else {
                    logger.error("nothing to do");
                }

            }
        }

    }

    public abstract class QueryTaskBase extends SwingWorker<Void, String> {

        private final CommandExecutor ce;

        boolean userCancelling = false;
        private String outFile;
        private boolean displayForm;
        private RequestProgress rp = null;
        String theTitle = "";
        private ISubTask finishingTask = null;
        private CountDownLatch finishLatch = null;
        private ISubTask startingTask = null;
        private CountDownLatch startingLatch = null;

        private QueryTaskBase(CommandExecutor aThis) {
            super();
            ce = aThis;
        }

        public boolean isUserCancelling() {
            return userCancelling || isCancelled();
        }

        public void setUserCancelling(boolean userCancelling) {
            this.userCancelling = userCancelling;
        }

        abstract void onBackground() throws InterruptedException, IOException;

        abstract void onDone();

        abstract void onCancel();

        boolean myCancel(boolean mayInterruptIfRunning) {
            try {
                setUserCancelling(true);
                onCancel();
                ce.cancel();
                cancel(mayInterruptIfRunning);

                try {
                    Thread.sleep(150);

                    /*
                     * may consider implementing this
                     * 
                     * from
                     * https://stackoverflow.com/questions/671049/how-do-you-kill-a-thread-in-java
                     * 
                     * Thread f = <A thread to be stopped> Method m =
                     * Thread.class.getDeclaredMethod( "stop0" , new Class[]{Object.class} );
                     * m.setAccessible( true ); m.invoke( f , new ThreadDeath() );
                     * 
                     */
                    if (!isDone()) {
                        SettingsDialog.info("Thread not done; killing");
                        Thread.currentThread().interrupt();
                    }

                } catch (InterruptedException ex) {
                    logger.log(org.apache.logging.log4j.Level.FATAL, ex);
                }
            } finally {
                if (rp != null) {
                    rp.dispose();
                }
            }
            return true;
        }

        public void setDisplayForm(boolean displayForm) {
            this.displayForm = displayForm;
        }

        @Override
        protected void process(List<String> chunks) {
            rp.addProgress(chunks);
        }

        public void setRp(RequestProgress rp) {
            this.rp = rp;
        }

        public ISubTask getStartingTask() {
            return startingTask;
        }

        public void setStartingTask(ISubTask startingTask) {
            this.startingTask = startingTask;
        }

        public CountDownLatch getStartingLatch() {
            return startingLatch;
        }

        public void setStartingLatch(CountDownLatch startingLatch) {
            this.startingLatch = startingLatch;
        }

        public ISubTask getFinishingTask() {
            return finishingTask;
        }

        public void setFinishingTask(ISubTask finishingTask) {
            this.finishingTask = finishingTask;
        }

        public CountDownLatch getFinishLatch() {
            return finishLatch;
        }

        public void setFinishLatch(CountDownLatch finishLatch) {
            this.finishLatch = finishLatch;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                if (rp != null) {
                    rp.doShow();
                }
                if (isUserCancelling()) {
                    return null;
                }
                if (startingTask != null) {
                    logger.info("Starting predownload task(s)");
                    startingTask.task();
                    if (startingLatch != null) {
                        startingLatch.await();
                    }
                }

                if (isUserCancelling()) {
                    return null;
                }
                onBackground();

                if (isUserCancelling()) {
                    return null;
                }
                if (finishingTask != null) {
                    logger.info("Starting postdownload task(s)");
                    finishingTask.task();
                    if (finishLatch != null) {
                        finishLatch.await();
                    }
                }

            } finally {

            }
            return null;
        }

        @Override
        protected void done() {
            logger.debug("swingworker done");
            if (rp != null) {
                rp.dispose();
            }
            if (isCancelled()) {
                // JOptionPane.showMessageDialog(null, "Query was cancelled", "Error",
                // JOptionPane.ERROR_MESSAGE);
                logMessage(Level.INFO, "Action was cancelled");
            } else {
                SettingsDialog.info("Command executed");
                onDone();
            }
        }

        private void setOutFile(String outFile) {
            this.outFile = outFile;
        }

        private void setBeforeActions(ArrayList<String> beforeActions) {
            if (beforeActions != null) {
                CountDownLatch startingLatch = new CountDownLatch(1);
                setStartingLatch(startingLatch);
                setStartingTask(new ISubTask() {
                    @Override
                    public void task() throws InterruptedException, IOException {

                        executor.execute(new CallbackThreadLatched(startingLatch, new ISubTask() {
                            @Override
                            public void task() throws InterruptedException, IOException {
                                for (String beforeAction : beforeActions) {
                                    executeCommand(beforeAction);
                                }

                                startingLatch.countDown();
                            }

                        }));

                    }
                });
            }
        }

        private void setAfterActions(ArrayList<String> afterActions) {
            if (afterActions != null) {
                CountDownLatch finalLatch = new CountDownLatch(1);
                setFinishLatch(finalLatch);
                setFinishingTask(new ISubTask() {
                    @Override
                    public void task() throws InterruptedException, IOException {
                        executor.execute(new CallbackThreadLatched(finalLatch, new ISubTask() {
                            @Override
                            public void task() throws InterruptedException, IOException {
                                for (String afterAction : afterActions) {
                                    executeCommand(afterAction);
                                }

                                finalLatch.countDown();
                            }

                        }));
                    }
                });
            }
        }

    }

    class CallbackThreadLatched implements Runnable {

        private final ISubTask task;
        private CountDownLatch latch;

        public CallbackThreadLatched(ISubTask tsk) {
            this.task = tsk;
        }

        private CallbackThreadLatched(CountDownLatch _latch, ISubTask iSubTask) {
            this(iSubTask);
            this.latch = _latch;
        }

        @Override
        public void run() {
            try {
                Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        logger.error("Uncought exception in thread " + t.getName(), e);
                        latch.countDown();
                    }
                });
                // logger.debug("Thread " + Thread.currentThread() + " starting task");
                try {
                    task.task();
                } catch (InterruptedException interruptedException) {
                    logMessage(Level.INFO, "Action interrupted");
                }
                // logger.debug("Thread " + Thread.currentThread() + " done task");

            } catch (IOException ex) {
                Logger.getLogger(CommandExecutor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            } finally {
                latch.countDown();
            }
        }

    }

}
