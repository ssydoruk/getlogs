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
public class HostAppdir extends Pair<String, String> {

    public HostAppdir(String key, String value) {
        super(key, value);
    }

    public String getHost() {
        return getKey();
    }

    public String getAppDir() {
        return getValue();
    }

    @Override
    public String toString() {
        return getHost();
    }

}
