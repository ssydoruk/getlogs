/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.util.ArrayList;

/**
 *
 * @author stepan_sydoruk
 */
class FilesToGet {

    private final AppProfile profile;
    private final App app;
    private final ArrayList<String> fileNames;

    public FilesToGet(AppProfile key, App value, String file) {
        profile = key;
        app = value;
        fileNames = new ArrayList<>();
        fileNames.add(file);
    }

    public AppProfile getProfile() {
        return profile;
    }

    public App getApp() {
        return app;
    }

    public ArrayList<String> getFileNames() {
        return fileNames;
    }

    public void addFile(String file) {
        fileNames.add(file);
    }
}
