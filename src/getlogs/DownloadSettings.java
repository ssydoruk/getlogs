/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.ScreenInfo;
import Utils.TDateRange;
import Utils.UTCTimeRange;
import com.jidesoft.dialog.StandardDialog;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Stepan
 */
public class DownloadSettings {

    private String outputDir;
    private SettingsPanel.TimeProfile timeProfile;
    private String settingsFile;

    public DownloadSettings() {
        this.appProfiles = new HashSet<>();
        lfmt = true;
        prod = true;
        rangeStart = 0;
        rangeEnd = 0;
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
        return timeProfile;
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

    int showGui() {
        DownloadSettings ds = this;
        java.awt.EventQueue.invokeLater(new Runnable() {
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

        private AppProfile(String newName, AppProfile appPr) {
            this(newName);
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

    }

    static public class AppSettings implements Serializable {

        public AppSettings() {
            isGenesys = true;
        }

        private boolean isGenesys;

        private AppSettings(AppSettings settings1) {
            this.isGenesys = settings1.isIsGenesys();
        }

        public boolean isIsGenesys() {
            return isGenesys;
        }

        public void setIsGenesys(boolean isGenesys) {
            this.isGenesys = isGenesys;
        }

    }

//    private void writeObject(java.io.ObjectOutputStream out)
//            throws IOException {
//
//    }
//
//    private void readObject(java.io.ObjectInputStream in)
//            throws IOException, ClassNotFoundException {
//
//    }
//
//    private void readObjectNoData()
//            throws ObjectStreamException {
//
//    }
}
