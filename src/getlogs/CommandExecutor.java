/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.Pair;
import Utils.ScreenInfo;
import Utils.UTCTimeRange;
import Utils.UnixProcess.ExtProcess;
import static Utils.Util.rSyncAddClause;
import static Utils.Util.stripDir;
import com.jidesoft.dialog.JideOptionPane;
import com.jidesoft.dialog.StandardDialog;
import getlogs.DownloadSettings.App;
import getlogs.DownloadSettings.AppProfile;
import getlogs.SettingsPanel.InfoPanel;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author stepan_sydoruk
 */
public class CommandExecutor {

//    public static void main(String[] args) throws Exception {
//        System.out.println(cloudPattern("bb{YYYY}_{MM}_{DD}_{HH}aa.log", "20190", "1"));
//        System.out.println(cloudPattern("bb{YYYY}_{MM}_{DD}_{HH}aa.log", "201901", "1"));
//        System.out.println(cloudPattern("bb{YYYY}_{MM}_{DD}_{HH}aa.log", "2019012", "1"));
//        System.out.println(cloudPattern("bb{YYYY}_{MM}_{DD}_{HH}aa.log", "20190124", "1"));
//    }
    private static final Pattern regVariable = Pattern.compile("\\{([A-Z]{2,4}|NAME)\\}");
    private static final Pattern regPostAction = Pattern.compile("\\{(NAME|OUTDIR)\\}");

    private static String cloudPattern(String fileNameRegex, String datePattern, String timePattern, App ap) {
        System.out.println(fileNameRegex + "-" + datePattern + "-" + timePattern);
        int pos = 0;
        Matcher m;
        StringBuilder ret = new StringBuilder();
        while ((m = regVariable.matcher(fileNameRegex)).find(pos)) {
            ret.append(fileNameRegex.substring(pos, m.start()));
            if (m.group(1).equals("YYYY")) {
                ret.append(fillPattern(datePattern, 0, 4));
            } else if (m.group(1).equals("MM")) {
                ret.append(fillPattern(datePattern, 4, 2));

            } else if (m.group(1).equals("DD")) {
                ret.append(fillPattern(datePattern, 6, 2));

            } else if (m.group(1).equals("HH")) {
                ret.append(fillPattern(timePattern, 0, 2));

            } else if (m.group(1).equals("MI")) {
                ret.append(fillPattern(timePattern, 2, 2));

            } else if (m.group(1).equals("SS")) {
                ret.append(fillPattern(timePattern, 4, 4));
            } else if (m.group(1).equals("NAME")) {
                ret.append(ap.getName());

            } else {
                SettingsDialog.error("Incorrect specification [" + m.group(1) + "] "
                        + "in pattern [" + fileNameRegex + "]. Allowed: YYYY MM DD HH MI SS");
                return null;
            }

//            ret.append(StringUtils.repeat("^", m.end() - m.start()));
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
            for (cnt = 0; cnt < start; cnt++) {//skipping to the start
                if ((m = GetLogs.regRegDigits.matcher(datePattern)).find(pos)) {
                    pos = m.end();
                } else {
                    break;
                }
            }
            //pos is position of first character to fill
            if (cnt == start) {
                for (cnt = 0; cnt < count; cnt++) {//skipping to the start
                    if ((m = GetLogs.regRegDigits.matcher(datePattern)).find(pos)) {
                        ret1.append(datePattern.substring(m.start(), m.end()));
                        pos = m.end();
                        filled++;
                    }
                }
            }
        }
        ret1.append(StringUtils.repeat("[0-9]", count - filled));

        return ret1;
    }

    private DownloadSettings ds;

    private boolean isText;
    private Window parent;
    private ArrayList<JTableFileEntry> lsFilesAll = new ArrayList<>();
    private ArrayList<JTableFileEntry> lsFilesLast = new ArrayList<>();
    private RequestProgress rp = null;
    private final ExtProcessManager extProcessManager;

    public CommandExecutor(boolean isText) {
        this.isText = isText;
        extProcessManager = new ExtProcessManager();
    }

    public CommandExecutor(Window p) {
        this(false);
        parent = p;
    }

    private int ret = StandardDialog.RESULT_CANCELLED;

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
            JOptionPane.showMessageDialog(parent, "Either Application or LCA checkbox needs to be checked", "Cannot continue",
                    JOptionPane.ERROR_MESSAGE);
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

    SettingsPanel.InfoPanel lsOutput;

    public void executeCmd(AppProfile appProfile, App ap, boolean isLFMT) throws IOException, InterruptedException {
        SettingsDialog.info("executing for profile [" + appProfile.toString() + "] ap[" + ap + "] lfmt:" + isLFMT);
        String theAppHost;
        if (GetLogs.appHost == null || GetLogs.appHost.isEmpty()) {
            theAppHost = (String) GetLogs.getHosts().get(ap.getName()); // first for one application only
            if (theAppHost == null) {
                GetLogs.exitHelp("Host for app [" + ap + "] not found; exiting");
                return;
            }
        } else {
            theAppHost = GetLogs.appHost;
        }

        StringBuilder logsDir = new StringBuilder();

        if (isLFMT) {
            DownloadSettings.LFMTHostInstance lfmtHostInstance = appProfile.getLFMT();
            if (lfmtHostInstance == null || lfmtHostInstance.getHost() == null) {
                GetLogs.exitHelp("LFMT not configured properly for app " + ap);
            }
            logsDir.append(lfmtHostInstance.getBaseDir());
            String instance = lfmtHostInstance.getInstance();
            if (instance != null && !instance.isEmpty()) {
                logsDir.append(lfmtHostInstance.getInstance()).append("/")
                        .append(lfmtHostInstance.getInstance()).append("_cls");
            };

            logsDir.append("/").append(theAppHost) //                    .append("/")
                    //                    .append(ap)
                    ;
        } else {
            logsDir.append(GetLogs.getProdBaseDir());

        }

        if (ds.isAppLogs()) {
            ArrayList<StringBuilder> fileNameClause = getFileNameClause(appProfile, ap, false);
            ArrayList<JTableFileEntry> lsFiles = executeCmd(appProfile, ap, theAppHost, logsDir.toString(), fileNameClause, isLFMT, false);
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
            ArrayList<StringBuilder> fileNameClause = getFileNameClause(appProfile, ap, true);
            ArrayList<JTableFileEntry> lsFiles = executeCmd(appProfile, ap, theAppHost, logsDir.toString(), fileNameClause, isLFMT, true);
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

    public SavedSearchStorage getStorage(AppProfile appProfile, App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog) {
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

    ArrayList<SavedSearchStorage> savedSearch = new ArrayList<>();
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private ArrayList<JTableFileEntry> executeLS(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir,
            ArrayList<StringBuilder> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        ArrayList<JTableFileEntry> lsFiles = null;
        ArrayList<String> sshParams = new ArrayList<>();
        HashMap<String, Boolean> nameSuffixes = appProfile.getNameSuffixes();

        sshParams.add("ssh");
        if (GetLogs.sshOptions != null) {
            sshParams.addAll(
                    Arrays.asList(StringUtils.split(GetLogs.sshOptions))
            );
        }
        sshParams.addAll(Arrays.asList(new String[]{"-o", "StrictHostKeyChecking no"}));

        if (isLFMT) {
            sshParams.add(appProfile.getLFMT().getHost());
        } else {
            sshParams.add(theAppHost);
        }

        StringBuilder sshCmd = new StringBuilder();

        sshCmd.append("bash -c \"");

        sshCmd.append("cd ").append(logsDir).append(";");
//        sshCmd.append(" declare -a arr=( \\\"-001\\\" \\\"-768\\\" ); for ext in \\\"\\${arr[@]}\\\"; do");
        sshCmd.append(" declare -a arr=(");
        sshCmd.append(" \\\"");
        for (int i = 0; i < fileNameClause.size(); i++) {
            StringBuilder s = fileNameClause.get(i);
            if (i > 0) {
                sshCmd.append("\\\" \\\"");
            }
            sshCmd.append(s);
        }

        sshCmd.append("\\\" ");

        sshCmd.append(" ); for ext in \\\"\\${arr[@]}\\\"; do");

//        sshCmd.append(" echo ").append("\\${ext}").append(" ; ");
        //this is used for testing
//        sshCmd.append(" pwd; echo \\$ext; echo \\${ext}; ");
        sshCmd.append(" find ")
                .append((lcaLog) ? "lca" : ap)
                .append(" -name \\${ext} ");
//        sshCmd.append(" -a -type f ");
        if (appProfile.isIsGenesysName()) {
            sshCmd.append(" -a ! \\( -name \\*snapshot.log \\) ");
        }
        sshCmd.append(" ");

        if (!ds.isListFiles()) {
            sshCmd.append(" -o -type d ");
        }

        sshCmd.append(" -print | sort -r");
        if (ds.getTimeProfile() == SettingsPanel.TimeProfile.VALUE_FILES) {
            sshCmd.append(" | head -").append(ds.getHours());
        }

        sshCmd.append(" ; done");

        sshCmd.append("\"");
        sshParams.add(sshCmd.toString());
        ExtProcess procSSH = null;
        procSSH = extProcessManager.addProcess(new ExtProcessApp(appProfile, ap, sshParams, false, true));
        procSSH.startProcess(true, true);

        int waitFor = procSSH.waitFor();
        logger.debug("process terminated, result: " + waitFor);
        if (waitFor != 0) {
            SettingsDialog.error("LS failed, error code: " + waitFor);
            ArrayList<String> errBuf = procSSH.getErrBuf();
            if (errBuf != null && !errBuf.isEmpty()) {
                logMessage(Level.ERROR, StringUtils.join(errBuf, " | "));
//                lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, null, logsDir, isLFMT, lcaLog), StringUtils.join(errBuf, " | ")));
//                lsTab.addRow(appProfile, ap, theAppHost, null, logsDir.toString(), isLFMT, lcaLog, StringUtils.join(errBuf, " | "));

            }

        } else {
            lsFiles = new ArrayList<>();
            boolean startFound = false;
            String savedPrefix = null;
            for (String string : procSSH.getSTDOut()) {
//                lsTab.addRow(appProfile, ap, theAppHost, string, logsDir.toString(), isLFMT, lcaLog);
                if (ds.getTimeProfile() == SettingsPanel.TimeProfile.RANGE) {
                    Pair<Long, String> utcTime = appProfile.getFileNameTime(string);
                    boolean shouldAdd = true;
                    UTCTimeRange timeRange = ds.getTimeRange();
                    if (utcTime != null) { // was able to parse time name
                        if (utcTime.getKey() > timeRange.getEnd()) {
                            shouldAdd = false;
                        } else {
                            if ((utcTime.getKey() > timeRange.getStart())) {
                                shouldAdd = true;
                            } else {
                                /*so the assumption is that files are always sorted*/
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
                    logger.debug("file [" + string + "] range: " + timeRange.toString()
                            //                                +" utcTime:" + utcTime + "timeRange:" + timeRange + "(utcTime > timeRange.getStart()): " + (utcTime > timeRange.getStart()) + " (utcTime < timeRange.getEnd()):" + (utcTime < timeRange.getEnd())
                            + " shouldadd: " + shouldAdd
                    );
                    if (shouldAdd) {
                        lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, string, logsDir, isLFMT, lcaLog), string));
                    }

                } else {
                    lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, string, logsDir, isLFMT, lcaLog), string));
                }
            }
            SettingsDialog.info("ls successful for [" + appProfile.getName() + "] + app[" + ap + "]" + ((isLFMT) ? " on LFMT" : "") + " : got " + lsFiles.size() + " files");
            ArrayList<String> errBuf = procSSH.getErrBuf();
            if (errBuf != null && !errBuf.isEmpty()) {
                logMessage(Level.ERROR, StringUtils.join(errBuf, " | "));
//                lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, null, logsDir, isLFMT, lcaLog), StringUtils.join(errBuf, " | ")));
//                lsTab.addRow(appProfile, ap, theAppHost, null, logsDir.toString(), isLFMT, lcaLog, StringUtils.join(errBuf, " | "));

            }
        }

        extProcessManager.doneProcess(procSSH);

        ((AbstractTableModel) lsTab.getModel()).fireTableDataChanged();
        return lsFiles;

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

    private void executeGrepGet(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, ArrayList<StringBuilder> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        ArrayList<String> executeGrep = executeGrep(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        ArrayList<String> rSyncFiles = new ArrayList<>();
        for (String fileName : executeGrep) {
            rSyncFiles.addAll(rSyncAddClause(FilenameUtils.getName(fileName)));
        }

        executeRSync(appProfile, ap, theAppHost, logsDir, rSyncFiles, isLFMT, lcaLog);
    }

    private ArrayList<String> executeGrep(AppProfile appProfile, DownloadSettings.App ap,
            String appHost1, String logsDir, ArrayList<StringBuilder> fileNameClause,
            boolean isLFMT, boolean isLCA) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (GetLogs.getsUserName() != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", GetLogs.getsUserName()}));
        }
        if (isLFMT) {
            DownloadSettings.LFMTHostInstance lfmt1 = appProfile.getLFMT();
            if (lfmt1 == null) {
                return null;
            }
            sshParams.add(lfmt1.getHost());
        } else {
            sshParams.add(appHost1);
        }

        StringBuilder fileClause = new StringBuilder();
//        fileClause.append("\\("); 

        fileClause.append(sshNameClause(fileNameClause));

//        fileClause.append(" \\) ");
        StringBuilder sshCmd = new StringBuilder();

        sshCmd.append("cd ").append(logsDir).append("; ");
        sshCmd.append("find ")
                .append((isLCA) ? "lca" : ap)
                .append(" ")
                .append(fileClause);
        sshCmd.append(" ");
//        sshCmd.append("\\( ")
//                .append(" -iname *.log -execdir grep Trc {} \\; -true ");
//        sshCmd.append("\\)");
//        sshCmd.append(" -o ");
        ArrayList<String> matchedFiles = new ArrayList<>();
        for (Map.Entry<String, String> extUnp : GetLogs.extUnpacker.entrySet()) {
            for (String matchedFile : GetLogs.execGrep(extUnp.getKey(), extUnp.getValue(), sshParams, sshCmd, ds.getGrepText())) {
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
        StringBuilder ret = new StringBuilder();
        while ((m = regPostAction.matcher(word)).find(pos)) {
            ret.append(word.substring(pos, m.start()));
            if (m.group(1).equals("OUTDIR")) {
                ret.append(ds.getOutputDir());

            } else if (m.group(1).equals("OUTDIR")) {
                ret.append(ds.getOutputDir());

            } else {
                SettingsDialog.error("Incorrect specification [" + m.group(1) + "] "
                        + "in pattern [" + word + "]. Allowed: {OUTDIR}");
                return null;
            }

//            ret.append(StringUtils.repeat("^", m.end() - m.start()));
            pos = m.end();
        }
        if (pos < word.length()) {
            ret.append(word.substring(pos));
        }
        return ret.toString();

    }

    private void executeAfterCommand(String key) throws IOException, InterruptedException {
        ArrayList<String> cmdParams = new ArrayList<>();
        String[] split = StringUtils.split(key);
        for (String string : split) {
            cmdParams.add(replacePostActionVars(string));
        }

//        logger.trace("executing: " + rsyncParams);
        ExtProcess procRSync = extProcessManager.addProcess(new ExtProcessFinishing(
                cmdParams, true, true));
        procRSync.startProcess();
        int waitFor = procRSync.waitFor();
        logger.debug("process terminated, result: " + waitFor);

        extProcessManager.doneProcess(procRSync);

    }

    /**
     *
     * @param appProfile
     * @param ap
     * @param theAppHost
     * @param logsDir
     * @param fileNameClause - cannot work via ssh variable
     * @param isLFMT
     * @param lcaLog
     * @throws IOException
     * @throws InterruptedException
     */
    private void executeRSync(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        ArrayList<String> rsyncParams = new ArrayList<>();
        rsyncParams.add("rsync");
        rsyncParams.add("-avz");
//        rsyncParams.add("--compress-level=8");
        rsyncParams.add("-e");
        rsyncParams.add("ssh " + GetLogs.getSshOptions());
//        rsyncParams.add(GetLogs.getSshOptions());

        rsyncParams.addAll(fileNameClause);
        rsyncParams.add("-f");
        rsyncParams.add("- **");
        StringBuilder srcSpec = new StringBuilder();
        String lfmtHost = null;

        String u = GetLogs.getsUserName();
        if (u != null && !u.isEmpty()) {
            srcSpec.append(u).append("@");
        }
        if (isLFMT) {
            DownloadSettings.LFMTHostInstance lfmtHostInstance = appProfile.getLFMT();
            if (lfmtHostInstance == null) {
                return;
            }
            srcSpec.append(lfmtHostInstance.getHost()).append(":")
                    .append(logsDir).append("/").append((lcaLog) ? "lca" : ap).append("/").append("");

        } else {
            srcSpec.append(theAppHost).append(":")
                    .append(logsDir).append("/").append((lcaLog) ? "lca" : ap).append("/").append("");

        }

        rsyncParams.add(srcSpec.toString());

        StringBuilder dstSpec = new StringBuilder();
        dstSpec.append(ds.getOutputDir()).append("/");
        if (lcaLog) {
            dstSpec.append(theAppHost).append("/").append("lca");
        } else {
            dstSpec.append(ap);
        }
        Utils.FileUtils.mkDir(dstSpec.toString());

        rsyncParams.add(dstSpec.toString());
//        logger.trace("executing: " + rsyncParams);
        ExtProcessApp procRSync = (ExtProcessApp) extProcessManager.addProcess(new ExtProcessApp(
                appProfile, ap, rsyncParams,
                true, true));
        procRSync.startProcess();
        int waitFor = procRSync.waitFor();
        logger.debug("process terminated, result: " + waitFor);

        extProcessManager.doneProcess(procRSync);

    }

    class ExtProcessApp extends ExtProcess {

        private AppProfile profile = null;
        private App app = null;

        private ExtProcessApp(AppProfile appProfile, App ap, ArrayList<String> sshParams, boolean b, boolean b0) throws IOException {
            this(sshParams, b, b0);
            this.profile = appProfile;
            this.app = ap;
        }

        private void initLogBack(boolean logStdin, boolean logStdout) {
            if (logStdin) {
                setStdinReadProc(new IProcessOutputRead() {
                    @Override
                    public void lineRead(String s) {
                        logMessage(Level.INFO, s);
                    }
                });
            }

            if (logStdout) {
                setStderrReadProc(new IProcessOutputRead() {
                    @Override
                    public void lineRead(String s) {
                        StringBuilder msg = new StringBuilder();
                        if (profile != null) {
                            msg.append("[").append(profile).append("]");
                        }
                        if (app != null) {

                            msg.append(" a[").append(app.getName()).append("(").append(GetLogs.getHosts().lookupHost(app.getName())).append(")]");
                            msg.append(" cmd[" + getCmd() + "]");
                        }
                        msg.append("! ").append(s);
                        logMessage(Level.ERROR, msg.toString());

                    }
                });
            }
        }

        public ExtProcessApp(ArrayList<String> tarParams, ExtProcess procSSH) throws IOException {
            super(tarParams, procSSH);
            initLogBack(false, false);
        }

        public ExtProcessApp(List<String> tarParams, boolean logStdin, boolean logStdout) throws IOException {
            super(tarParams);
            initLogBack(logStdin, logStdout);
        }

    }

    class ExtProcessFinishing extends ExtProcess {

        private void initLogBack(boolean logStdin, boolean logStdout) {
            if (logStdin) {
                setStdinReadProc(new IProcessOutputRead() {
                    @Override
                    public void lineRead(String s) {
                        logMessage(Level.INFO, s);
                    }
                });
            }

            if (logStdout) {
                setStderrReadProc(new IProcessOutputRead() {
                    @Override
                    public void lineRead(String s) {
                        StringBuilder msg = new StringBuilder();
                        msg.append("! ").append(s);
                        logMessage(Level.ERROR, msg.toString());

                    }
                });
            }
        }

        public ExtProcessFinishing(ArrayList<String> tarParams, ExtProcess procSSH) throws IOException {
            super(tarParams, procSSH);
            initLogBack(false, false);
        }

        public ExtProcessFinishing(List<String> tarParams, boolean logStdin, boolean logStdout) throws IOException {
            super(tarParams);
            initLogBack(logStdin, logStdout);
        }

    }

    private void executeGet(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, ArrayList<StringBuilder> fileNameClause, boolean useRSync1, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        if (useRSync1) {
            executeRSync(appProfile, ap, theAppHost, logsDir, rSyncAddClause(fileNameClause.toString()), isLFMT, lcaLog);
        } else {
            Utils.FileUtils.setCurrentDirectory(ds.getOutputDir());
            ArrayList<String> sshParams = new ArrayList<>();
            sshParams.add("ssh");
            if (GetLogs.sshOptions != null) {
                sshParams.addAll(
                        Arrays.asList(StringUtils.split(GetLogs.sshOptions))
                );
            }

            if (isLFMT) {
                DownloadSettings.LFMTHostInstance lfmtHostInstance = appProfile.getLFMT();
                if (lfmtHostInstance == null) {
                    return;

                }
                sshParams.add(lfmtHostInstance.getHost());

            } else {
                sshParams.add(theAppHost);

            }

            StringBuilder fileClause = new StringBuilder();
            if (fileNameClause != null && fileNameClause.size() > 0) {
                fileClause.append("\\( -type f ");

//                fileClause.append("-a -name ")
//                        .append(fileNameClause);
                fileClause.append("-a ")
                        .append(sshNameClause(fileNameClause));

                fileClause.append(" \\) ");

            }

            StringBuilder sshCmd = new StringBuilder();

            sshCmd.append("cd ").append(logsDir).append("; ");
            sshCmd.append("find ")
                    .append((lcaLog) ? "lca" : ap)
                    .append(" ")
                    .append(fileClause);
            sshCmd.append(" -exec ");
            sshCmd.append("tar -");
            if (ds.isProd()) {
                sshCmd.append("z");
            }
            sshCmd.append("cvf - ")
                    .append("{} +");
            sshParams.add(sshCmd.toString());
            ExtProcess procSSH = extProcessManager.addProcess(new ExtProcessApp(sshParams, false, true));

            ExtProcess procTar = null;
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

    private StringBuilder getGenesysNameClause(AppProfile appProfile, App ap, boolean lca, String suffix) {
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
        if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX && ds.getDateSpec() != null && !ds.getDateSpec().isEmpty()) {
            fileNameClause.append(GetLogs.expandPattern(ds.getDateSpec(), 8));
        } else {
            fileNameClause.append(StringUtils.repeat("[0-9]", 8));
        }
        fileNameClause.append("_");

        if (ds.getTimeProfile() == SettingsPanel.TimeProfile.REGEX && ds.getTimeSpec() != null && !ds.getTimeSpec().isEmpty()) {
            fileNameClause.append(GetLogs.expandPattern(ds.getTimeSpec(), 6));
        } else {
            fileNameClause.append(StringUtils.repeat("[0-9]", 6));
        }
        fileNameClause.append("_");

        fileNameClause.append(StringUtils.repeat("[0-9]", 3))
                .append("").append(backSlash).append(".").append(backSlash).append("*");

        return fileNameClause;
    }

    private ArrayList<StringBuilder> getFileNameClause(AppProfile appProfile, App ap, boolean isLCA) {
        ArrayList<StringBuilder> ret1 = new ArrayList<>();
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
                            ret1.add(new StringBuilder(cloudPattern(suffix, datePattern, timePattern, ap)));
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
                        if (suffix.trim().equals(".")) {
                            ret1.add(getGenesysNameClause(appProfile, ap, isLCA, ap.getName()));
                        } else {
                            ret1.add(getGenesysNameClause(appProfile, ap, isLCA, suffix));
                        }
                    }
                }
            } else {
                ret1.add(getGenesysNameClause(appProfile, ap, isLCA, null));
            }

        }
        GetLogs.logger.trace("fileName clause: [" + ret1 + "]");
        return ret1;
    }

    private ArrayList<JTableFileEntry> executeCmd(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, ArrayList<StringBuilder> fileNameClause, boolean isLFMT, boolean isLCALog) {
        try {
            switch (ds.getActionCommand()) {
                case GREP:
                    executeGrep(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    break;

                case GET:
                    HashMap<String, Boolean> suff = appProfile.getNameSuffixes();
                    if (!isLCALog && (suff == null || suff.isEmpty()) && ds.getTimeProfile() != SettingsPanel.TimeProfile.VALUE_FILES) {
                        executeGet(appProfile, ap, theAppHost, logsDir, fileNameClause, ds.isUseRSync(), isLFMT, isLCALog);
                    } else {
                        /*
                        this may be confusing and is not very good design.
                        So if there are suffixes, we execute ls instead. Get will be called at very top level after application is processed.
                        this way for suffixes we first get list of files and then execute rsync on files received.
                        This may be ugly, but is effective
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
            SettingsDialog.error(e.toString() + ": " + StringUtils.join(e.getStackTrace(), ";"));
        } catch (InterruptedException e) {
//            SettingsDialog.error(e.toString() + ": " + StringUtils.join(e.getStackTrace(), ";"));
        }
        return null;
    }

    void setSettingsFile(String sGUIProfile) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void showRecent() throws IOException, InterruptedException {
        if (lsFilesLast != null && !lsFilesLast.isEmpty()) {
            if (lsTab == null) {
                lsTab = new JTableFileList();
            }
            if (lsOutput == null) {
                lsOutput = new SettingsPanel.InfoPanel(parent, "List of files", lsTab,
                        "Download %d files");
            }
            lsTab.setFiles(lsFilesLast);
            lsOutput.doShow();

            if (lsOutput.getCloseCause() == JOptionPane.OK_OPTION) {
                if (lsTab.getSelectedFiles().size() > 0) {

                    HashMap<SavedSearchStorage, ArrayList<String>> r = new HashMap<>();

                    for (JTableFileEntry row : lsTab.getSelectedFiles()) {
                        SavedSearchStorage storage = row.storage;
                        ArrayList<String> rSyncFiles = r.get(storage);
                        if (rSyncFiles == null) {
                            rSyncFiles = new ArrayList<>();
                            r.put(storage, rSyncFiles);
                        }

                        rSyncFiles.addAll(rSyncAddClause(FilenameUtils.getName(row.fileName)));
                    }
                    SettingsDialog.info("About to download " + lsTab.getSelectedFiles().size() + " files (" + r.size() + " threads)");

                    CountDownLatch latch = new CountDownLatch(r.size());
                    CountDownLatch finalLatch = new CountDownLatch(1);

                    doExecuteCmd(parent, this,
                            latch,
                            new ISubTask() {
                        @Override
                        public void task() throws InterruptedException, IOException {
                            executeRSync(r, latch);
                        }
                    },
                            finalLatch,
                            new ISubTask() {
                        @Override
                        public void task() throws InterruptedException, IOException {
                            afterActions(finalLatch);
                        }
                    }
                    );

                }
            }
        } else {
            JOptionPane.showMessageDialog(parent, "No files to display", "Info", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void afterActions(CountDownLatch latch) throws InterruptedException {

        executor.execute(new CallbackThreadLatched(latch, new ISubTask() {
            @Override
            public void task() throws InterruptedException, IOException {
                for (Pair<String, Boolean> entry : ds.getAfterActions()) {
                    if (entry.getValue()) {
                        executeAfterCommand(entry.getKey());
                    }

                }
//                for (int i = 0; i < 10; i++) {
//                    Thread.sleep(1000);
//                    SettingsDialog.info(Integer.toString(i));
//                }
                latch.countDown();
            }

        }));

    }

    private void executeRSync(HashMap<SavedSearchStorage, ArrayList<String>> r,
            CountDownLatch latch) {

        for (Map.Entry<SavedSearchStorage, ArrayList<String>> entry : r.entrySet()) {
            SavedSearchStorage key = entry.getKey();
            ArrayList<String> value = entry.getValue();
            executor.execute(new CallbackThreadLatched(latch, new ISubTask() {
                @Override
                public void task() throws InterruptedException, IOException {
                    executeRSync(key.getAppProfile(), key.getAp(), key.getAppHost(), key.getLogsDir(), value, key.isLfmt(), key.isLcaLog());
                }
            }));

        }

    }

    private void executeRSync(ArrayList<JTableFileEntry> selectedRows) {
        if (selectedRows == null || selectedRows.size() == 0) {
            SettingsDialog.info("No rows selected");
        } else {
            HashMap<SavedSearchStorage, ArrayList<String>> r = new HashMap<>();

            for (JTableFileEntry row : selectedRows) {
                SavedSearchStorage storage = row.storage;
                ArrayList<String> rSyncFiles = r.get(storage);
                if (rSyncFiles == null) {
                    rSyncFiles = new ArrayList<>();
                    r.put(storage, rSyncFiles);
                }

                rSyncFiles.addAll(rSyncAddClause(FilenameUtils.getName(row.fileName)));
            }
            if (r.size() > 0) {
                for (Map.Entry<SavedSearchStorage, ArrayList<String>> entry : r.entrySet()) {
                    SavedSearchStorage key = entry.getKey();
                    ArrayList<String> value = entry.getValue();
                    try {
                        executeRSync(key.getAppProfile(), key.getAp(), key.getAppHost(), key.getLogsDir(), value, key.isLfmt(), key.isLcaLog());

                    } catch (IOException ex) {
                        Logger.getLogger(CommandExecutor.class
                                .getName()).log(java.util.logging.Level.SEVERE, null, ex);

                    } catch (InterruptedException ex) {
                        Logger.getLogger(CommandExecutor.class
                                .getName()).log(java.util.logging.Level.SEVERE, null, ex);
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

    private StringBuilder sshNameClause(ArrayList<StringBuilder> fileNameClause) {
        if (fileNameClause != null && !fileNameClause.isEmpty()) {
            StringBuilder ret1 = new StringBuilder();
            if (fileNameClause.size() > 1) {
                ret1.append(("\\( "));
            }
            for (int i = 0; i < fileNameClause.size(); i++) {
                StringBuilder s = fileNameClause.get(i);
                if (i > 0) {
                    ret1.append(" -o ");
                }
                ret1.append("-name ").append(s);

            }
            if (fileNameClause.size() > 1) {
                ret1.append((" \\)"));
            }
            return ret1;
        } else {
            return null;
        }

    }

    private void logMessage(Level lvl, String str) {
        if (lvl == Level.INFO) {
            SettingsDialog.info(str);
        } else if (lvl == Level.ERROR) {
            SettingsDialog.error(str);
        }
    }

    private ArrayList<StringBuilder> cloudStandardNames() {
        StringBuilder fileNameClause = new StringBuilder();
        if (!ds.isUseRSync()) {
            String backSlash = "\\";
            fileNameClause.append("").append(backSlash).append("*").append(backSlash).append(".");
        } else {
            fileNameClause.append("*cloud*").append("-");
        }
        fileNameClause.append(GetLogs.cloudDatePattern(ds.getDateSpec(), ds.getTimeSpec(), ds.getTimeProfile()));
        ArrayList<StringBuilder> ret1 = new ArrayList<>();
        ret1.add(fileNameClause);
        ret1.add(new StringBuilder("*cloud.log*"));
        return ret1;
    }

    private void cancel() {
        extProcessManager.cancelAll();

    }

    /**
     *
     */
    public interface ISubTask {

        void task() throws InterruptedException, IOException;
    }

    public interface IThreadingSubTask {

        ArrayList<ISubTask> task() throws InterruptedException, IOException;
    }

    public interface ISubTaskGroup {

        ThreadGroup task() throws InterruptedException, IOException;
    }

    public class QueryTask extends QueryTaskBase {

        private CountDownLatch latch;
        private ISubTask subTask;

        private QueryTask(CommandExecutor aThis, CountDownLatch latch, ISubTask subTask) {
            super(aThis);
            this.latch = latch;
            this.subTask = subTask;
        }

        private QueryTask(CommandExecutor aThis,
                CountDownLatch latch,
                ISubTask subTask,
                CountDownLatch finishLatch,
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

    };

    public class QueryThreadingTask extends QueryTaskBase {

        private IThreadingSubTask threadingSubTask;

        public QueryThreadingTask(CommandExecutor ce, IThreadingSubTask threadingSubTask) {
            super(ce);
            this.threadingSubTask = threadingSubTask;
        }

//        boolean myCancel(boolean mayInterruptIfRunning) {
//            ce.cancel();
//            cancel(mayInterruptIfRunning);
//            if (isDone()) {
//                return true;
//            }
//
//            try {
//                Thread.sleep(150);
//
//                /*  
//            may consider implementing this
//            
//            from https://stackoverflow.com/questions/671049/how-do-you-kill-a-thread-in-java
//            
//            Thread f = <A thread to be stopped>
//            Method m = Thread.class.getDeclaredMethod( "stop0" , new Class[]{Object.class} );
//            m.setAccessible( true );
//            m.invoke( f , new ThreadDeath() );
//            
//                 */
//                if (!isDone()) {
//                    SettingsDialog.info("Thread not done; killing");
//                    Thread.currentThread().stop();
//                }
//
//            } catch (InterruptedException ex) {
//                logger.log(org.apache.logging.log4j.Level.FATAL, ex);
//            }
//            return true;
//        }
        @Override
        void onDone() {
            if (ds.getActionCommand() == GetCommand.LS || ds.getActionCommand() == GetCommand.GREP) {
                lsFilesLast = saveLS(lsFilesAll);
                try {
                    showRecent();
                } catch (IOException ex) {
                    Logger.getLogger(CommandExecutor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(CommandExecutor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
            }
        }

        @Override
        void onCancel() {
            cancelExecutor();
        }

        CountDownLatch latch = null;

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

    };

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

    public abstract class QueryTaskBase extends SwingWorker<Void, String> {

        private final CommandExecutor ce;

        boolean userCancelling = false;

        public boolean isUserCancelling() {
            return userCancelling;
        }

        public void setUserCancelling(boolean userCancelling) {
            this.userCancelling = userCancelling;
        }

        abstract void onBackground() throws InterruptedException, IOException;

        abstract void onDone();

        abstract void onCancel();

        private QueryTaskBase(CommandExecutor aThis) {
            super();
            ce = aThis;
        }

        boolean myCancel(boolean mayInterruptIfRunning) {
            try {
                setUserCancelling(true);
                onCancel();
                ce.cancel();
                cancel(mayInterruptIfRunning);

                try {
                    Thread.sleep(150);

                    /*  
            may consider implementing this
            
            from https://stackoverflow.com/questions/671049/how-do-you-kill-a-thread-in-java
            
            Thread f = <A thread to be stopped>
            Method m = Thread.class.getDeclaredMethod( "stop0" , new Class[]{Object.class} );
            m.setAccessible( true );
            m.invoke( f , new ThreadDeath() );
            
                     */
                    if (!isDone()) {
                        SettingsDialog.info("Thread not done; killing");
                        Thread.currentThread().stop();
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

        private String outFile;

        public void setDisplayForm(boolean displayForm) {
            this.displayForm = displayForm;
        }

        private boolean displayForm;

        @Override
        protected void process(List<String> chunks) {
            rp.addProgress(chunks);
        }

        private RequestProgress rp = null;

        public void setRp(RequestProgress rp) {
            this.rp = rp;
        }

        String theTitle = "";
        private ISubTask finishingTask = null;
        private CountDownLatch finishLatch = null;

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
                onBackground();

                if (finishingTask != null) {
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
//                JOptionPane.showMessageDialog(null, "Query was cancelled", "Error", JOptionPane.ERROR_MESSAGE);
                logger.debug("Query was cancelled");
            } else {
                SettingsDialog.info("Command executed");
                onDone();
            }
        }

        private void setOutFile(String outFile) {
            this.outFile = outFile;
        }

    };

    private void doExecuteCmd(java.awt.Window parent1, CommandExecutor aThis) {
        QueryTaskBase tsk = null;

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
                for (DownloadSettings.AppProfile appProfile : ds.getAppProfiles()) {
                    if (appProfile.isSelected()) {
                        GetLogs.logger.debug("processing command for profile " + appProfile);
                        for (DownloadSettings.App app : appProfile.getApps()) {
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
            rp = new RequestProgress(parent1, true, tsk);
        }
        tsk.setRp(rp);
        if (ds.getActionCommand() == GetCommand.GET || ds.getActionCommand() == GetCommand.GREPGET) {

            CountDownLatch finalLatch = new CountDownLatch(1);
            tsk.setFinishLatch(finalLatch);
            tsk.setFinishingTask(
                    new ISubTask() {
                @Override
                public void task() throws InterruptedException, IOException {
                    afterActions(finalLatch);
                }
            }
            );

        }
        tsk.execute();
    }

    private void doExecuteCmd(Window parent1, CommandExecutor aThis,
            CountDownLatch latch,
            ISubTask subTask) {
        doExecuteCmd(parent1, aThis,
                latch,
                subTask, null, null);
    }

    private void doExecuteCmd(Window parent1, CommandExecutor aThis,
            CountDownLatch latch,
            ISubTask subTask,
            CountDownLatch finishLatch,
            ISubTask finishingTask) {

        QueryTaskBase tsk = new QueryTask(aThis, latch, subTask, finishLatch, finishingTask);
        if (rp == null) {
            rp = new RequestProgress(parent1, true, tsk);
        }

        tsk.setRp(rp);

        tsk.execute();

    }

    static class JTableFileEntry {

        private final AppProfile appProfile;
        private final String fileName;
        private Object errorMsg;
        private final SavedSearchStorage storage;

        private JTableFileEntry(AppProfile appProfile, SavedSearchStorage s, String fileName) {
            this.appProfile = appProfile;
            this.storage = s;
            this.fileName = fileName;
        }

        private JTableFileEntry(AppProfile appProfile, SavedSearchStorage s, String fileName, String errMessage) {
            this(appProfile, s, fileName);
            errorMsg = errMessage;
        }

        public Object getColumn(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return appProfile.getName();

                case 1:
                    return storage.ap.getName();

                case 2:
                    return storage.isLfmt();

                case 3:
                    return storage.getAppHost();

                case 4:
                    return fileName;

            }
            return null;
        }

    }

    private static HashMap<Integer, String> initCalls() {
        HashMap<Integer, String> ret1 = new HashMap<>();
        ret1.put(0, "Profile");
        ret1.put(1, "application");
        ret1.put(2, "LFMT?");
        ret1.put(3, "host");
        ret1.put(4, "file");

        return ret1;
    }

    public static final HashMap<Integer, String> fileTableColls = initCalls();

    class SavedSearchStorage {

        public AppProfile getAppProfile() {
            return appProfile;
        }

        private final boolean lcaLog;
        private final boolean lfmt;
        private final String logsDir;
        private final App ap;
        private final String appHost;
        AppProfile appProfile;

        SavedSearchStorage(AppProfile appProfile, App ap, String theAppHost, String logsDir, boolean lfmt, boolean lcaLog) {
            this.appProfile = appProfile;
            this.ap = ap;
            this.appHost = theAppHost;
            this.logsDir = logsDir;
            this.lfmt = lfmt;
            this.lcaLog = lcaLog;

        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SavedSearchStorage) {
                SavedSearchStorage obj1 = (SavedSearchStorage) obj;
                return obj1.getAp().equals(ap)
                        && obj1.getAppHost().equals(appHost)
                        && obj1.isLcaLog() == lcaLog
                        && obj1.isLfmt() == lfmt;

            } else {
                return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
            }
        }

        public boolean isLcaLog() {
            return lcaLog;
        }

        public boolean isLfmt() {
            return lfmt;
        }

        public String getLogsDir() {
            return logsDir;
        }

        public App getAp() {
            return ap;
        }

        public String getAppHost() {
            return appHost;
        }
    }

    JTableFileList lsTab = null;

    private static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

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
//                logger.debug("Thread " + Thread.currentThread() + " starting task");
                try {
                    task.task();
                } catch (InterruptedException interruptedException) {
                }
//                logger.debug("Thread " + Thread.currentThread() + " done task");

            } catch (IOException ex) {
                Logger.getLogger(CommandExecutor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            } finally {
                latch.countDown();
            }
        }

    }

}
