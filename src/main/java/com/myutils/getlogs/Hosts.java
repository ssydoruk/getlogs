/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import static com.myutils.getlogs.GetLogs.logger;
import com.myutils.getlogs.ansible.HostsInventory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.csv.*;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author stepansydoruk class contain hosts configuration as read from file.
 *
 *         Lines in the file: host,app[,applicationDirectory]
 *
 */
class Hosts extends HashMap<String, HostAppdir> {

    Hosts(String fileName) throws FileNotFoundException, IOException {
        File file = new File(fileName);

        if (StringUtils.containsAny(FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase(),
                "yml", "yaml")) {
            Yaml yaml = new Yaml();
            try (Reader r = new BufferedReader(new FileReader(fileName))) {
                LinkedHashMap<String, Object> load;
                load = yaml.load(r);
                load.forEach((type, apps) -> {
                    ((LinkedHashMap<String, LinkedHashMap<String, Map>>) apps).get("hosts").forEach((app, vals) -> {
                        put(app,
                                new HostAppdir(
                                        vals.get("ansible_host").toString(),
                                        vals.get("dir").toString(),
                                        vals.get("ansible_become_user").toString(),
                                        vals.get("defaultrx").toString()));
                    });
                });
            }
        } else { // reading as csv
            try (Reader r = new BufferedReader(new FileReader(fileName))) {
                CSVParser parser = CSVParser.parse(r, CSVFormat.EXCEL.builder()
                        .setSkipHeaderRecord(true).setHeader()
                        .setQuote('\"').build());

                for (CSVRecord csvRecord : parser) {
                    put(csvRecord.get("application"), new HostAppdir(csvRecord.get("host"), csvRecord.get("logpath")));
                }
            }
        }

        logger.debug("Read " + size() + " records");
    }

    Hosts(HostsInventory inventory) {
        for (Entry<String, LinkedHashMap> entry : inventory.entrySet()) {
            String hostType = entry.getKey();
            LinkedHashMap<String, LinkedHashMap> host = entry.getValue();
            LinkedHashMap<String, LinkedHashMap> hosts = host.get("hosts");
            for (Entry<String, LinkedHashMap> hostEntry : hosts.entrySet()) {
                String hostName = hostEntry.getKey();
                LinkedHashMap<String, String> vals = hostEntry.getValue();
                put(hostName,
                        new HostAppdir(
                                vals.get("ansible_host").toString(),
                                vals.get("dir").toString(),
                                vals.get("ansible_become_user").toString(),
                                vals.get("defaultrx").toString(),
                                hostType,
                                vals.get("archive_dir").toString()
                                ));
                             
                
            }
//            for (HostEntry host : hosts) {
//                put(host.getKey(),
//                        new HostAppdir(
//                                host.getValue().getAnsible_host(),
//                                host.getValue().getDir(),
//                                host.getValue().getAnsible_become_user(),
//                                host.getValue().getDefaultrx()));                
//            }
            
        }
    }

    /**
     * Search host/appDir by App
     *
     * @param app - application name
     * @return Pair<hostName, appDir>
     */
    public HostAppdir lookupHost(String app) {
        HostAppdir ret = get(app);
        if (ret == null || ret.getHost().isEmpty()) {
            ret = null;
        }
        return ret;
    }

    String getAppDir(String app) {

        HostAppdir had = lookupHost(app);
        if (had != null) {
            return had.getAppDir();
        } else {
            return null;
        }
    }

}
