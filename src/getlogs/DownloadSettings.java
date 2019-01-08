/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.Pair;
import Utils.TDateRange;
import Utils.UTCTimeRange;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
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
    private boolean appLogs;
    private boolean lcaLogs;

    public boolean isAppLogs() {
        return appLogs;
    }

    public void setAppLogs(boolean appLogs) {
        this.appLogs = appLogs;
    }

    public boolean isLcaLogs() {
        return lcaLogs;
    }

    public void setLcaLogs(boolean lcaLogs) {
        this.lcaLogs = lcaLogs;
    }

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
        this.appLogs = true;
        this.lcaLogs = false;
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

    void checkLFMT() {
        for (AppProfile appProfile : appProfiles) {
            if (appProfile.isSelected()) {
                GetLogs.logger.debug("checking LFMT settings for profile [" + appProfile + "]");
                for (App app : appProfile.getApps()) {
                    app.checkLFMT(lfmtHostInstances);
                }
            }
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
            return "Host[" + getKey() + "] instance[" + getValue() + "]";
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

        public LFMTHostInstance getLFMT() {
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

        private void checkLFMT(ArrayList<LFMTHostInstance> lfmtHostInstances) {
            LFMTHostInstance lfmt1 = getLFMT();
            if (lfmt1 != null) {
                for (LFMTHostInstance lfmtHostInstance : lfmtHostInstances) {
                    if (lfmtHostInstance.getHost().equalsIgnoreCase(lfmt1.getHost())
                            && lfmtHostInstance.getInstance().equalsIgnoreCase(lfmt1.getInstance())) {
                        return;
                    }
                }

            }
            LogManager.getLogger().error("Incorrect LFMT setting for " + this.toString() + "; was: " + ((lfmt1 == null) ? "null" : lfmt1) + " changed to " + lfmtHostInstances.get(0));
            getSettings().setLfmtHostInstance(lfmtHostInstances.get(0));

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
