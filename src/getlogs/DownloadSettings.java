/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.Pair;
import Utils.UTCTimeRange;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

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
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private ArrayList<Pair<String, Boolean>> afterActions;
    private ArrayList<Pair<String, Boolean>> beforeActions;

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

    static public class AppProfile {

        private String Name;
        private boolean selected;
        private LFMTHostInstance lftm;
        private boolean isGenesysName;
        private HashMap<String, Boolean> nameSuffixes;

        public LFMTHostInstance getLFMT() {
            return lftm;
        }

        public void setLFMT(LFMTHostInstance lftm) {
            this.lftm = lftm;
        }

        public boolean isIsGenesysName() {
            return isGenesysName;
        }

        public void setIsGenesysName(boolean isGenesysName) {
            this.isGenesysName = isGenesysName;
        }

        public HashMap<String, Boolean> getNameSuffixes() {
            return nameSuffixes;
        }

        public void setNameSuffixes(ArrayList<Pair<String, Boolean>> nameSuffixes) {
            this.nameSuffixes = new HashMap<>(nameSuffixes.size());
            for (Pair<String, Boolean> sfx : nameSuffixes) {
                this.nameSuffixes.put(sfx.getKey(), sfx.getValue());
            }
        }

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

        App addApp(String app, String appDir) {
            App ret = new App(app, appDir);
            apps.add(ret);
            return ret;
        }

        void removeApp(App app) {
            boolean ret = apps.remove(app);
            if (!ret) {
                System.out.println("not removed: " + app);
            }

        }

        private void checkLFMT(ArrayList<LFMTHostInstance> lfmtHostInstances) {
            LFMTHostInstance lfmt1 = getLFMT();
            if (lfmt1 != null) {
                for (LFMTHostInstance lfmtHostInstance : lfmtHostInstances) {
                    if (lfmtHostInstance.getHost() != null && lfmt1.getHost() != null
                            && lfmtHostInstance.getInstance() != null && lfmt1.getInstance() != null
                            && lfmtHostInstance.getHost().equalsIgnoreCase(lfmt1.getHost())
                            && lfmtHostInstance.getInstance().equalsIgnoreCase(lfmt1.getInstance())) {
                        return;
                    }
                }

            }
            LFMTHostInstance newLFMT = null;
            if (lfmtHostInstances != null && !lfmtHostInstances.isEmpty()) {
                newLFMT = lfmtHostInstances.get(0);
            }
            logger.error("Incorrect LFMT setting for " + this.toString() + "; was: " + ((lfmt1 == null) ? "null" : lfmt1) + " changed to "
                    + ((newLFMT == null) ? "null" : newLFMT));
            setLFMT(newLFMT);

        }

        private final static Pattern ptGenesysTimestamp = Pattern.compile("^(.+)\\.(\\d{4})(\\d{2})(\\d{2})_(\\d{2})(\\d{2})(\\d{2})_(\\d{3})");

        public boolean fitsTimeRange(String string, UTCTimeRange timeRange) {
            if (!isGenesysName) {
                return true;
            } else {
                Matcher m;
                if ((m = ptGenesysTimestamp.matcher(string)).find()) {
                    ZonedDateTime fileZoneDateTime = ZonedDateTime.of(
                            Integer.parseInt(m.group(2)),
                            Integer.parseInt(m.group(3)),
                            Integer.parseInt(m.group(4)),
                            Integer.parseInt(m.group(5)),
                            Integer.parseInt(m.group(6)),
                            Integer.parseInt(m.group(7)),
                            Integer.parseInt(m.group(8)) * 1000000,
                            Utils.UTCTimeRange.zoneId);
                    long utcTime = Utils.UTCTimeRange.getUtcTime(fileZoneDateTime.toLocalDateTime(), 0);
                    logger.debug("file [" + string + "] utcTime:" + utcTime + "timeRange:" + timeRange + "(utcTime > timeRange.getStart()): " + (utcTime > timeRange.getStart()) + " (utcTime < timeRange.getEnd()):" + (utcTime < timeRange.getEnd()));
                    if ((utcTime <= timeRange.getStart()) || (utcTime >= timeRange.getEnd())) {
                        return false;
                    }
                }
                return true;
            }
        }

        public Pair<Long, String> getFileNameTime(String string) {
            if (isGenesysName) {
                Matcher m;
                try {
                    if ((m = ptGenesysTimestamp.matcher(string)).find()) {
                        ZonedDateTime fileZoneDateTime = ZonedDateTime.of(
                                Integer.parseInt(m.group(2)),
                                Integer.parseInt(m.group(3)),
                                Integer.parseInt(m.group(4)),
                                Integer.parseInt(m.group(5)),
                                Integer.parseInt(m.group(6)),
                                Integer.parseInt(m.group(7)),
                                Integer.parseInt(m.group(8)) * 1000000,
                                Utils.UTCTimeRange.zoneId);
                        return new Pair(Utils.UTCTimeRange.getUtcTime(fileZoneDateTime.toLocalDateTime(), 0), m.group(1));
                    }
                } catch (DateTimeException e) {
                    logger.error("error parsing timestamp name for [" + string + "]", e);
                }

            }
            return null;
        }

        public static class SortByName implements Comparator<AppProfile> {

            public int compare(AppProfile a, AppProfile b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        }

    }

    static public class App implements Comparable {

        private String name;
        private String appDir;

        public String getAppDir() {
            if (appDir != null && !appDir.isEmpty()) {
                return appDir;
            } else {
                return name;
            }
        }

        public void setAppDir(String appDir) {
            this.appDir = appDir;
        }
        private boolean checked;

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            logger.debug(this.toString() + " set checked " + checked);
            this.checked = checked;
        }

        public String getName() {
            return name;
        }

        public App(App app) {
            this(app.getName(), app.getAppDir());

        }

        public App(String n, String appDir) {
            name = n;
            this.appDir = appDir;

        }

        @Override
        public String toString() {
//            System.out.println("--"+name);
            return name; //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof App) {
                return this.getName().compareTo(((App) o).getName());
            }
            return 0;
        }

    }

}
