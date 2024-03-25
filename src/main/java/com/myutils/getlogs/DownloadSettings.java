/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.*;
import Utils.swing.*;
import static com.myutils.getlogs.GetLogs.logger;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

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
    private boolean zipDest;
    private boolean parserWhileDownload;

    public boolean isParserWhileDownload() {
        return parserWhileDownload;
    }

    public void setParserWhileDownload(boolean parserWhileDownload) {
        this.parserWhileDownload = parserWhileDownload;
    }
    

    public boolean isZipDest() {
        return zipDest;
    }

    public void setZipDest(boolean zipDest) {
        this.zipDest = zipDest;
    }
    private boolean lcaLogs;
    private String statusScript = "/Users/stepan_sydoruk/bin/getAppStatus";

    private ArrayList<Pair<String, Boolean>> afterActions;
    private ArrayList<Pair<String, Boolean>> beforeActions;
    private boolean listFiles;
    ArrayList<LFMTHostInstance> lfmtHostInstances;
    private ArrayList<AppProfile> appProfiles;
    private ArrayList<LoginProfile> loginProfiles;

    private boolean lfmt;
    private boolean prod;
    private String hours;
    private long rangeStart;
    private long rangeEnd;
    
    private AnsibleSettings ansible;

    private int maxThreads;

    public int getMaxThreads() {

        return (maxThreads > 0) ? maxThreads : Integer.MAX_VALUE;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public DownloadSettings() {
        this.appLogs = true;
        this.lcaLogs = false;
        this.actionCommand = GetCommand.LS;
        this.lfmtHostInstances = new ArrayList();
        this.appProfiles = new ArrayList<>();
        this.loginProfiles = new ArrayList<>();
        this.ansible = new AnsibleSettings(
                "./getfiles.py ls --inventory ${INV}/invUAT_all.yml --csvdir /Users/ssydoruk/work/getfiles/csv --files $FILES",
                " ./getfiles.py csv --destdir $DESTDIR  --inventory ${INV}/invUAT_all.yml $CSVFILES", 
                "./getfiles.py last --destdir $DESTDIR --inventory ${INV}/invUAT_all.yml --files $FILES");
        lfmt = true;
        prod = true;
        rangeStart = 0;
        rangeEnd = 0;
    }

    public ArrayList<LoginProfile> getLoginProfiles() {
        return loginProfiles;
    }

    public String getStatusScript() {
        return statusScript;
    }

    public void setStatusScript(String statusScript) {
        this.statusScript = statusScript;
    }

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

    protected GetCommand getActionCommand() {
        return actionCommand;
    }

    protected void setActionCommand(GetCommand actionCommand) {
        this.actionCommand = actionCommand;
    }

    public ArrayList<LFMTHostInstance> getLfmtHostInstances() {
        return lfmtHostInstances;
    }

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

    void loadLFMTs(ArrayList<EditableValue[]> data) {
        lfmtHostInstances = new ArrayList<>();
        for (Object[] objects : data) {
            lfmtHostInstances.add(new LFMTHostInstance(objects));
        }
    }

    int getTotalApps() {
        int ret = 0;
        for (AppProfile appProfile : getAppProfiles()) {
            for (App app : appProfile.getApps()) {
                ret++;
            }
        }
        return ret;
    }

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

    void setLoginProfiles(ArrayList<EditableValue[]> data) {
        if (loginProfiles == null) {
            loginProfiles = new ArrayList<>();
        } else {
            loginProfiles.clear();
        }
        for (EditableValue[] objects : data) {
            LoginProfile lp = new LoginProfile();
            if (objects.length > 0) {
                lp.setName((String) objects[0].getValue());
            }
            if (objects.length >= 1) {
                lp.setUsername((String) objects[1].getValue());
            }
            if (objects.length >= 2) {
                lp.setPassword(new PasswordValue( objects[2].toString()));
            }
            loginProfiles.add(lp);
        }
    }

    String getUser(AppProfile appProfile) throws ConfigException {
        for (LoginProfile loginProfile : loginProfiles) {
            if (loginProfile.getName().equalsIgnoreCase(appProfile.getLoginProfile())) {
                return loginProfile.getUsername();
            }
        }
        throw new ConfigException("Not found login profile for " + appProfile.getLoginProfile());
    }

    String getPassword(AppProfile appProfile) throws ConfigException {
        for (LoginProfile loginProfile : loginProfiles) {
            if (loginProfile.getName().equalsIgnoreCase(appProfile.getLoginProfile())) {
                return loginProfile.getPassword();
            }
        }
        throw new ConfigException("Not found login profile for " + appProfile.getLoginProfile());
    }

    public static class LFMTHostInstance {

        private final String host;
        private final String instance;
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

}
