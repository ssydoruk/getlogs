/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Utils.Pair;

/**
 *
 * @author stepan_sydoruk
 */
public class HostAppdir {

    private String host;
    private String appDir;
    private String becomeUser;
    private String defaultRx;
    private String appType;
    private List<String> archiveDir;

    public List<String> getArchiveDir() {
        return archiveDir;
    }

    public String getAppType() {
        return appType;
    }

    public HostAppdir(String _host, String _appDir, String _becomeUser, String _defaultRx) {
        this(_host, _appDir, _becomeUser);
        defaultRx = _defaultRx;
    }

    public HostAppdir(String _host, String _appDir, String _becomeUser, String _defaultRx, String _appType) {
        this(_host, _appDir, _becomeUser, _defaultRx);
        appType = _appType;
    }

    public HostAppdir(String _host, String _appDir, String _becomeUser, String _defaultRx, String _appType,
            String[] _archiveDir) {
        this(_host, _appDir, _becomeUser, _defaultRx, _appType);
        archiveDir.addAll(Arrays.asList(_archiveDir));
    }

    public String getBecomeUser() {
        return becomeUser;
    }

    public String getDefaultRx() {
        return defaultRx;
    }

    public HostAppdir(String _host, String _appDir, String _becomeUser) {
        this(_host, _appDir);
        becomeUser = _becomeUser;
    }

    public HostAppdir(String _host, String _appDir) {
        host = _host;
        appDir = _appDir;
        archiveDir = new ArrayList<String>();
    }

    public String getHost() {
        return host;
    }

    public String getAppDir() {
        return appDir;
    }

    @Override
    public String toString() {
        return getHost();
    }

}
