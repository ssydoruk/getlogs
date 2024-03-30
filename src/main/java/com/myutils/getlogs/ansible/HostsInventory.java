/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.getlogs.ansible;

import com.myutils.getlogs.HostAppdir;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 *
 * @author ssydo
 */
public class HostsInventory extends LinkedHashMap<String,HostsEntry> {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml( new Constructor(HostsInventory.class, loaderOptions));
        HostsInventory load ;
        try (Reader r = new BufferedReader(new FileReader("C:\\src\\src\\GCTI\\var\\invUAT_all.yml"))) {
            load = yaml.load(r);
            System.out.println("-1-");
//            l.forEach((type, apps) -> {
//                ((LinkedHashMap<String, LinkedHashMap<String, Map>>) apps).get("hosts").forEach((app, vals) -> {
//                    
//                });
//            });
        }        
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setPrettyFlow(true);
        Yaml dumper = new Yaml(dumperOptions);
        try(BufferedWriter r = new BufferedWriter(new FileWriter("ss.yml"))){
            r.append(dumper.dump(load));
        }
    }
}
