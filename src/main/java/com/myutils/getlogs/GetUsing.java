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
enum GetUsing {
    RSync("ls"),
    SSH("get"),
    Ansible("grep"),
    RSyncFallbackAnsible("grepget"),
    Default(""),;

    static String showAll() {
        StringBuilder ret = new StringBuilder();
        for (GetUsing value : GetUsing.values()) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(value);
        }
        return ret.toString();
    }

    private final String name;

    private static final Map<String, GetUsing> ENUM_MAP;

    GetUsing(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // Build an immutable map of String name to enum pairs.
    // Any Map impl can be used.
    static {
        Map<String, GetUsing> map = new ConcurrentHashMap<>();
        for (GetUsing instance : GetUsing.values()) {
            map.put(instance.getName(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static GetUsing get(String name) {
        return ENUM_MAP.get(name);
    }
}
