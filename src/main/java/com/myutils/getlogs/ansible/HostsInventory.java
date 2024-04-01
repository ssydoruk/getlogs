package com.myutils.getlogs.ansible;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.myutils.getlogs.HostAppdir;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * @author ssydo
 */
public class HostsInventory extends LinkedHashMap<String, LinkedHashMap> {

    public static HostsInventory load(File file) throws FileNotFoundException, IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(HostsInventory.class, loaderOptions));
        HostsInventory load = null;
        try (Reader r = new BufferedReader(new FileReader(file))) {
            load = yaml.load(r);
        }
        return load;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        HostsInventory load = HostsInventory.load(new File("C:\\src\\src\\GCTI\\var\\invUAT_all.yml"));
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setPrettyFlow(true);
        Yaml dumper = new Yaml(dumperOptions);
        try (BufferedWriter r = new BufferedWriter(new FileWriter("ss.yml"))) {
            r.append(dumper.dump(load));
        }
    }

    LinkedHashMap<String, LinkedHashMap> getMap(LinkedHashMap<String, LinkedHashMap> map, String key, int capacity) {
        if (map.containsKey(key))
            return map.get(key);
        else {
            LinkedHashMap<String, LinkedHashMap> ret = new LinkedHashMap<>(capacity);
            map.put(key, ret);
            return ret;
        }
    }

    public void addHost(String s, HostAppdir appDir, HashMap<String, Object> hh) {
        LinkedHashMap<String, LinkedHashMap> typeHosts = getMap(this, appDir.getAppType(), 1);
        LinkedHashMap<String, LinkedHashMap> hostsHosts = getMap(typeHosts, "hosts", 1);
        if (!hostsHosts.containsKey(s)) {
            LinkedHashMap<String, Object> hostProperties = new LinkedHashMap<>(4 + ((hh != null) ? hh.size() : 0));
            hostProperties.put("ansible_host", appDir.getHost());
            hostProperties.put("dir", appDir.getAppDir());
            hostProperties.put("ansible_become_user", appDir.getBecomeUser());
            hostProperties.put("defaultrx", appDir.getDefaultRx());
            if (hh != null)
                hh.forEach((key, val) -> {
                    hostProperties.put(key, val);
                });
            hostsHosts.put(s, hostProperties);
        }
    }

    public void dump(File file) throws IOException {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setPrettyFlow(true);
        Yaml dumper = new Yaml(dumperOptions);
        try (BufferedWriter r = new BufferedWriter(new FileWriter(file))) {
            r.append(dumper.dump(this));
        }
    }
}
