/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import com.myutils.getlogs.LogFiles.LogFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Stepan
 */
public class LogFiles extends HashMap<String, ArrayList<LogFile>> {

    LogFiles(String fileName) throws FileNotFoundException, IOException {
        File file = new File(fileName);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String st;
            while ((st = br.readLine()) != null) {
                if (st != null && !st.isEmpty()) {
                    LogFile logFile = new LogFile(st);
                    ArrayList<LogFile> get = get(logFile.getAppName());
                    if (get == null) {
                        get = new ArrayList<>();
                        put(logFile.getAppName(), get);
                    }
                    get.add(logFile);
                }
            }
        }
    }

    public static class LogFile {

        private final String lfmtName;
        private String targetFile;
        private String appName;

        public String getLfmtName() {
            return lfmtName;
        }

        public String getTargetFile() {
            return targetFile;
        }

        public String getAppName() {
            return appName;
        }

        private LogFile(String st) {
            lfmtName = st;
            String[] split = st.split("/");
            if (split != null && split.length > 1) {
                StringBuilder s = new StringBuilder();
                appName = split[split.length - 2];
                s.append(appName).append(File.separator)
                        .append(split[split.length - 1]);
                targetFile = s.toString();
            }
        }

    }

}
