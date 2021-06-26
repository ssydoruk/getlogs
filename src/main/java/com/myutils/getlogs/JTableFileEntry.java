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
class JTableFileEntry extends JTableFileEntryGeneral {

    public static final HashMap<Integer, String> fileTableColls = initCalls();

    private static HashMap<Integer, String> initCalls() {
        HashMap<Integer, String> ret1 = new HashMap<>();
        ret1.put(0, "Profile");
        ret1.put(1, "application");
        ret1.put(2, "LFMT?");
        ret1.put(3, "host");
        ret1.put(4, "file");
        ret1.put(5, "size");
        ret1.put(6, "size MB");

        return ret1;
    }

    public static String getColumnName(Integer key) {
        return fileTableColls.get(key);
    }

    private final SavedSearchStorage storage;

    public JTableFileEntry(AppProfile appProfile, SavedSearchStorage s, String fileName) {
        super(new FilesToGet(appProfile, s.getAp(), fileName), fileName);
        this.storage = s;
    }

    public SavedSearchStorage getStorage() {
        return storage;
    }

    @Override
    public Object getColumn(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return getAppProfile().getName();

            case 1:
                return getAp().getName();

            case 2:
                return storage.isLfmt();

            case 3:
                return storage.getAppHost();

            case 4:
                return getFileName();

        }
        return null;
    }

}
