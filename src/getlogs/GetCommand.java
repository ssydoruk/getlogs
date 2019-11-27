/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author ssydoruk
 */
enum GetCommand {
    LS("ls"),
    GET("get"),
    GREP("grep"),
    GREPGET("grepget"),
    Unknown(""),;

    static String showAll() {
        StringBuilder ret = new StringBuilder();
        for (GetCommand value : GetCommand.values()) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(value);
        }
        return ret.toString();
    }

    private String name;

    private static final Map<String, GetCommand> ENUM_MAP;

    GetCommand(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // Build an immutable map of String name to enum pairs.
    // Any Map impl can be used.
    static {
        Map<String, GetCommand> map = new ConcurrentHashMap<String, GetCommand>();
        for (GetCommand instance : GetCommand.values()) {
            map.put(instance.getName(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static GetCommand get(String name) {
        return ENUM_MAP.get(name);
    }
}
