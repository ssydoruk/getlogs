/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.Pair;
import Utils.ScreenInfo;
import Utils.UnixProcess.ExtProcess;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final Pattern regVariable = Pattern.compile("\\{([A-Z]{2,4})\\}");

    private static String cloudPattern(String fileNameRegex, String datePattern, String timePattern) {
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
    private ArrayList<JTableFileEntry> lsFiles = new ArrayList<>();
    private ArrayList<JTableFileEntry> lsFilesAll = new ArrayList<>();
    private ArrayList<JTableFileEntry> lsFilesLast = new ArrayList<>();
    private RequestProgress rp;
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
        SettingsDialog.info("* " + ds.getActionCommand() + " started");
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

            for (DownloadSettings.AppProfile appProfile : ds.getAppProfiles()) {
                if (appProfile.isSelected()) {
                    GetLogs.logger.debug("processing command for profile " + appProfile);
                    for (DownloadSettings.App app : appProfile.getApps()) {
                        if (app.isChecked()) {
                            if (ds.isProd()) {
                                lsFiles.clear();
                                executeCmd(appProfile, app, false);
                                if (!lsFiles.isEmpty()) {
                                    lsFilesAll.addAll(lsFiles);
                                    if (ds.getActionCommand() == GetCommand.GET) {
                                        executeRSync(lsFiles);
                                    }
                                }
                            }
                            if (ds.isLfmt()) {
                                lsFiles.clear();
                                executeCmd(appProfile, app, true);
                                if (!lsFiles.isEmpty()) {
                                    lsFilesAll.addAll(lsFiles);
                                    if (ds.getActionCommand() == GetCommand.GET) {
                                        executeRSync(lsFiles);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        SettingsDialog.info("Command executed");
        if (ds.getActionCommand() == GetCommand.LS || ds.getActionCommand() == GetCommand.GREP) {
            lsFilesLast = saveLS(lsFilesAll);
            if (!isText && tsk != null && !tsk.isCancelled()) {
                showRecent();
            }
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
            logsDir.append(lfmtHostInstance.getBaseDir())
                    .append(lfmtHostInstance.getInstance()).append("/")
                    .append(lfmtHostInstance.getInstance()).append("_cls/")
                    .append(theAppHost) //                    .append("/")
                    //                    .append(ap)
                    ;
        } else {
            logsDir.append(GetLogs.getProdBaseDir());

        }

        if (ds.isAppLogs()) {
            ArrayList<StringBuilder> fileNameClause = getFileNameClause(appProfile, ap, false);
            executeCmd(appProfile, ap, theAppHost, logsDir.toString(), fileNameClause, isLFMT, false);
        }
        if (ds.isLcaLogs()) {
            ArrayList<StringBuilder> fileNameClause = getFileNameClause(appProfile, ap, true);
            executeCmd(appProfile, ap, theAppHost, logsDir.toString(), fileNameClause, isLFMT, true);
        }

    }

    public SavedSearchStorage getStorage(AppProfile appProfile, App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog) {
        SavedSearchStorage _ret = new SavedSearchStorage(appProfile, ap, theAppHost, logsDir, lfmt, lcaLog);

        for (SavedSearchStorage savedSearchStorage : savedSearch) {
            if (savedSearchStorage.equals(_ret)) {
                return savedSearchStorage;
            }
        }
        savedSearch.add(_ret);

        return _ret;

    }

    ArrayList<SavedSearchStorage> savedSearch = new ArrayList<>();

    private void executeLS(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir,
            ArrayList<StringBuilder> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
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
        ExtProcess procSSH = extProcessManager.addProcess(new ExtProcessLogback(sshParams, false, true));

        procSSH.startProcess(true, true);

        int waitFor = procSSH.waitFor();
        if (waitFor != 0) {
            SettingsDialog.error("LS failed, error code: " + waitFor);
            ArrayList<String> errBuf = procSSH.getErrBuf();
            if (errBuf != null && !errBuf.isEmpty()) {
                logMessage(Level.ERROR, StringUtils.join(errBuf, " | "));
//                lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, null, logsDir, isLFMT, lcaLog), StringUtils.join(errBuf, " | ")));
//                lsTab.addRow(appProfile, ap, theAppHost, null, logsDir.toString(), isLFMT, lcaLog, StringUtils.join(errBuf, " | "));

            }

        } else {
            SettingsDialog.info("ls successful ");
            for (String string : procSSH.getSTDOut()) {
//                lsTab.addRow(appProfile, ap, theAppHost, string, logsDir.toString(), isLFMT, lcaLog);
                lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, string, logsDir, isLFMT, lcaLog), string));
            }
            ArrayList<String> errBuf = procSSH.getErrBuf();
            if (errBuf != null && !errBuf.isEmpty()) {
                lsFiles.add(new JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, null, logsDir, isLFMT, lcaLog), StringUtils.join(errBuf, " | ")));
//                lsTab.addRow(appProfile, ap, theAppHost, null, logsDir.toString(), isLFMT, lcaLog, StringUtils.join(errBuf, " | "));

            }
        }
        extProcessManager.doneProcess(procSSH);
        ((AbstractTableModel) lsTab.getModel()).fireTableDataChanged();

    }

    private class ExtProcessManager {

        HashSet<ExtProcess> processes = new HashSet<>(2);

        private ExtProcess addProcess(ExtProcess extProcess) {
            synchronized (processes) {
                processes.add(extProcess);
                return extProcess;
            }
        }

        private void doneProcess(ExtProcess procSSH) {
            synchronized (processes) {
                processes.remove(procSSH);
            }
        }

        private void cancelAll() {
            synchronized (processes) {
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
            rSyncFiles.addAll(GetLogs.rSyncAddClause(stripDir(fileName)));
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
        FileUtils.forceMkdir(new File(dstSpec.toString()));

        rsyncParams.add(dstSpec.toString());
//        LogManager.getLogger().trace("executing: " + rsyncParams);
        ExtProcessLogback procRSync = (ExtProcessLogback) extProcessManager.addProcess(new ExtProcessLogback(rsyncParams,
                true, true));
        procRSync.startProcess();
        procRSync.waitFor();
        extProcessManager.doneProcess(procRSync);
    }

    class ExtProcessLogback extends ExtProcess {

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
                        logMessage(Level.ERROR, "! " + s);

                    }
                });
            }
        }

        public ExtProcessLogback(ArrayList<String> tarParams, ExtProcess procSSH) throws IOException {
            super(tarParams, procSSH);
            initLogBack(false, false);
        }

        public ExtProcessLogback(List<String> tarParams, boolean logStdin, boolean logStdout) throws IOException {
            super(tarParams);
            initLogBack(logStdin, logStdout);
        }

    }

    private void executeGet(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, ArrayList<StringBuilder> fileNameClause, boolean useRSync1, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        if (useRSync1) {
            executeRSync(appProfile, ap, theAppHost, logsDir, GetLogs.rSyncAddClause(fileNameClause.toString()), isLFMT, lcaLog);
        } else {
            FileUtils.forceMkdir(new File(ds.getOutputDir()));
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
            ExtProcess procSSH = extProcessManager.addProcess(new ExtProcessLogback(sshParams, true, true));

            ExtProcess procTar = null;
            ArrayList<String> tarParams = new ArrayList<>();
            tarParams.add("tar");
            tarParams.add("-x");
            tarParams.add("-f");
            tarParams.add("-");

            procTar = extProcessManager.addProcess(new ExtProcessLogback(tarParams, procSSH));
            procTar.startProcess();

            procSSH.startProcess();
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
                            ret1.add(new StringBuilder(cloudPattern(suffix, datePattern, timePattern)));
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

    private void executeCmd(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, ArrayList<StringBuilder> fileNameClause, boolean isLFMT, boolean isLCALog) {
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
                        executeLS(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    }
                    break;

                case LS:
                    executeLS(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    break;

                case GREPGET:
                    executeGrepGet(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    break;

            }
        } catch (IOException e) {
            SettingsDialog.error(e.getMessage());
        } catch (InterruptedException e) {
            SettingsDialog.error(e.getMessage());
        }
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
                SettingsDialog.info("About to download " + lsTab.getSelectedFiles().size() + " files");
                executeRSync(lsTab.getSelectedFiles());
                SettingsDialog.info("Done downloading " + lsTab.getSelectedFiles().size() + " files");

            }
        }
    }

    private void executeRSync(ArrayList<JTableFileEntry> selectedRows) throws IOException, InterruptedException {
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

                rSyncFiles.addAll(GetLogs.rSyncAddClause(stripDir(row.fileName)));
            }
            for (Map.Entry<SavedSearchStorage, ArrayList<String>> entry : r.entrySet()) {
                SavedSearchStorage key = entry.getKey();
                ArrayList<String> value = entry.getValue();
                executeRSync(key.getAppProfile(), key.getAp(), key.getAppHost(), key.getLogsDir(), value, key.isLfmt(), key.isLcaLog());

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
            fileNameClause.append("*_cloud*").append("-");
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

    public class QueryTask extends SwingWorker<Void, String> {

        private final CommandExecutor ce;

        private QueryTask(CommandExecutor aThis) {
            super();
            ce = aThis;
        }

        boolean myCancel(boolean mayInterruptIfRunning) {
            ce.cancel();
            cancel(mayInterruptIfRunning);
            if (isDone()) {
                return true;
            }

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
                LogManager.getLogger().log(org.apache.logging.log4j.Level.FATAL, ex);
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

        @Override
        protected Void doInBackground() throws Exception {
            if (!isText) {
                if (lsTab == null) {
                    lsTab = new JTableFileList();
                }
                lsTab.clearTable();

            }
            lsFilesAll.clear();

            for (DownloadSettings.AppProfile appProfile : ds.getAppProfiles()) {
                if (appProfile.isSelected()) {
                    GetLogs.logger.debug("processing command for profile " + appProfile);
                    for (DownloadSettings.App app : appProfile.getApps()) {
                        if (app.isChecked()) {
                            if (ds.isProd()) {
                                lsFiles.clear();
                                executeCmd(appProfile, app, false);
                                if (!lsFiles.isEmpty()) {
                                    lsFilesAll.addAll(lsFiles);
                                    if (ds.getActionCommand() == GetCommand.GET) {
                                        executeRSync(lsFiles);
                                    }
                                }
                            }
                            if (ds.isLfmt()) {
                                lsFiles.clear();
                                executeCmd(appProfile, app, true);
                                if (!lsFiles.isEmpty()) {
                                    lsFilesAll.addAll(lsFiles);
                                    if (ds.getActionCommand() == GetCommand.GET) {
                                        executeRSync(lsFiles);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }

        @Override
        protected void done() {
            LogManager.getLogger().debug("swingworker done");
            rp.dispose();
            if (isCancelled()) {
                JOptionPane.showMessageDialog(null, "Query was cancelled", "Error", JOptionPane.ERROR_MESSAGE);
            } else {

            }
        }

        private void setOutFile(String outFile) {
            this.outFile = outFile;
        }

    };

    QueryTask tsk = null;

    private void doExecuteCmd(java.awt.Window parent1, CommandExecutor aThis) {

        tsk = new QueryTask(aThis);
        if (rp == null) {
            rp = new RequestProgress(parent1, true, tsk);
        }
        tsk.setRp(rp);
        tsk.execute();
        rp.doShow();
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
                    return storage.getAppHost();

                case 3:
                    return fileName;

                case 4:

                    return errorMsg;

            }
            return null;
        }

    }

    private static HashMap<Integer, String> initCalls() {
        HashMap<Integer, String> ret1 = new HashMap<>();
        ret1.put(0, "Profile");
        ret1.put(1, "application");
        ret1.put(2, "host");
        ret1.put(3, "file");
        ret1.put(4, "Error message");

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

}
