/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import static getlogs.GetLogs.logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author stepan_sydoruk
 */
class App implements Comparable {

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

    Logger logger=GetLogs.getLogger();
    
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

    public String getHost() {
        return GetLogs.getHosts().lookupHost(name).getHost();
    }

    @Override
    public String toString() {
//            System.out.println("--"+name);
        try {
            if (GetLogs.isHostsVisible()) {
                return name + " @ " + GetLogs.getHosts().lookupHost(name); //To change body of generated methods, choose Tools | Templates.
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
        return StringUtils.equals(getName(), app);
    }

}
