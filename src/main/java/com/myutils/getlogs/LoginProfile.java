/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.swing.*;

/**
 *
 * @author stepan_sydoruk
 */
public class LoginProfile {
    private String name;
    private String username;
    private PasswordValue password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PasswordValue getPassword() {
        return password;
    }

    public void setPassword(PasswordValue password) {
        this.password = password;
    }
}
