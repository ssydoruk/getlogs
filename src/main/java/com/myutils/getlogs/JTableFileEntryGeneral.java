/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.util.HashMap;

/**
 *
 * @author stepan_sydoruk
 */
class JTableFileEntryGeneral {

    private final String fileName;
    private FilesToGet filesToGet;

    JTableFileEntryGeneral(FilesToGet filesToGet, String fileName) {
        this.fileName = fileName;
        this.filesToGet = filesToGet;
    }

    public AppProfile getAppProfile() {
        return filesToGet.getProfile();
    }

    public String getFileName() {
        return fileName;
    }

    public App getAp() {
        return filesToGet.getApp();
    }

    public FilesToGet getFilesToGet() {
        return filesToGet;
    }


    public Object getColumn(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return getAppProfile().getName();

            case 1:
                return getAp().getName();

            case 2:
                return getFileName();

        }
        return null;
    }

    private static HashMap<Integer, String> initCalls() {
        HashMap<Integer, String> ret1 = new HashMap<>();
        ret1.put(0, "Profile");
        ret1.put(1, "application");
        ret1.put(2, "file");

        return ret1;
    }

    public static final HashMap<Integer, String> fileTableColls = initCalls();

    public static String getColumnName(Object key) {
        return fileTableColls.get(key);
    }

}
