/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.Pair;
import Utils.UTCTimeRange;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.myutils.getlogs.GetLogs.logger;

/**
 *
 * @author stepan_sydoruk
 */
public final class AppProfile {

    private String Name;
    private boolean selected;
    private DownloadSettings.LFMTHostInstance lftm;
    private boolean isGenesysName;
    private HashMap<String, Boolean> nameSuffixes;

    public DownloadSettings.LFMTHostInstance getLFMT() {
        return lftm;
    }

    public void setLFMT(DownloadSettings.LFMTHostInstance lftm) {
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

    public AppProfile(String newName, AppProfile appPr) {
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

    public void checkLFMT(ArrayList<DownloadSettings.LFMTHostInstance> lfmtHostInstances) {
        DownloadSettings.LFMTHostInstance lfmt1 = getLFMT();
        if (lfmt1 != null) {
            for (DownloadSettings.LFMTHostInstance lfmtHostInstance : lfmtHostInstances) {
                if (lfmtHostInstance.getHost() != null && lfmt1.getHost() != null
                        && lfmtHostInstance.getInstance() != null && lfmt1.getInstance() != null
                        && lfmtHostInstance.getHost().equalsIgnoreCase(lfmt1.getHost())
                        && lfmtHostInstance.getInstance().equalsIgnoreCase(lfmt1.getInstance())) {
                    return;
                }
            }

        }
        DownloadSettings.LFMTHostInstance newLFMT = null;
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

    protected App getApp(String app, String file, String fullFileName) {
        for (App app1 : apps) {
            if (app1.correspondTo(app, file, fullFileName)) {
                return app1;
            }
        }
        return null;
    }

    public static class SortByName implements Comparator<AppProfile> {

        @Override
        public int compare(AppProfile a, AppProfile b) {
            return a.getName().compareToIgnoreCase(b.getName());
        }
    }

}
