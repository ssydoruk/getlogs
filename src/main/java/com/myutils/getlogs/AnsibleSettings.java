/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.getlogs;

/**
 *
 * @author ssydo
 */
public class AnsibleSettings {
    private String lsCmd;
    private String getCSV;
    private String getCmd;

    public String getLsCmd() {
        return lsCmd;
    }

    public String getGetCSV() {
        return getCSV;
    }

    public String getGetCmd() {
        return getCmd;
    }

    public AnsibleSettings(String lsCmd, String getCSV, String getCmd) {
        this.lsCmd = lsCmd;
        this.getCSV = getCSV;
        this.getCmd = getCmd;
    }

    public AnsibleSettings() {
    }
    
}
