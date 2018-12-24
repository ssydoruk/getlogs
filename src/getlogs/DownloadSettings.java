/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.Pair;
import Utils.ScreenInfo;
import Utils.TDateRange;
import Utils.UTCTimeRange;
import Utils.UnixProcess.ExtProcess;
import static Utils.Util.stripDir;
import com.jidesoft.dialog.StandardDialog;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Stepan
 */
public class DownloadSettings {

    private String outputDir;
    private SettingsPanel.TimeProfile timeProfile;
    private String settingsFile;
    private String dateSpec;
    private String timeSpec;
    private GetCommand actionCommand;
    private String grepText;
    private boolean useRSync;

    public boolean isUseRSync() {
        return useRSync;
    }

    public void setUseRSync(boolean useRSync) {
        this.useRSync = useRSync;
    }

    private boolean listFiles;

    public boolean isListFiles() {
        return listFiles;
    }

    public void setListFiles(boolean listFiles) {
        this.listFiles = listFiles;
    }

    public String getGrepText() {
        return grepText;
    }

    public void setGrepText(String grepText) {
        this.grepText = grepText;
    }

    public GetCommand getActionCommand() {
        return actionCommand;
    }

    public void setActionCommand(GetCommand actionCommand) {
        this.actionCommand = actionCommand;
    }

    public ArrayList<LFMTHostInstance> getLfmtHostInstances() {
        return lfmtHostInstances;
    }

    public DownloadSettings() {
        this.actionCommand = GetCommand.LS;
        this.lfmtHostInstances = new ArrayList();
        this.appProfiles = new HashSet<>();
        lfmt = true;
        prod = true;
        rangeStart = 0;
        rangeEnd = 0;
        settingsFile = "foundation.txt";
    }

    ArrayList<LFMTHostInstance> lfmtHostInstances;

    LFMTHostInstance addLFMTPair(String text, String text0) {
        LFMTHostInstance ret1 = new LFMTHostInstance(text, text0);
        lfmtHostInstances.add(ret1);
        return ret1;
    }

    public void executeCmd() throws IOException, InterruptedException {
        for (AppProfile appProfile : appProfiles) {
            if (appProfile.isSelected()) {
                GetLogs.logger.debug("processing command for profile " + appProfile);
                for (App app : appProfile.getApps()) {
                    if (app.isChecked()) {
                        if (isProd()) {
                            processApp(app, false);
                        }
                        if (isLfmt()) {
                            processApp(app, true);
                        }
                    }
                }
            }
        }
    }

    public void processApp(App ap, boolean isLFMT) throws IOException, InterruptedException {
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
            LFMTHostInstance lfmtHostInstance = ap.getSettings().lfmtHostInstance;
            if (lfmtHostInstance == null || lfmtHostInstance.getHost() == null) {
                GetLogs.exitHelp("LFMT not configured properly for app " + ap);
            }
            logsDir.append("/Logs/")
                    .append(lfmtHostInstance.getHost()).append("/")
                    .append(lfmtHostInstance.getHost()).append("_cls/")
                    .append(theAppHost) //                    .append("/")
                    //                    .append(ap)
                    ;
        } else {
            logsDir.append("/AppLog/GCTI");

        }

        GetLogs.logger.debug("app: " + ap + " logsDir clause: [" + logsDir + "], action: " + getActionCommand() + " lfmt:" + isLFMT);

        StringBuilder fileNameClause = getFileNameClause(ap);

        try {
            switch (getActionCommand()) {
                case GREP:
                    executeGrep(ap, theAppHost, logsDir, fileNameClause, isLFMT);
                    break;

                case GET:
                    executeGet(ap, theAppHost, logsDir, fileNameClause, isUseRSync(), isLFMT);
                    break;

                case LS:
                    executeLS(ap, theAppHost, logsDir, fileNameClause, isLFMT);
                    break;

                case GREPGET:
                    executeGrepGet(ap, theAppHost, logsDir, fileNameClause, isLFMT);
                    break;

            }
        } catch (IOException e) {
            LogManager.getLogger().error(e.getMessage());
        } catch (InterruptedException e) {
            LogManager.getLogger().error(e.getMessage());
        }
    }

    private void executeLS(App ap, String theAppHost, StringBuilder logsDir, StringBuilder fileNameClause,
            boolean isLFMT) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (GetLogs.sshUser != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", GetLogs.sshUser}));
        }

        if (isLFMT) {
            sshParams.add(ap.getSettings().getLfmtHostInstance().getHost());
        } else {
            sshParams.add(theAppHost);
        }

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

    private void executeGrepGet(App ap, String theAppHost, StringBuilder logsDir, StringBuilder fileNameClause,
            boolean isLFMT) throws IOException, InterruptedException {
        ArrayList<String> executeGrep = executeGrep(ap, theAppHost, logsDir, fileNameClause, isLFMT);
        ArrayList<String> rSyncFiles = new ArrayList<>();
        for (String fileName : executeGrep) {
            rSyncFiles.addAll(GetLogs.rSyncAddClause(stripDir(fileName)));
        }

        executeRSync(ap, theAppHost, logsDir, rSyncFiles, isLFMT);
    }

    private ArrayList<String> executeGrep(App ap, String appHost1, StringBuilder logsDir, StringBuilder fileNameClause,
            boolean isLFMT) throws IOException, InterruptedException {
        ArrayList<String> sshParams = new ArrayList<>();
        sshParams.add("ssh");
        if (GetLogs.sshUser != null) {
            sshParams.addAll(Arrays.asList(new String[]{"-l", GetLogs.sshUser}));
        }
        if (isLFMT) {
            LFMTHostInstance lfmt1 = ap.getLFMT();
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
                .append(ap)
                .append(" ")
                .append(fileClause);
        sshCmd.append(" ");
//        sshCmd.append("\\( ")
//                .append(" -iname *.log -execdir grep Trc {} \\; -true ");
//        sshCmd.append("\\)");
//        sshCmd.append(" -o ");
        ArrayList<String> matchedFiles = new ArrayList<>();
        for (Map.Entry<String, String> extUnp : GetLogs.extUnpacker.entrySet()) {
            try {
                for (String matchedFile : GetLogs.execGrep(extUnp.getKey(), extUnp.getValue(), sshParams, sshCmd, getGrepText())) {
                    if (matchedFile.startsWith(GetLogs.filePrefix)) {
                        matchedFiles.add(matchedFile.substring(GetLogs.filePrefix.length()));
                    } else {
                        GetLogs.logger.error("Not file name: [" + matchedFile + "]Ï");
                    }
                }
            } catch (IOException e) {
                LogManager.getLogger().error(e.getMessage());
            } catch (InterruptedException e) {
                LogManager.getLogger().error(e.getMessage());
            }

        }
        return matchedFiles;
    }

    private void executeRSync(App ap, String theAppHost, StringBuilder logsDir, ArrayList<String> fileNameClause,
            boolean isLFMT) throws IOException, InterruptedException {
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
            LFMTHostInstance lfmtHostInstance = ap.getLFMT();
            if (lfmtHostInstance == null) {
                return;
            }
            srcSpec.append(lfmtHostInstance.getHost()).append(":")
                    .append(logsDir).append("/").append(ap).append("/").append("");

        } else {
            srcSpec.append(theAppHost).append(":")
                    .append(logsDir).append("/").append(ap).append("/").append("");

        }

        rsyncParams.add(srcSpec.toString());

        StringBuilder dstSpec = new StringBuilder();
        dstSpec.append(getOutputDir()).append("/").append(ap);

        rsyncParams.add(dstSpec.toString());
//        LogManager.getLogger().trace("executing: " + rsyncParams);
        ExtProcess procRSync = new ExtProcess(rsyncParams);
        procRSync.startProcess();
        procRSync.waitFor();
    }

    private void executeGet(App ap, String theAppHost, StringBuilder logsDir, StringBuilder fileNameClause, boolean useRSync1,
            boolean isLFMT) throws IOException, InterruptedException {
        if (useRSync1) {
            executeRSync(ap, theAppHost, logsDir, GetLogs.rSyncAddClause(fileNameClause.toString()), isLFMT);
        } else {
            ArrayList<String> sshParams = new ArrayList<>();
            sshParams.add("ssh");
            if (GetLogs.sshUser != null) {
                sshParams.addAll(Arrays.asList(new String[]{"-l", GetLogs.sshUser}));
            }

            if (isLFMT) {
                LFMTHostInstance lfmtHostInstance = ap.getLFMT();
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
                    .append(ap)
                    .append(" ")
                    .append(fileClause);
            sshCmd.append(" -exec ");
            sshCmd.append("tar -");
            if (isProd()) {
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

    private StringBuilder getFileNameClause(App ap) {
        StringBuilder fileNameClause = new StringBuilder();

        if (!ap.getSettings().isIsGenesys()) {
            String backSlash = "";
            if (!isUseRSync()) {
                backSlash = "\\";
                fileNameClause.append("").append(backSlash).append("*").append(backSlash).append(".");
            } else {
                fileNameClause.append("*_cloud*").append("-");
            }
            fileNameClause.append(GetLogs.cloudDatePattern(dateSpec, timeSpec));

        } else {
            String backSlash = "";
            if (!isUseRSync()) {
                backSlash = "\\";
            }
            fileNameClause.append("").append(backSlash).append("*").append(backSlash).append(".");
            if (dateSpec != null && !dateSpec.isEmpty()) {
                fileNameClause.append(GetLogs.expandPattern(dateSpec, 8));
            } else {
                fileNameClause.append(StringUtils.repeat("[0-9]", 8));
            }
            fileNameClause.append("_");

            if (timeSpec != null && !timeSpec.isEmpty()) {
                fileNameClause.append(GetLogs.expandPattern(timeSpec, 6));
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

    void setCMDDate(String dateSpec) {
        if (dateSpec != null && !dateSpec.isEmpty() && !GetLogs.regDateTimeSpec.matcher(dateSpec).matches()) {
            GetLogs.exitHelp("Date is specified but the format is incorrect");
        } else {
            this.dateSpec = dateSpec;
        }
    }

    public String getDateSpec() {
        return dateSpec;
    }

    public String getTimeSpec() {
        return timeSpec;
    }

    void setCMDTime(String timeSpec) {
        if (timeSpec != null && !timeSpec.isEmpty() && !GetLogs.regDateTimeSpec.matcher(timeSpec).matches()) {
            GetLogs.exitHelp("Time is specified but the format is incorrect");
        } else {
            this.timeSpec = timeSpec;
        }
    }

    public static class LFMTHostInstance extends Pair<String, String> {

        public String getHost() {
            return getKey();
        }

        public String getInstance() {
            return getValue();
        }

        public LFMTHostInstance(String key, String value) {
            super(key, value);
        }

        @Override
        public String toString() {
            return getKey() + "-" + getValue();
        }

    }

    private HashSet<AppProfile> appProfiles;

    public HashSet<AppProfile> getAppProfiles() {
        return appProfiles;
    }

    private boolean lfmt;
    private boolean prod;
    private String hours;

    public String getHours() {
        return hours;
    }

    public void setHours(String hours) {
        this.hours = hours;
    }

    public boolean isLfmt() {
        return lfmt;
    }

    public void setLfmt(boolean lfmt) {
        this.lfmt = lfmt;
    }

    public boolean isProd() {
        return prod;
    }

    public void setProd(boolean prod) {
        this.prod = prod;
    }

    AppProfile addProfile(String name) {
        AppProfile appProfile = new AppProfile(name);
        appProfiles.add(appProfile);
        return appProfile;
    }

    boolean profileExists(String showInputDialog) {
        for (AppProfile appProfile : appProfiles) {
            if (showInputDialog.equalsIgnoreCase(appProfile.getName())) {
                return true;
            }
        }
        return false;//To change body of generated methods, choose Tools | Templates.
    }

    void removeProfile(AppProfile appPr) {
        appProfiles.remove(appPr);
    }

    AppProfile addProfile(String showInputDialog, AppProfile appPr) {
        AppProfile appProfile = new AppProfile(showInputDialog, appPr);
        appProfiles.add(appProfile);
        return appProfile;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    String getOutputDir() {
        return outputDir;
    }

    void setTimeProfile(SettingsPanel.TimeProfile _timeProfile) {
        timeProfile = _timeProfile;
    }

    public SettingsPanel.TimeProfile getTimeProfile() {
        SettingsPanel.TimeProfile ret1 = timeProfile;
        if (ret1 == null) {
            ret1 = SettingsPanel.TimeProfile.VALUE;
        }
        return ret1;
    }

    private long rangeStart;

    public String getSettingsFile() {
        return settingsFile;
    }
    private long rangeEnd;

    UTCTimeRange getTimeRange() {
        UTCTimeRange ret = new UTCTimeRange();
        if (rangeStart > 0) {
            ret.setStart(rangeStart);
        } else {
            ret.setStart(TDateRange.getUtcTime(java.time.LocalDateTime.now().minusHours(3), 0));
        }

        if (rangeEnd > 0) {
            ret.setEnd(rangeEnd);
        } else {
            ret.setEnd(TDateRange.getUtcTime(java.time.LocalDateTime.now(), 1));
        }
        return ret;
    }

    void setTimeRange(UTCTimeRange timeRange) {
        rangeStart = timeRange.getStart();
        rangeEnd = timeRange.getEnd();
    }

    void setSettingsFile(String sGUIProfile) {
        this.settingsFile = sGUIProfile;
    }

    int showGui() throws InterruptedException, InvocationTargetException {
        DownloadSettings ds = this;
        java.awt.EventQueue.invokeAndWait(new Runnable() {
            public void run() {

                SettingsDialog dlg = new SettingsDialog(ds);
                dlg.pack();
                dlg.invalidate();
                ScreenInfo.CenterWindow(dlg);
                dlg.setVisible(true);
                ret = dlg.getDialogResult();
            }
        });
        return ret;
    }

    private int ret = StandardDialog.RESULT_CANCELLED;

    public int getRet() {
        return ret;
    }

    static public class AppProfile {

        private String Name;
        private boolean selected;

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        private AppProfile(String newName, AppProfile appPr) {
            this(newName);
            setSelected(selected);
            for (App app : appPr.getApps()) {
                apps.add(new App(app));
            }
        }

        public void setName(String Name) {
            this.Name = Name;
        }

        public String getName() {
            return Name;
        }
        HashSet<App> apps = new HashSet<>();

        public HashSet<App> getApps() {
            return apps;
        }

        public AppProfile(String Name) {
            this.Name = Name;
            selected = true;
        }

        @Override
        public String toString() {
            return Name;
        }

        App addApp(String string) {
            App ret = new App(string, new AppSettings());
            apps.add(ret);
            return ret;
        }

        void removeApp(App app) {
            boolean ret = apps.remove(app);
            if (!ret) {
                System.out.println("not removed: " + app);
            }

        }
    }

    static public class App {

        private String name;
        private AppSettings settings;
        private boolean checked;

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }

        public String getName() {
            return name;
        }

        private App(App app) {
            this(app.getName(), app.getSettings());
        }

        public AppSettings getSettings() {
            return settings;
        }

        App(String sip1, AppSettings object) {
            name = sip1;
            if (object != null) {
                settings = object;
            } else {
                settings = new AppSettings();
            }
            checked = true;
        }

        @Override
        public String toString() {
//            System.out.println("--"+name);
            return name; //To change body of generated methods, choose Tools | Templates.
        }

        private LFMTHostInstance getLFMT() {
            AppSettings settings = getSettings();
            LFMTHostInstance lfmtHostInstance = null;
            if (settings != null) {
                lfmtHostInstance = settings.getLfmtHostInstance();
                if (lfmtHostInstance != null) {
                    if (lfmtHostInstance.getHost() == null || lfmtHostInstance.getInstance() == null) {
                        lfmtHostInstance = null;
                    }
                }
                if (lfmtHostInstance == null) {
                    LogManager.getLogger().error(this + ": - lfmt is selected but LFMT host not configured.");
                }
            }
            return lfmtHostInstance;
        }
    }

    static public class AppSettings implements Serializable {

        public AppSettings() {
            isGenesys = true;
        }

        private boolean isGenesys;
        LFMTHostInstance lfmtHostInstance;

        public LFMTHostInstance getLfmtHostInstance() {
            return lfmtHostInstance;
        }

        public void setLfmtHostInstance(LFMTHostInstance lfmtHostInstance) {
            this.lfmtHostInstance = lfmtHostInstance;
        }

        public AppSettings(AppSettings settings1) {
            this.isGenesys = settings1.isIsGenesys();
        }

        public boolean isIsGenesys() {
            return isGenesys;
        }

        public void setIsGenesys(boolean isGenesys) {
            this.isGenesys = isGenesys;
        }

    }

}
