/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.ScreenInfo;
import Utils.UnixProcess.ExtProcess;
import static Utils.Util.stripDir;
import com.jidesoft.dialog.StandardDialog;
import getlogs.DownloadSettings.App;
import getlogs.DownloadSettings.AppProfile;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author stepan_sydoruk
 */
public class CommandExecutor {

    private DownloadSettings ds;

    private boolean isText;
    private Window parent;

    public CommandExecutor(boolean isText) {
        this.isText = isText;
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

    public void executeCmd() throws IOException, InterruptedException {
        if (!isText) {
            if (lsTab == null) {
                lsTab = new JTableFileList();
            } else {
                lsTab.clearTable();
            }

        }
        for (DownloadSettings.AppProfile appProfile : ds.getAppProfiles()) {
            if (appProfile.isSelected()) {
                GetLogs.logger.debug("processing command for profile " + appProfile);
                for (DownloadSettings.App app : appProfile.getApps()) {
                    if (app.isChecked()) {
                        if (ds.isProd()) {
                            processApp(appProfile, app, false);
                        }
                        if (ds.isLfmt()) {
                            processApp(appProfile, app, true);
                        }
                    }
                }
            }
        }
        if (!isText) {
            if (ds.getActionCommand() == GetCommand.LS) {
                showRecent();
            }
        }
        LogManager.getLogger().info("Command executed");
    }

    SettingsPanel.InfoPanel lsOutput;

    public void processApp(AppProfile appProfile, App ap, boolean isLFMT) throws IOException, InterruptedException {
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
            logsDir.append("/AppLog/GCTI");

        }

        StringBuilder fileNameClause = getFileNameClause(appProfile, ap);
        GetLogs.logger.debug("app: " + ap + " logsDir clause: [" + logsDir + "], action: " + ds.getActionCommand() + " lfmt:" + isLFMT
                + "fileName: [" + fileNameClause + "]");

        if (ds.isAppLogs()) {
            processApp(appProfile, ap, theAppHost, logsDir.toString(), fileNameClause, isLFMT, false);
        }
        if (ds.isLcaLogs()) {
            processApp(appProfile, ap, theAppHost, logsDir.toString(), fileNameClause, isLFMT, true);
        }

    }

    private void executeLS(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, StringBuilder fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (GetLogs.sshUser != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", GetLogs.sshUser}));
        }

        if (isLFMT) {
            sshParams.add(appProfile.getLFMT().getHost());
        } else {
            sshParams.add(theAppHost);
        }

        StringBuilder fileClause = new StringBuilder();
        fileClause.append("\\( -type f ");

        if (fileNameClause.length() > 0) {
            fileClause.append("-a -name ")
                    .append(fileNameClause);
        }
fileClause.append(" -a ! \\( -name \\*snapshot.log \\) ");
        fileClause.append(" \\) ");
        StringBuilder sshCmd = new StringBuilder();

        sshCmd.append("cd ").append(logsDir).append("; ");
        sshCmd.append("find ")
                .append((lcaLog) ? "lca" : ap)
                .append(" ")
                .append(fileClause);
        if (!ds.isListFiles()) {
            sshCmd.append(" -o -type d ");
        }
        sshCmd.append("-print | sort");
        sshParams.add(sshCmd.toString());
        ExtProcess procSSH = new ExtProcess(sshParams);

        procSSH.startProcess(true, true);

        int waitFor = procSSH.waitFor();
        if (waitFor != 0) {
            LogManager.getLogger().error("error code: " + waitFor);
            ArrayList<String> errBuf = procSSH.getErrBuf();
            if (errBuf != null && !errBuf.isEmpty()) {
                lsTab.addRow(appProfile, ap, theAppHost, null, logsDir.toString(), isLFMT, lcaLog, StringUtils.join(errBuf, " | "));

            }

        } else {
            LogManager.getLogger().info("ls output: " + procSSH.getSTDOut());
            for (String string : procSSH.getSTDOut()) {
                lsTab.addRow(appProfile, ap, theAppHost, string, logsDir.toString(), isLFMT, lcaLog);
            }
            ArrayList<String> errBuf = procSSH.getErrBuf();
            if (errBuf != null && !errBuf.isEmpty()) {
                lsTab.addRow(appProfile, ap, theAppHost, null, logsDir.toString(), isLFMT, lcaLog, StringUtils.join(errBuf, " | "));

            }
        }
        ((AbstractTableModel) lsTab.getModel()).fireTableDataChanged();

    }

    private void executeGrepGet(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, StringBuilder fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        ArrayList<String> executeGrep = executeGrep(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, lcaLog);
        ArrayList<String> rSyncFiles = new ArrayList<>();
        for (String fileName : executeGrep) {
            rSyncFiles.addAll(GetLogs.rSyncAddClause(stripDir(fileName)));
        }

        executeRSync(appProfile, ap, theAppHost, logsDir, rSyncFiles, isLFMT, lcaLog);
    }

    private ArrayList<String> executeGrep(AppProfile appProfile, DownloadSettings.App ap, String appHost1, String logsDir, StringBuilder fileNameClause,
            boolean isLFMT, boolean isLCA) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (GetLogs.sshUser != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", GetLogs.sshUser}));
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

        if (fileNameClause.length() > 0) {
            fileClause.append(" -name ")
                    .append(fileNameClause);
        }

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

    private void executeRSync(AppProfile appProfile,DownloadSettings.App ap, String theAppHost, String logsDir, ArrayList<String> fileNameClause, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        ArrayList<String> rsyncParams = new ArrayList<>();
        rsyncParams.add("rsync");
        rsyncParams.add("-avz");
        rsyncParams.add("-e");
        rsyncParams.add("ssh");
        rsyncParams.addAll(fileNameClause);
        rsyncParams.add("-f");
        rsyncParams.add("- **");
        StringBuilder srcSpec = new StringBuilder();
        String lfmtHost = null;

        if (GetLogs.sshUser != null) {
            srcSpec.append(GetLogs.sshUser).append("@");
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
        ExtProcess procRSync = new ExtProcess(rsyncParams);
        procRSync.startProcess();
        procRSync.waitFor();
    }

    private void executeGet(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, StringBuilder fileNameClause, boolean useRSync1, boolean isLFMT, boolean lcaLog) throws IOException, InterruptedException {
        if (useRSync1) {
            executeRSync(appProfile, ap, theAppHost, logsDir, GetLogs.rSyncAddClause(fileNameClause.toString()), isLFMT, lcaLog);
        } else {
            FileUtils.forceMkdir(new File(ds.getOutputDir()));
            Utils.FileUtils.setCurrentDirectory(ds.getOutputDir());
            ArrayList<String> sshParams = new ArrayList<>();
            sshParams.add("ssh");
            if (GetLogs.sshUser != null) {
                sshParams.addAll(Arrays.asList(new String[]{"-l", GetLogs.sshUser}));
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
            if (fileNameClause.length() > 0) {
                fileClause.append("\\( -type f ");

                fileClause.append("-a -name ")
                        .append(fileNameClause);

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

    private StringBuilder getFileNameClause(AppProfile appProfile, App ap) {
        StringBuilder fileNameClause = new StringBuilder();

        if (!appProfile.isIsGenesysName()) {
            String backSlash = "";
            if (!ds.isUseRSync()) {
                backSlash = "\\";
                fileNameClause.append("").append(backSlash).append("*").append(backSlash).append(".");
            } else {
                fileNameClause.append("*_cloud*").append("-");
            }
            fileNameClause.append(GetLogs.cloudDatePattern(ds.getDateSpec(), ds.getTimeSpec()));

        } else {
            String backSlash = "";
            if (!ds.isUseRSync()) {
                backSlash = "\\";
            }
            fileNameClause.append("").append(backSlash).append("*").append(backSlash).append(".");
            if (ds.getDateSpec() != null && !ds.getDateSpec().isEmpty()) {
                fileNameClause.append(GetLogs.expandPattern(ds.getDateSpec(), 8));
            } else {
                fileNameClause.append(StringUtils.repeat("[0-9]", 8));
            }
            fileNameClause.append("_");

            if (ds.getTimeSpec() != null && !ds.getTimeSpec().isEmpty()) {
                fileNameClause.append(GetLogs.expandPattern(ds.getTimeSpec(), 6));
            } else {
                fileNameClause.append(StringUtils.repeat("[0-9]", 6));
            }
            fileNameClause.append("_");

            fileNameClause.append(StringUtils.repeat("[0-9]", 3))
                    .append("").append(backSlash).append(".").append(backSlash).append("*");

        }
        GetLogs.logger.trace("fileName clause: [" + fileNameClause + "]");
        return fileNameClause;
    }

    private void processApp(AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String logsDir, StringBuilder fileNameClause, boolean isLFMT, boolean isLCALog) {
        try {
            switch (ds.getActionCommand()) {
                case GREP:
                    executeGrep(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    break;

                case GET:
                    executeGet(appProfile, ap, theAppHost, logsDir, fileNameClause, ds.isUseRSync(), isLFMT, isLCALog);
                    break;

                case LS:
                    executeLS(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    break;

                case GREPGET:
                    executeGrepGet(appProfile, ap, theAppHost, logsDir, fileNameClause, isLFMT, isLCALog);
                    break;

            }
        } catch (IOException e) {
            LogManager.getLogger().error(e.getMessage());
        } catch (InterruptedException e) {
            LogManager.getLogger().error(e.getMessage());
        }
    }

    void setSettingsFile(String sGUIProfile) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void showRecent() throws IOException, InterruptedException {
        if (lsTab!=null && !lsTab.isEmpty()) {
            if (lsOutput == null) {
                lsOutput = new SettingsPanel.InfoPanel(parent, "List of files", lsTab,
                        "Download %d files");
            }
            lsOutput.doShow();

            if (lsOutput.getCloseCause() == JOptionPane.OK_OPTION) {
                ArrayList<JTableFileEntry> selectedRows = lsTab.getSelectedFiles();
                if (selectedRows == null || selectedRows.size() == 0) {
                    LogManager.getLogger().info("No rows selected");
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
//                        executeRSync(key.getAp(), key.getAppHost(), key.getLogsDir(), value, key.isLfmt(), key.isLcaLog());

                    }

                }

            }
        }
    }

    class JTableFileEntry {

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

        private Object getColumn(int columnIndex) {
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

    private static final HashMap<Integer, String> fileTableColls = initCalls();

    class JTableFileModel extends AbstractTableModel {

        private ArrayList<JTableFileEntry> tabRows = new ArrayList<>();

        public JTableFileModel() {

        }

        @Override
        public int getRowCount() {
            return tabRows.size();
        }

        @Override
        public int getColumnCount() {
            return fileTableColls.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return fileTableColls.get(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            JTableFileEntry get = tabRows.get(rowIndex);
            if (get != null) {
                return get.getColumn(columnIndex);
            } else {
                return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        }

        private ArrayList<JTableFileEntry> getSelectedRows(int[] selectedRows) {
            ArrayList<JTableFileEntry> ret1 = new ArrayList<>(selectedRows.length);
            for (int row : selectedRows) {
                ret1.add(tabRows.get(row));
            }
            return ret1;
        }

        private void addRow(AppProfile appProfile, App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog) {
            tabRows.add(new JTableFileEntry(appProfile, getStorage(ap, theAppHost, searchFile, logsDir, lfmt, lcaLog), searchFile));

        }

        private void addRow(AppProfile appProfile, App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog,
                String errorMessage) {
            tabRows.add(new JTableFileEntry(appProfile, getStorage(ap, theAppHost, searchFile, logsDir, lfmt, lcaLog), searchFile));

        }

        private SavedSearchStorage getStorage(App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog) {
            SavedSearchStorage _ret = new SavedSearchStorage(ap, theAppHost, logsDir, lfmt, lcaLog);

            for (SavedSearchStorage savedSearchStorage : savedSearch) {
                if (savedSearchStorage.equals(_ret)) {
                    return savedSearchStorage;
                }
            }
            savedSearch.add(_ret);

            return _ret;

        }

        ArrayList<SavedSearchStorage> savedSearch = new ArrayList<>();
    }

    class SavedSearchStorage {

        private final boolean lcaLog;
        private final boolean lfmt;
        private final String logsDir;
        private final App ap;
        private final String appHost;

        SavedSearchStorage(App ap, String theAppHost, String logsDir, boolean lfmt, boolean lcaLog) {
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

    class JTableFileList extends JTablePopup {

        private JTableFileModel mod;

        public boolean isEmpty() {
            return mod.getRowCount() == 0;
        }

        public JTableFileList() {
            super();
            mod = new JTableFileModel();
            setModel(mod);
        }

        @Override
        void theMousePressed(MouseEvent e) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        void callingPopup() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void clearTable() {
            mod.tabRows.clear();
        }

        private ArrayList<JTableFileEntry> getSelectedFiles() {
            int[] selectedRows = getSelectedRows();
            if (selectedRows != null && selectedRows.length > 0) {
                return mod.getSelectedRows(selectedRows);
            } else {
                return null;
            }
        }

        private void addRow(AppProfile appProfile, App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog) {
            mod.addRow(appProfile, ap, theAppHost, searchFile, logsDir, lfmt, lcaLog);
        }

        private void addRow(AppProfile appProfile, App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog,
                String errorMsg) {
            mod.addRow(appProfile, ap, theAppHost, searchFile, logsDir, lfmt, lcaLog, errorMsg);
        }
    }
    JTableFileList lsTab = null;

}
