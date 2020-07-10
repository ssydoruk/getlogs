/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

/**
 *
 * @author stepan_sydoruk
 */
class SavedSearchStorage {

    public AppProfile getAppProfile() {
        return appProfile;
    }

    private final boolean lcaLog;
    private final boolean lfmt;
    private final String logsDir;
    private final App ap;
    private final HostAppdir appHost;
    AppProfile appProfile;

    SavedSearchStorage(AppProfile appProfile, App ap, HostAppdir theAppHost, String logsDir, boolean lfmt, boolean lcaLog) {
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

    public HostAppdir getAppHost() {
        return appHost;
    }
}
