/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author ssydoruk
 */
enum LSUsing {
    Bash("ls"),
    Ansible("grepget"),
    Default(""),;

    static String showAll() {
        StringBuilder ret = new StringBuilder();
        for (LSUsing value : LSUsing.values()) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(value);
        }
        return ret.toString();
    }

    private final String name;

    private static final Map<String, LSUsing> ENUM_MAP;

    LSUsing(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // Build an immutable map of String name to enum pairs.
    // Any Map impl can be used.
    static {
        Map<String, LSUsing> map = new ConcurrentHashMap<>();
        for (LSUsing instance : LSUsing.values()) {
            map.put(instance.getName(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static LSUsing get(String name) {
        return ENUM_MAP.get(name);
    }
}
