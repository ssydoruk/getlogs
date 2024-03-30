/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.getlogs.ansible;

/**
 *
 * @author ssydo
 */
public class Host {

    String ansible_become_user;
    String ansible_host;
    String defaultrx;
    String dir;

    public String getAnsible_become_user() {
        return ansible_become_user;
    }

    public void setAnsible_become_user(String ansible_become_user) {
        this.ansible_become_user = ansible_become_user;
    }

    public String getAnsible_host() {
        return ansible_host;
    }

    public void setAnsible_host(String ansible_host) {
        this.ansible_host = ansible_host;
    }

    public String getDefaultrx() {
        return defaultrx;
    }

    public void setDefaultrx(String defaultrx) {
        this.defaultrx = defaultrx;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
