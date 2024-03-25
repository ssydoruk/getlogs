/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

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

    public HostAppdir(String _host, String _appDir, String _becomeUser, String _defaultRx) {
        this(_host, _appDir, _becomeUser);
        defaultRx = _defaultRx;
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
