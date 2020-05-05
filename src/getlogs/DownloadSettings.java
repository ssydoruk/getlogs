/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.Pair;
import Utils.UTCTimeRange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import static getlogs.GetLogs.logger;

/**
 *
 * @author Stepan
 */
public class DownloadSettings {

    private String outputDir;
    private SettingsPanel.TimeProfile timeProfile;
    private String dateSpec;
    private String timeSpec;
    private GetCommand actionCommand;
    private String grepText;
    private boolean useRSync;
    private boolean appLogs;
    private boolean lcaLogs;
    private String statusScript = "/Users/stepan_sydoruk/bin/getAppStatus";

    public String getStatusScript() {
        return statusScript;
    }

    public void setStatusScript(String statusScript) {
        this.statusScript = statusScript;
    }

    private ArrayList<Pair<String, Boolean>> afterActions;
    private ArrayList<Pair<String, Boolean>> beforeActions;

    public Collection<App> getCheckedApps() {
        ArrayList<App> ret = new ArrayList<>();
        for (AppProfile appProfile : getAppProfiles()) {
            if (appProfile.isSelected()) {
                GetLogs.logger.debug("processing command for profile " + appProfile);
                for (App app : appProfile.getApps()) {
                    GetLogs.logger.debug("processing app  " + app + ": " + app.isChecked());
                    if (app.isChecked()) {
                        ret.add(app);
                    }
                }
            }
        }
        return (ret.isEmpty()) ? null : ret;
    }

    public ArrayList<Pair<String, Boolean>> getBeforeActions() {
        return beforeActions;
    }

    public void setBeforeActions(ArrayList<Pair<String, Boolean>> beforeActions) {
        this.beforeActions = beforeActions;
    }

    public ArrayList<Pair<String, Boolean>> getAfterActions() {
        return afterActions;
    }

    public void setAfterActions(ArrayList<Pair<String, Boolean>> afterActions) {
        this.afterActions = afterActions;
    }

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
        return true;
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
        this.appProfiles = new ArrayList<>();
        lfmt = true;
        prod = true;
        rangeStart = 0;
        rangeEnd = 0;
    }

    ArrayList<LFMTHostInstance> lfmtHostInstances;

    LFMTHostInstance addLFMTInstance(String host, String instance, String baseDir) {
        LFMTHostInstance ret1 = new LFMTHostInstance(host, instance, baseDir);
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
        logger.trace("Set time: " + timeSpec);
        if (timeSpec != null && !timeSpec.isEmpty() && !GetLogs.regDateTimeSpec.matcher(timeSpec).matches()) {
            logger.error("Time is specified [" + timeSpec + "] but the format is incorrect");
        } else {
            this.timeSpec = timeSpec;
        }
    }

    void checkLFMT() {
        for (AppProfile appProfile : appProfiles) {
            if (appProfile.isSelected()) {
                GetLogs.logger.debug("checking LFMT settings for profile [" + appProfile + "]");
                appProfile.checkLFMT(lfmtHostInstances);

            }
        }

    }

    void loadLFMTs(ArrayList<Object[]> data) {
        lfmtHostInstances = new ArrayList<>();
        for (Object[] objects : data) {
            lfmtHostInstances.add(new LFMTHostInstance(objects));
        }
    }

    public static class LFMTHostInstance {

        private String host;
        private String instance;
        private String baseDir;

        public LFMTHostInstance(String host, String instance, String baseDir) {
            this.host = host;
            this.instance = instance;
            this.baseDir = baseDir;
        }

        private LFMTHostInstance(Object[] objects) {
            this(StringUtils.defaultString((String) objects[0]), StringUtils.defaultString((String) objects[1]), StringUtils.defaultString((String) objects[2]));

        }

        public String getBaseDir() {
            if (baseDir == null || baseDir.isEmpty()) {
                return "/Logs/";
            } else {
                return baseDir;
            }
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }

        public String getHost() {
            return host;
        }

        public String getInstance() {
            return instance;
        }

        @Override
        public String toString() {
            return "Host[" + host + "] instance[" + instance + "]";
        }

    }

    private ArrayList<AppProfile> appProfiles;

    public Pair<AppProfile, App> findAppProfile(String app, String file, String fullFileName) {
        for (AppProfile appProfile : appProfiles) {
            App app1 = appProfile.getApp(app, file, fullFileName);
            if (app1 != null) {
                return new Pair<>(appProfile, app1);
            }
        }
        return null;
    }

    public ArrayList<AppProfile> getAppProfiles() {
        return appProfiles;
    }

    public ArrayList<AppProfile> getAppProfilesSorted() {
        ArrayList<AppProfile> ret = new ArrayList<>(appProfiles);
        Collections.sort(ret, new AppProfile.SortByName());
        return ret;
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
            ret1 = SettingsPanel.TimeProfile.VALUE_HOURS;
        }
        return ret1;
    }

    private long rangeStart;

    private long rangeEnd;

    UTCTimeRange getTimeRange() {
        UTCTimeRange ret = new UTCTimeRange();
        if (rangeStart > 0) {
            ret.setStart(rangeStart);
        } else {
            ret.setStart(Utils.UTCTimeRange.getUtcTime(java.time.LocalDateTime.now().minusHours(3), 0));
        }

        if (rangeEnd > 0) {
            ret.setEnd(rangeEnd);
        } else {
            ret.setEnd(Utils.UTCTimeRange.getUtcTime(java.time.LocalDateTime.now(), 1));
        }
        return ret;
    }

    void setTimeRange(UTCTimeRange timeRange) {
        rangeStart = timeRange.getStart();
        rangeEnd = timeRange.getEnd();
    }

}
