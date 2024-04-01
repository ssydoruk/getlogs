/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ssydoruk
 */
public enum RegexType {
    Default(1),
    ShellRegex(2),
    Any(3);


    private final int value;

    RegexType(int value) {
        this.value = value;
    }


    public int getValue() {
        return value;
    }

}
