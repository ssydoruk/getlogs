/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.swing.*;
import org.jasypt.util.text.*;

/**
 *
 * @author stepan_sydoruk
 */
public class LoginProfile {

    private static StrongTextEncryptor getTextEncryptor() {
        StrongTextEncryptor ret = new StrongTextEncryptor();
        ret.setPassword(LoginProfile.class.toString());
        return ret;
    }
    private String name;
    private String username;
    private String password;

    private static final StrongTextEncryptor textEncryptor = getTextEncryptor();

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

    public String getPassword() {
        return textEncryptor.decrypt(password);
    }

    public void setPassword(PasswordValue password) {
        this.password = textEncryptor.encrypt(password.getValue());
    }
}
