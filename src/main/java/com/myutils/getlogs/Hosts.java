/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import static com.myutils.getlogs.GetLogs.logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import org.apache.commons.csv.*;


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

        try (Reader r = new BufferedReader(new FileReader(fileName))) {
            CSVParser parser = CSVParser.parse(r, CSVFormat.EXCEL.builder()
            .setSkipHeaderRecord(true).setHeader()
            .setQuote('\"').build());
        
            for (CSVRecord csvRecord : parser) {
                put(csvRecord.get("application"), new HostAppdir(csvRecord.get("host"), csvRecord.get("logpath")));
            }                
        }
        
        int cnt;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            logger.debug("Reading host/app settings from file [" + fileName + "]");
            String st;
            cnt = 0;
            while ((st = br.readLine()) != null) {
                String[] split = st.split(",");
                if (split.length > 1) {
                    // So expecting first field to be host name and the second to be application
                    if (split.length > 2) {
                        put(split[1], new HostAppdir(split[0], split[2]));
                    } else {
                        put(split[1], new HostAppdir(split[0], null));
                    }
                    cnt++;

                }
            }
        }
        logger.debug("Read " + cnt + " records");
    }

    /**
     * Search host/appDir by App
     *
     * @param app - application name
     * @return Pair<hostName, appDir>
     */
    public HostAppdir lookupHost(String app) {
        HostAppdir ret = get(app);
        if (ret == null || ret.getKey().isEmpty()) {
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
