/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.Pair;
import Utils.UTCTimeRange;
import Utils.UnixProcess.*;
import com.jidesoft.dialog.StandardDialog;
import com.myutils.logbrowser.common.ExecutionEnvironment;
import com.myutils.logbrowser.indexer.Main;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.mina.util.Base64;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.SystemClipboard.getSystemClipboard;
import static Utils.Util.rSyncAddClause;
import static com.myutils.getlogs.GetLogs.logger;

/**
 * @author stepan_sydoruk
 */
public final class CommandExecutor {

    private static final Pattern regVariable = Pattern.compile("\\{([A-Z]{2,4}|NAME)\\}");
    private static final Pattern regPostAction = Pattern.compile("\\{(NAME|OUTDIR)\\}");
    private static final Pattern ptFullFileName = Pattern.compile("([^/]+)/([^/]+)$");
    private static final Matcher mWindowsErrorCode = Pattern.compile("error (\\d+) has occurred").matcher("");
    private static final int MAX_FILE_LIST_LEN = 80;
    private static final String zipExt = ".zip";
    private static final Matcher mFileName = Pattern.compile("(\\w)(?:\\$|:)(.+)$").matcher("");
    private static int MAX_FILES_IN_BANCH = 5;
    final ArrayList<SavedSearchStorage> savedSearch = new ArrayList<>();
    private final boolean isText;
    final private ArrayList<JTableFileEntry> lsFilesAll = new ArrayList<>();
    private final ExtProcessManager extProcessManager;
    private final int ret = StandardDialog.RESULT_CANCELLED;
    Main indexer;
    InfoPanel lsOutput;
    InfoPanel lsPasteOutput;
    JTableFileList lsTab = new JTableFileList();
    JTablePasteFileList lsGeneralTab = null;
    SSHClientWrapper sshClient = null;
    private ThreadPoolExecutor executor = null;
    private ThreadPoolExecutor helperExecutor = null;
    private DownloadSettings ds;
    private Window parent;
    private ArrayList<JTableFileEntry> lsFilesLast = new ArrayList<>();
    private RequestProgress rp = null;

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
        setDs(ds);
    }

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
                    SettingsForm.error("Incorrect specification [" + m.group(1) + "] " + "in pattern ["
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

    private void initExecutor(int maxThreads) {
        if (executor != null)
            executor.purge();
        if (helperExecutor != null)
            helperExecutor.purge();

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
        executor.setCorePoolSize(maxThreads);
        executor.setMaximumPoolSize(maxThreads);
        helperExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    synchronized private void cancelExecutor() {
        logger.debug("Cancelling...");
        boolean terminatedOK = true;
        shutdownExecutor(helperExecutor, "helper executor");
        shutdownExecutor(executor, "main executor");

        initExecutor(ds.getMaxThreads());
    }

    private void shutdownExecutor(ThreadPoolExecutor _executor, String helper_executor) {
        boolean terminatedOK = true;

        List<Runnable> shutdownNow = _executor.shutdownNow();
        if (shutdownNow != null && !shutdownNow.isEmpty()) {
            try {
                if (!_executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    terminatedOK = false;
                    logger.error("Not all thread terminated after timeout");
                }
            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
        if (terminatedOK) {
            _executor.purge();
        }
        if (_executor.isTerminated()) {
            logger.error(helper_executor + " terminated");
        }

    }

    public DownloadSettings getDs() {
        return ds;
    }

    public void setDs(DownloadSettings ds) {
        this.ds = ds;
        initExecutor(ds.getMaxThreads());
    }

    public void executeCmd(java.awt.Window parent) throws IOException, InterruptedException {
        if (!ds.isAppLogs() && !ds.isLcaLogs()) {
            JOptionPane.showMessageDialog(parent, "Either Application or LCA checkbox needs to be checked",
                    "Cannot continue", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SettingsForm.info("* " + ds.getActionCommand() + " started");
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

    protected void executeCmd(AppProfile appProfile, App ap, boolean isLFMT) throws Exception {
        logMessage("executing command lfmt: " + isLFMT, appProfile, ap);
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
            ArrayList<JTableFileEntry> lsFiles
                    = null;
            lsFiles = executeLS(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, false);
            if (lsFiles != null && !lsFiles.isEmpty()) {
                synchronized (lsFilesAll) {
                    lsFilesAll.addAll(lsFiles);
                }
                if (ds.getActionCommand() == GetCommand.GET) {
                    executeDownload(lsFiles);
                }
            }

        }
        if (ds.isLcaLogs()) {
            ArrayList<String> fileNameClause = getFileNameClause(appProfile, ap, true);
            ArrayList<JTableFileEntry> lsFiles = executeLS(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, true);
            if (lsFiles != null && !lsFiles.isEmpty()) {
                synchronized (lsFilesAll) {
                    lsFilesAll.addAll(lsFiles);
                }
                if (ds.getActionCommand() == GetCommand.GET) {
                    executeDownload(lsFiles);
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
        sshCmd.append(" find ").append((lcaLog) ? "lca" : ap.getAppDir()).append(" -maxdepth 5 ")
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
                                                 ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException, ConfigException {
//        prepareZips(appProfile, ap, theAppHost, logsDir, null);

        if (ap.isIsWindows()) {
            return executeLSWin(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        } else {
            return executeLSLinux(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        }

    }

    private synchronized String connectWindowsShare(AppProfile appProfile, App ap) throws IncorrectPasswordException {
        Pair<String, String> winPath = getWinDrive(ap.getAppDir());
        String logPath = "\\\\" + ap.getHost() + "\\" + winPath.getKey();
        String ret = logPath + winPath.getValue();
        if (shareConnected(logPath, appProfile, ap)) {
            return ret;
        }
        try {
            StringBuilder cmd = new StringBuilder("net use ");
            cmd.append(logPath).append(" /user:").append(ds.getUser(appProfile)).append(" ").append(ds.getPassword(appProfile));
            Pair<ArrayList<String>, ArrayList<String>> ret1 = null;
            ret1 = executeCommand(cmd.toString(), true, true, appProfile, ap);
            if (ret1 != null) { // success
                ArrayList<String> stdOut = ret1.getKey();
                if (!stdOut.isEmpty() && stdOut.size() > 1
                        && stdOut.get(0).equals("The command completed successfully.")) {
                    return ret;
                }
                for (String s : ret1.getValue()
                ) {
                    if (mWindowsErrorCode.reset(s).find()) {
                        int code = Integer.parseInt(mWindowsErrorCode.group(1));
                        if (code == 86 ||
                                code == 1909 //! The referenced account is currently locked out and may not be logged on to.
                        ) { //
                            throw new IncorrectPasswordException("");
                        }
                    }
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
        } catch (ConfigException e) {
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
            ret1 = executeCommand("net use " + logPathLower, true, true, appProfile, ap);
            if (ret1 != null) { // success
                for (String s : ret1.getKey()) {
                    String[] s1 = StringUtils.split(s, " ", 2);
                    if (s1.length >= 2 && s1[0].toLowerCase().equals("status")) {
                        if (s1[1].equals("OK")) {
                            return true;
                        } else if (s1[1].equals("Disconnected")) {
                            disconnectShare(logPathLower, appProfile, ap);
                            return false;
                        }
                    }
                }
            }

        } catch (IOException e) {
            logMessage("Not able to check connected share", e, appProfile, ap);
        } catch (InterruptedException e) {
            logMessage("Not able to check connected share", e, appProfile, ap);
        }
        return false;
    }

    private boolean disconnectShare(String logPath, AppProfile appProfile, App ap) {
        Pair<ArrayList<String>, ArrayList<String>> ret1 = null;
        try {
            ret1 = executeCommand("net use " + logPath + " /delete",
                    true, true, appProfile, ap);
            if (ret1 != null) { // success
                ArrayList<String> stdOut = ret1.getKey();
                if (!stdOut.isEmpty() && stdOut.size() > 1
                        && stdOut.get(0).equals("The command completed successfully.")) {
                    return true;
                }
                logMessage(Level.ERROR,
                        (new StringBuilder("Failed to disconnect share ["))
                                .append(logPath)
                                .append("]")
                                .append("\n\tstdout [")
                                .append(StringUtils.join(stdOut, "\n")).append("]\n\t").append("\n\tstderr [")
                                .append(StringUtils.join(ret1.getValue(), "\n")).append("]\n\t").toString(),
                        appProfile, ap);

            }

        } catch (IOException e) {
            logMessage("Not able to disconnected share", e, appProfile, ap);
        } catch (InterruptedException e) {
            logMessage("Not able to disconnected share", e, appProfile, ap);
        }
        return false;
    }

    /**
     * Checks if first two letters of 'logDir' is windows drive if so, returns
     * By default returns d$
     *
     * @param logDir
     * @return Pair of [1st letter to lower case with $ attached], [Path without
     * dir]
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

        String logPath = null;
        for (int i = 0; i < 3; i++) {
            logMessage("attempt to connect to share " + i, appProfile, ap);
            try {
                logPath = connectWindowsShare(appProfile, ap);
            } catch (IncorrectPasswordException e) {
                logMessage("The specified network password is not correct.", appProfile, ap);
                break;
            }
            if (logPath != null) {
                break;
            }
            Thread.sleep(5000);
        }
        if (logPath != null) { // success

            HashMap<String, ArrayList<OSFile>> nameSuffixes = new HashMap<>();
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
            if (nameSuffixes.isEmpty()) {
                return null;
            }

            Matcher rxDateTime = null;
            if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX) {
                rxDateTime = Pattern.compile(getFileRegexMatch(ds.getDateSpec(), ds.getTimeSpec())).matcher("");
            }
            for (File f : FileUtils.listFiles(new File(logPath), null, true)) {
                String fileName = f.getName().toLowerCase();
                if (!fileName.contains("snapshot.log") && !fileName.endsWith(".zip")) {
                    for (Map.Entry<String, ArrayList<OSFile>> entryNameSuffix : nameSuffixes
                            .entrySet()) {

                        if (entryNameSuffix.getKey().isEmpty()
                                || (entryNameSuffix.getKey().equals(".")
                                || fileName.contains(ap.getName().toLowerCase()))
                                || (fileName.contains(entryNameSuffix.getKey().toLowerCase()))) {
                            if (rxDateTime == null || rxDateTime.reset(fileName).find()) {
                                BasicFileAttributes basicFileAttributes = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                                entryNameSuffix.getValue().add(
                                        new OSFile(f.getAbsoluteFile().getAbsolutePath(), basicFileAttributes.size(),
                                                basicFileAttributes.creationTime()));

                            }
                            break;
                        }
                    }
                }
            }
            for (ArrayList<OSFile> files : nameSuffixes.values()) {
                files.sort(new Comparator<OSFile>() {
                    @Override
                    public int compare(OSFile o1, OSFile o2) {
                        return o2.getCreationTime().compareTo(o1.getCreationTime());
                    }
                });
            }

            ArrayList<JTableFileEntry> lsFiles = new ArrayList<JTableFileEntry>();
            if (ds.getTimeProfile() == SettingsPanel.TimeProfile.VALUE_FILES) {
                int cnt = 0;
                int max = Integer.parseInt(ds.getHours());
                if (max <= 0) {
                    max = 99999999;
                }
                for (ArrayList<OSFile> files : nameSuffixes.values()) {
                    for (OSFile f : files) {
                        if (cnt++ >= max) {
                            break;
                        }
                        lsFiles.add(new JTableFileEntry(appProfile,
                                getStorage(appProfile, ap, theAppHost, logsDir, isLFMT, lcaLog), f));
                    }
                }
            } else if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX) {
                for (ArrayList<OSFile> files : nameSuffixes.values()) {
                    for (OSFile f : files) {
                        lsFiles.add(new JTableFileEntry(appProfile,
                                getStorage(appProfile, ap, theAppHost, logsDir, isLFMT, lcaLog), f));
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
                                                      String logsDir, ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) throws ConfigException, IOException, InterruptedException {

        String remoteCmd = remoteSSHCmd(appProfile, ap, logsDir, fileNameClause, lcaLog, isLFMT);
        int executionResult;
        List<String> stdout;
        List<String> stderr;

        if (GetLogs.isbIsSSHJava()) {
            RemoteExecutionResult ret1 = sshClient.executeRemoteCommand(ds.getUser(appProfile), ds.getPassword(appProfile),
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
            logMessage("LS failed, error code: " + executionResult, appProfile, ap);
            if (stderr != null && !stderr.isEmpty()) {
                logMessage(Level.ERROR, StringUtils.join(stderr, " | "), appProfile, ap);
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
                    long fileSize = Long.parseLong(split[1]);
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
                                    getStorage(appProfile, ap, theAppHost, logsDir, isLFMT, lcaLog), new OSFile(fileName, fileSize)));
                        }

                    } else {
                        lsFiles.add(new JTableFileEntry(appProfile,
                                getStorage(appProfile, ap, theAppHost, logsDir, isLFMT, lcaLog), new OSFile(fileName, fileSize)));
                    }
                }
                logMessage("ls successful " + ((isLFMT) ? " on LFMT" : "") + " : got " + lsFiles.size() + " files",
                        appProfile, ap);
                if (stderr != null && !stderr.isEmpty()) {
                    logMessage(Level.ERROR, StringUtils.join(stderr, " | "), appProfile, ap);
                    // lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap,
                    // theAppHost, null, logsDir, isLFMT, lcaLog), StringUtils.join(errBuf, " |
                    // ")));
                    // lsTab.addRow(appProfile, ap, theAppHost, null, logsDir.toString(), isLFMT,
                    // lcaLog, StringUtils.join(errBuf, " | "));

                }

                ((AbstractTableModel) lsTab.getModel()).fireTableDataChanged();
                return lsFiles;
            } else {
                SettingsForm.error("LS failed, error code: " + executionResult);
                if (stderr != null && !stderr.isEmpty()) {
                    logMessage(Level.ERROR, StringUtils.join(stderr, " | "), appProfile, ap);
                }
                String s = (stderr != null && stderr.size() > 0) ? "\n" + StringUtils.join(stderr, "\n") : "<Empty>";
                logMessage(Level.ERROR, "Command successful but stdout is empty. Stderr: " + s, appProfile, ap);
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
                        StringUtils.join(new String[]{ds.getStatusScript(), StringUtils.join(appNames, " ")}, " "),
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

                                ArrayList<JTableFileEntry> lsFiles = null;
                                try {
                                    lsFiles = executeLS(key.getProfile(), key.getApp(), hh,
                                            getLogDir(key.getProfile(), key.getApp(), true, hh.toString()), value, true,
                                            false);
                                    if (lsFiles != null && !lsFiles.isEmpty()) {
                                        synchronized (lsFilesAll) {
                                            lsFilesAll.addAll(lsFiles);
                                        }
                                    }
                                } catch (ConfigException e) {
                                    logMessage("Config error", e, key.getProfile(), key.getApp());
                                }
                            }
                        });
                    }
                    if (getDs().isProd()) {
                        ret1.add(new ISubTask() {
                            @Override
                            public void task() throws InterruptedException, IOException {
                                HostAppdir hh = GetLogs.getHosts().lookupHost(key.getApp().getName());
                                try {

                                    ArrayList<JTableFileEntry> lsFiles = executeLS(key.getProfile(), key.getApp(), hh,
                                            getLogDir(key.getProfile(), key.getApp(), false, hh.toString()), value, false,
                                            false);
                                    if (lsFiles != null && !lsFiles.isEmpty()) {
                                        synchronized (lsFilesAll) {
                                            lsFilesAll.addAll(lsFiles);
                                        }
                                    }
                                } catch (ConfigException e) {
                                    logMessage("Config error", e, key.getProfile(), key.getApp());
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

    private String replacePostActionVars(String word) {
        int pos = 0;
        Matcher m;
        StringBuilder _ret = new StringBuilder();
        while ((m = regPostAction.matcher(word)).find(pos)) {
            _ret.append(word, pos, m.start());
            if (m.group(1).equals("OUTDIR")) {
                _ret.append(osSpecificPath(ds.getOutputDir()));

            } else {
                SettingsForm.error("Incorrect specification [" + m.group(1) + "] " + "in pattern [" + word
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
        logMessage(Level.INFO, logPrefix + " Executing ["
                + StringUtils.join(cmdParams, " ") + "]");
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
                                    ArrayList<OSFile> filesList, boolean isLFMT, boolean lcaLog) throws InterruptedException {
        ArrayList<OSFile> filesToGet = new ArrayList<>();
        String outDir = FilenameUtils.concat(getDs().getOutputDir(), ap.getName());
        for (OSFile file
                : filesList) {
            fileTransfer(file, outDir, appProfile, ap, (f, src, dst) -> {
                filesToGet.add(f);
            });
        }
        if (filesToGet.isEmpty()) {
            logMessage("File list empty; nothing to do", appProfile, ap);
            return;
        }
        StringBuilder fileListForTar = new StringBuilder();
        StringBuilder fileListForInfo = new StringBuilder();

        for (OSFile f
                : filesToGet) {
            if (fileListForTar.length() > 0) {
                fileListForTar.append(" ");
            }
            if (fileListForInfo.length() <= MAX_FILE_LIST_LEN) {
                if (fileListForInfo.length() > 0) {
                    fileListForInfo.append(",");
                }
                fileListForInfo.append(new File(f.getFileName()).getName());
                if (fileListForInfo.length() >= MAX_FILE_LIST_LEN) {
                    fileListForInfo.append(",..");
                }
            }
            fileListForTar.append(f.getFileName());
        }
        logMessage("Shell downloading " + fileListForInfo + " (total: " + filesToGet.size() + ")", appProfile, ap);

        // ssh -c "cd <dest>; find . -name <app_start> -print|xargs tar -cz" | tar zx
        // tar -C <dest directory> -cz file1,file2,,, | tar zx
        // https://mkyong.com/java/how-to-create-tar-gz-in-java/
        StringBuilder remoteCmd = new StringBuilder().append("tar -C ").append(appProfile.getLogDirectory())
                .append(" -cz ").append(fileListForTar);
        try {
            ThreadedUnTarGZ stdoutReader = new ThreadedUnTarGZ(FilenameUtils.concat(ds.getOutputDir(), ap.getName())
                    , ds.isZipDest());
            stdoutReader.setProgressProc(new IProcessOutputRead() {
                @Override
                public void lineRead(String s) {
                    logMessage(s, appProfile, ap);
                }
            });
            stdoutReader.setDoneFileAction(new IDoneFileAction() {
                @Override
                public void fileDone(Path path) {
                    try {
                        indexer.processAddedFile(path);
                    } catch (Exception e) {
                        logMessage("exception adding file " + path, e);
                    }
                }
            });
            RemoteExecutionResult ret1 = sshClient.executePipedRemoteCommand(
                    ds.getUser(appProfile),
                    ds.getPassword(appProfile), theAppHost.getHost(), 22, remoteCmd.toString(), stdoutReader);
        } catch (Exception e) {
            SettingsForm.error("Exception while executing remote command: " + e.getMessage());
        }

        // executionResult = ret1.getRetCode();
        // stderr = ret1.getStderr();
        // stdout = ret1.getStdout();
    }

    private void fileTransfer(OSFile file, String outDir,
                              AppProfile appProfile, App ap, IFileTransferAction action) throws InterruptedException {
        File srcFile = new File(file.getFileName());
        final File destFile = Paths.get(outDir, srcFile.getParent(), srcFile.getName()).toFile();
        Path destPath = destFile.toPath();

        StringBuilder msg = new StringBuilder();
        if (Files.exists(destPath)) {
            msg.append("File [").append(destPath.toString()).append("] exists.");
            long destSize = 0;
            try {
                destSize = Files.size(destPath);
            } catch (IOException e) {
                logMessage("Exception while getting size of file [" + destPath + "]", e, appProfile, ap);
            }
            msg.append("Dst size:").append(destSize).append(" src size: ").append(file.getSize());
            if (destSize == file.getSize()) {
                logMessage(msg.append(" skipping").toString(), appProfile, ap);
                return;
            } else if (destSize > file.getSize()) {
                logMessage(Level.ERROR, msg.append(" destination is larger then source; skipping ").toString(), appProfile, ap);
                return;
            } else {
                logMessage(msg.append(" replacing").toString(), appProfile, ap);
            }
        }
        action.TransferActionOK(file, srcFile, destFile);
    }

    private void executeWinDownload(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
                                    ArrayList<OSFile> fileNameClause, boolean isLFMT, boolean lcaLog) throws InterruptedException {

        // preparing list of zip files and list of files to archive
        ArrayList<String> filesToArchive = new ArrayList<>(fileNameClause.size());
        ArrayList<OSFile> zippedFiles = new ArrayList<>();
        String filesPath = null;
        for (OSFile f : fileNameClause) {
            String fn = getLocalFileName(f.getFileName(), appProfile, ap);
            if (fn != null) {
                String n = FilenameUtils.getName(fn);
                String newPath = FilenameUtils.getFullPath(fn);
                if (filesPath != null && newPath != null && !filesPath.equalsIgnoreCase(newPath)) {
                    logMessage(Level.ERROR, "Different paths??? filesPath [" + filesPath + "] filename[" + fn + "]", appProfile, ap);
                    return;
                } else {
                    filesPath = newPath;
                }
                filesToArchive.add("'" + n + "'");
                zippedFiles.add(new OSFile(f.getFileName() + zipExt, Long.MAX_VALUE));
            }
        }
        if (filesToArchive.isEmpty())
            return;


        String outDir = FilenameUtils.concat(getDs().getOutputDir(), ap.getName());
        try {
            FileUtils.forceMkdir(new File(outDir));
        } catch (IOException e) {
            logMessage(Level.ERROR, "Not created output dir: " + e.getMessage(), appProfile, ap);
        }

        String finalFilesPath = filesPath;
        helperExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    prepareZips(appProfile, ap, theAppHost, finalFilesPath, filesToArchive);
                } catch (IOException e) {
                    logMessage("Exception preparing zip", e, appProfile, ap);
                } catch (ConfigException e) {
                    logMessage("Exception preparing zip", e, appProfile, ap);
                } catch (InterruptedException e) {
                    logMessage("Exception preparing zip", e, appProfile, ap);
                }
            }
        });

        for (OSFile file : zippedFiles) {
            logMessage("Transfering file [" + file.getFileName() + "] to dir [" + outDir + "]: ",
                    appProfile, ap);

            fileTransfer(file, outDir, appProfile, ap, (f, src, dst) -> {
                for (int i = 0; i < 30; i++) {
                    if (Files.exists(src.toPath())) {
                        long srcSize = -1;
                        long destSize = -1;
                        try {
                            if (Files.exists(dst.toPath())) {
                                destSize = Files.size(dst.toPath());
                                srcSize = Files.size(src.toPath());
                            }
                        } catch (IOException e) {
                        }
                        if (srcSize < 0 || destSize < 0 || destSize < srcSize) {
                            try {
                                FileUtils.copyFile(src, dst, true);
                                indexer.processAddedFile(dst);
                                logMessage("Copied [" + file.getFileName() + "] to dir [" + outDir + "]: ",
                                        appProfile, ap);
                            } catch (IOException e) {
                                logMessage("Failed to copy [" + file.getFileName() + "] to dir [" + outDir + "]: ", e,
                                        appProfile, ap);
                            }
                        }

                        try {
                            Files.delete(src.toPath());
                        } catch (IOException e) {
                            logMessage("Failed to delete [" + src.toString() + "]: ", e,
                                    appProfile, ap);
                        } finally {
                            break;
                        }
                    } else {
                        logMessage("File [" + file.getFileName() + "] not found yet. Waiting",
                                appProfile, ap);
                        Thread.sleep(1000);
                    }
                }
            });
        }
    }

    private void prepareZips(AppProfile appProfile, App ap, HostAppdir theAppHost, String destLogsDir, ArrayList<String> fileNames) throws IOException, InterruptedException, ConfigException {
        ArrayList<String> filesToGet = new ArrayList<>();
        for (int i = 0; i < fileNames.size(); i++) {
            filesToGet.add(fileNames.get(i));
            if (i % MAX_FILES_IN_BANCH == 0 || (i + 1 >= fileNames.size())) {
                StringBuilder zipCommand = new StringBuilder()
                        .append("Add-Type -assembly 'System.IO.Compression'\n" +
                                "Add-Type -assembly 'System.IO.Compression.FileSystem'\n" +
                                "$d='" + destLogsDir + "'\n" +
                                "foreach ($f in " +
                                StringUtils.join(filesToGet, ',') +
                                ") {\n" +
                                "$zn = $d+$f+'" + zipExt + "';$t=$d+'.'+$f+'" + zipExt + "'\n" +
                                "if(Test-Path $zn){Remove-Item $zn};if(Test-Path $t){Remove-Item $t}\n" +
                                "[System.IO.Compression.ZipArchive]$z=[System.IO.Compression.ZipFile]::Open($t,([System.IO.Compression.ZipArchiveMode]::Create))\n" +
                                "[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($z,$d+$f,$f)|out-null\n" +
                                "$z.Dispose()\n" +
                                "Rename-Item -Path $t -NewName $zn\n" +
                                "}");
                logger.info("executing [" + zipCommand.toString() + "]");


                StringBuilder cmd = new StringBuilder()
                        .append("winrs ")
                        .append("/remote:")
                        .append(theAppHost.getHost())
                        .append(" /username:")
                        .append(ds.getUser(appProfile))
                        .append(" /password:")
                        .append(ds.getPassword(appProfile))
                        .append(" Powershell -NoLogo -NonInteractive -encodedCommand ")
                        .append(base64Encode(zipCommand.toString()));
                Pair<ArrayList<String>, ArrayList<String>> arrayListArrayListPair = executeCommand(cmd.toString(), true, true, appProfile, ap);
                logMessage("Output of zip prepare: stdout:\n" +
                        StringUtils.join(arrayListArrayListPair.getKey(), '\n')
                        + "\nstderr:\n"
                        + StringUtils.join(arrayListArrayListPair.getValue()), appProfile, ap);
                filesToGet.clear();
            }
        }
    }

    private String getLocalFileName(String fileName, AppProfile appProfile, App ap) {
        if ((mFileName.reset(fileName)).find()) {
            return mFileName.group(1) + ":" + mFileName.group(2);
        } else {
            logMessage("Unrecognized file name [" + fileName.replace("\\", "\\\\") + "]", appProfile, ap);
            return null;
        }
    }

    private String base64Encode(String s) {
        byte[] bytes = Base64.encodeBase64(s.getBytes(Charsets.UTF_16LE));

        return new String(bytes);
    }

    private void executeDownload(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
                                 ArrayList<OSFile> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
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
                              ArrayList<OSFile> fileNames, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {

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
        for (OSFile file : fileNames) {
            rsyncParams.addAll(rSyncAddClause(FilenameUtils.getName(file.getFileName())));
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

    //    private void executeGet(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir,
//            ArrayList<String> fileNameClause, boolean useRSync1, boolean isLFMT, boolean lcaLog)
//            throws IOException, InterruptedException {
//        if (useRSync1) {
////            executeDownload(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
//        } else {
//            Utils.FileUtils.setCurrentDirectory(ds.getOutputDir());
//            ArrayList<String> sshParams = new ArrayList<>();
//            sshParams.add("ssh");
//            if (GetLogs.sshOptions != null) {
//                sshParams.addAll(Arrays.asList(StringUtils.split(GetLogs.sshOptions)));
//            }
//
//            if (isLFMT) {
//                DownloadSettings.LFMTHostInstance lfmtHostInstance = appProfile.getLFMT();
//                if (lfmtHostInstance == null) {
//                    return;
//
//                }
//                sshParams.add(lfmtHostInstance.getHost());
//
//            } else {
//                sshParams.add(theAppHost.getHost());
//
//            }
//
//            StringBuilder fileClause = new StringBuilder();
//            if (fileNameClause != null && fileNameClause.size() > 0) {
//                fileClause.append("\\( -type f ");
//
//                // fileClause.append("-a -name ")
//                // .append(fileNameClause);
////                fileClause.append("-a ").append(sshNameClause(fileNameClause));
//
//                fileClause.append(" \\) ");
//
//            }
//
//            StringBuilder sshCmd = new StringBuilder();
//
//            sshCmd.append("cd ").append(logsDir).append("; ");
//            sshCmd.append("find ").append((lcaLog) ? "lca" : ap).append(" -maxdepth 1 ").append(fileClause);
//            sshCmd.append(" -exec ");
//            sshCmd.append("tar -");
//            if (ds.isProd()) {
//                sshCmd.append("z");
//            }
//            sshCmd.append("cvf - ").append("{} +");
//            sshParams.add(sshCmd.toString());
//            ExtProcess procSSH = extProcessManager.addProcess(new ExtProcessApp(sshParams, false, true));
//
//            ExtProcess procTar;
//            ArrayList<String> tarParams = new ArrayList<>();
//            tarParams.add("tar");
//            tarParams.add("-x");
//            tarParams.add("-f");
//            tarParams.add("-");
//
//            procSSH.startProcess();
//            procTar = extProcessManager.addProcess(new ExtProcessApp(tarParams, procSSH));
//            procTar.startProcess();
//
//            procSSH.waitFor();
//            procTar.waitFor();
//            extProcessManager.doneProcess(procSSH);
//            extProcessManager.doneProcess(procTar);
//        }
//
//    }
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
                    SettingsForm.info("About to download " + lsGeneralTab.getSelectedFiles().size() + " files ("
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

                    HashMap<SavedSearchStorage, ArrayList<OSFile>> r = new HashMap<>();

                    for (JTableFileEntry row : lsTab.getSelectedFiles()) {
                        SavedSearchStorage storage = row.getStorage();
                        ArrayList<OSFile> rSyncFiles = r.get(storage);
                        if (rSyncFiles == null) {
                            rSyncFiles = new ArrayList<>();
                            r.put(storage, rSyncFiles);
                        }

                        rSyncFiles.add(row.getFile());
                    }
                    SettingsForm.info("About to download " + lsTab.getSelectedFiles().size() + " files (" + r.size()
                            + " threads)");

                    CountDownLatch latch = new CountDownLatch(r.size());

                    doExecuteCmd(parent, this, latch, new ISubTask() {
                        @Override
                        public void task() throws Exception {
                            executeDownload(r, latch);
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

    private void executeDownload(HashMap<SavedSearchStorage, ArrayList<OSFile>> r, CountDownLatch latch) throws Exception {
        for (Map.Entry<SavedSearchStorage, ArrayList<OSFile>> entry : r.entrySet()) {
            SavedSearchStorage key = entry.getKey();
            ArrayList<OSFile> value = entry.getValue();
            executor.execute(new CallbackThreadLatched(latch, new ISubTask() {
                @Override
                public void task() throws InterruptedException, IOException {
                    executeDownload(key.getAppProfile(), key.getAp(), key.getAppHost(), key.getLogsDir(), value,
                            key.isLfmt(), key.isLcaLog());
                }
            }));
        }
    }

    private void initParser() {
        try {
            ExecutionEnvironment ee = new ExecutionEnvironment();
            ee.setXmlCFG(GetLogs.getsXMLCfg());
            String s = GetLogs.getsDBName();
            ee.setDbname((StringUtils.isEmpty(s)) ? Paths.get(getDs().getOutputDir(), "logbr").toString() : s);
            ee.setBaseDir(getDs().getOutputDir());
            s = GetLogs.getsDBAlias();
            ee.setAlias((StringUtils.isEmpty(s)) ? "logbr" : s);
            ee.setIgnoreZIP(GetLogs.isbIgnoreZIP());
            ee.setParseTDiff(GetLogs.isbTDiffParse());
            ee.setSqlPragma(GetLogs.isbSQLPragma());
            System.setProperty("logPath", ee.getLogbrowserDir());
            System.setProperty("log4j2.saveDirectory", ee.getLogbrowserDir());

            indexer = Main.getInstance().init(ee);
            logger.info("Init parser: " + ((indexer == null) ? "FAIL" : "ok"));

        } catch (Exception e) {
            logMessage("Failed to init logbrowser", e);
        }
    }

    private void executeDownload(ArrayList<JTableFileEntry> selectedRows) throws Exception {
        if (selectedRows == null || selectedRows.isEmpty()) {
            SettingsForm.info("No rows selected");
        } else {
            HashMap<SavedSearchStorage, ArrayList<OSFile>> r = new HashMap<>();

            for (JTableFileEntry row : selectedRows) {
                SavedSearchStorage storage = row.getStorage();
                ArrayList<OSFile> rSyncFiles = r.get(storage);
                if (rSyncFiles == null) {
                    rSyncFiles = new ArrayList<>();
                    r.put(storage, rSyncFiles);
                }

                rSyncFiles.add(row.getFile());
            }
            if (r.size() > 0) {
//                initParser();

                for (Map.Entry<SavedSearchStorage, ArrayList<OSFile>> entry : r.entrySet()) {
                    SavedSearchStorage key = entry.getKey();
                    ArrayList<OSFile> value = entry.getValue();
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

    private String sshNameClause(ArrayList<OSFile> fileNameClause) {
        if (fileNameClause != null && !fileNameClause.isEmpty()) {
            StringBuilder ret1 = new StringBuilder();
            if (fileNameClause.size() > 1) {
                ret1.append(("\\( "));
            }
            for (int i = 0; i < fileNameClause.size(); i++) {
                String s = fileNameClause.get(i).getFileName();
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
        logMessage(Level.ERROR, str + ", Exception: "
                + e.getMessage());
    }

    private void logMessage(String str, AppProfile appProfile, App ap) {
        logMessage(Level.INFO, getLogPrefix(appProfile, ap) + str);
    }

    private void logMessage(Level lvl, String str, AppProfile appProfile, App ap) {
        logMessage(lvl, getLogPrefix(appProfile, ap) + str);
    }

    private void logMessage(Level lvl, String str) {
        SwingUtilities.invokeLater(() -> {

            if (lvl == Level.INFO) {
                SettingsForm.info(str);
            } else if (lvl == Level.ERROR) {
                SettingsForm.error(str);
            }
        });
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

        if (ds.getActionCommand() == GetCommand.GET || ds.getActionCommand() == GetCommand.GREPGET) {
            initParser();
        }
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
                                        public void task() throws Exception {
                                            try {
                                                executeCmd(appProfile, app, false);
                                            } catch (ConfigException e) {
                                                logMessage("Config error", e, appProfile, app);
                                            }
                                        }
                                    });
                                }
                                if (ds.isLfmt()) {
                                    ret1.add(new ISubTask() {
                                        @Override
                                        public void task() throws Exception {
                                            try {
                                                executeCmd(appProfile, app, true);
                                            } catch (ConfigException e) {
                                                logMessage("Config error", e, appProfile, app);
                                            }
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
            tsk.addParsingFinalize();
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

        initParser();
        tsk.setRp(rp);
        tsk.addParsingFinalize();
        tsk.setBeforeActions(getBeforeActions());
        tsk.setAfterActions(getAfterActions());

        tsk.execute();

    }

    interface IFileTransferAction {

        void TransferActionOK(OSFile file, File src, File dst) throws InterruptedException;
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
            setFinishingAction(finishingTask, finishLatch);
        }

        @Override
        void onBackground() throws Exception {
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
        String theTitle = "";
        private String outFile;
        private boolean displayForm;
        private RequestProgress rp = null;
        private ISubTask startingTask = null;
        private CountDownLatch startingLatch = null;
        private ArrayList<Pair<ISubTask, CountDownLatch>> finishingActions = new ArrayList<>();

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

        abstract void onBackground() throws Exception;

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
                        SettingsForm.info("Thread not done; killing");
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

        public void setFinishingAction(ISubTask finishingTask, CountDownLatch finishingLatch) {
            this.finishingActions.add(new Pair<>(finishingTask, finishingLatch));
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
                for (Pair<ISubTask, CountDownLatch> action : finishingActions) {
                    if (action.getKey() != null)
                        action.getKey().task();
                    if (action.getValue() != null)
                        action.getValue().await();
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
                SettingsForm.info("Command executed");
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

        private void addParsingFinalize(){
            CountDownLatch finalLatch = new CountDownLatch(1);
            setFinishingAction(
                    new ISubTask() {
                        @Override
                        public void task() throws Exception {
                            if(indexer!=null){
                                Thread.sleep(300);
                                indexer.finishParsing();
                            }
                            finalLatch.countDown();
                        }
                    }, finalLatch);
        }

        private void setAfterActions(ArrayList<String> afterActions) {
            if (afterActions != null) {
                CountDownLatch finalLatch = new CountDownLatch(1);
                setFinishingAction(
                        new ISubTask() {
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
                        }, finalLatch);
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
                } catch (Exception interruptedException) {
                    logMessage(Level.INFO, "Action interrupted");
                }
                // logger.debug("Thread " + Thread.currentThread() + " done task");

            } finally {
                latch.countDown();
            }
        }

    }

}
