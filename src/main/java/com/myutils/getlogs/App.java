/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import static com.myutils.getlogs.GetLogs.logger;
import java.nio.file.Paths;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.*;

/**
 *
 * @author stepan_sydoruk
 */
class App implements Comparable {

    private String name;
    private String appDir;
    private String appPrefix;
    private boolean isWindows;
    private String becomeUser;
    private String defaultRx;
    private String archiveDir;

    App(String appName, HostAppdir appParams, String baseLogDirectory) {
        this(appName, appParams.getAppDir());
        setBecomeUser(appParams.getBecomeUser());
        setDefaultRx(appParams.getDefaultRx());
        String archDir = Paths
                .get(baseLogDirectory, appParams.getBecomeUser(), "sharedmount", "${HOSTNAME}", "logs", appName)
                .toString();
        setArchiveDir((isWindows)
                ? FilenameUtils.separatorsToWindows(archDir)
                : FilenameUtils.separatorsToUnix(archDir));
    }

    public String getArchiveDir() {
        return archiveDir;
    }

    public void setArchiveDir(String archiveDir) {
        this.archiveDir = archiveDir;
    }

    public String getBecomeUser() {
        return becomeUser;
    }

    public void setBecomeUser(String becomeUser) {
        this.becomeUser = becomeUser;
    }

    public String getDefaultRx() {
        return defaultRx;
    }

    public void setDefaultRx(String defaultRx) {
        this.defaultRx = defaultRx;
    }

    public boolean isIsWindows() {
        return isWindows;
    }

    public void setIsWindows(boolean isWindows) {
        this.isWindows = isWindows;
    }

    public String getAppPrefix() {
        return appPrefix;
    }

    public void setAppPrefix(String appPrefix) {
        this.appPrefix = appPrefix;
    }

    private boolean checked;

    public App(App app) {
        this(app.getName(), app.getAppDir());

    }

    public App(String n, String appDir) {
        this(n, appDir, n);
    }

    public App(String n, String appDir, String appPrefix) {
        name = n;
        this.appDir = appDir;
        this.appPrefix = appPrefix;
        isWindows = false;

    }

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

    public String getHost() {
        return GetLogs.getHosts().lookupHost(name).getHost();
    }

    @Override
    public String toString() {
        // System.out.println("--"+name);
        try {
            if (GetLogs.isHostsVisible()) {
                return name + " @ " + GetLogs.getHosts().lookupHost(name); // To change body of generated methods,
                                                                           // choose Tools | Templates.
            } else {
                return name;
            }
        } catch (Exception e) {
            return name;
        }
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof App) {
            return this.getName().compareTo(((App) o).getName());
        }
        return 0;
    }

    public boolean correspondTo(String app, String file, String fullFileName) {
        return StringUtils.equalsIgnoreCase(getName(), app);
    }

}
