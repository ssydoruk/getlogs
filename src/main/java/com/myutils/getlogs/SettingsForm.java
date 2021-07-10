/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.*;
import com.jidesoft.dialog.*;
import static com.myutils.getlogs.GetLogs.logger;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.*;
import javax.swing.*;

/**
 *
 * @author Stepan
 */
public final class SettingsForm extends JFrame {

    private static LogWindow lw;

    static void info(String str) {
        logger.info(str);
        lw.addMsg(str);

    }

    static void error(String str) {
        logger.error(str);
        lw.addMsg(str);
    }

    private final SettingsPanel settingsPanel;
    private final CommandExecutor ce;
    private JButton jbRun;
    private JToggleButton showLog;

    public SettingsForm(DownloadSettings ds, String guiProfile) {

        super();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        settingsPanel = new SettingsPanel(ds, this);
        ce = new CommandExecutor(this);
        lw = new LogWindow(this);

        getContentPane().add(settingsPanel);
        getContentPane().add(createButtonPanel());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                closeForm();
            }

            @Override
            public void windowClosing(WindowEvent e) {

            }
        });

        addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                if ((e.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
                    if (lw.isVisible()) {
                        lw.setVisible(false);
                        showLog.setSelected(false);
                    }
                } else if (((e.getNewState() & Frame.NORMAL) == Frame.NORMAL)) {
                    lw.setVisible(true);
                    showLog.setSelected(true);

                }
            }
        });
        setTitle("GetLogs" + "(" + guiProfile + ")");
    }

    public void setJBRunEnabled(boolean b) {
        if (jbRun != null) {
            jbRun.setEnabled(b);
            jbRun.repaint();
        }
    }

    public CommandExecutor getCe() {
        return ce;
    }

    public ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = new ButtonPanel();

        showLog = new JToggleButton(new AbstractAction("log window") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logWindowToggled(e);
//                    cancelButtonAction(e);
            }

        });
        buttonPanel.addButton(showLog);
        showLog.doClick();
        JButton cancelButton = new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeForm();
            }

        });
        buttonPanel.addButton(cancelButton);

        lw.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                showLog.setSelected(false);
                super.windowClosing(e);
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                super.windowGainedFocus(e);
            }

        });

        JButton pasteFiles = new JButton(new AbstractAction("Paste files") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    pasteFiles();
//                    cancelButtonAction(e);
                } catch (IOException ex) {
                    Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });

        buttonPanel.addButton(pasteFiles);

        JButton lastLSButton = new JButton(new AbstractAction("recent list") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    showRecent();
//                    cancelButtonAction(e);
                } catch (IOException ex) {
                    Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });
        buttonPanel.addButton(lastLSButton);

        JButton uncheckBackup = new JButton(new AbstractAction("Uncheck backup") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    uncheckBackup();
//                    cancelButtonAction(e);
                } catch (IOException ex) {
                    Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });
        buttonPanel.addButton(uncheckBackup);

        jbRun = new JButton(new AbstractAction("Start!") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    executeCommand(e);
                } catch (IOException ex) {
                    logger.error("", ex);
                } catch (InterruptedException ex) {
                    logger.error("", ex);
                }
            }

        });
        buttonPanel.addButton(jbRun);
        jbRun.setEnabled(settingsPanel.canRun());

        getRootPane().setDefaultButton(jbRun);

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
        return buttonPanel;
    }

    private void prepareDS() {
        settingsPanel.saveConfig();
        ce.setDs(settingsPanel.getDs());

    }

    private void loginProfiles() {
        settingsPanel.editLoginProfiles();

    }

    private void executeCommand(ActionEvent e) throws IOException, InterruptedException {
        prepareDS();
        ce.executeCmd(this);
    }

    private void showRecent() throws IOException, InterruptedException {
        prepareDS();
        ce.showRecent();
    }

    private void uncheckBackup() throws IOException, InterruptedException {
        prepareDS();
//        Pair<ArrayList<String>, ArrayList<String>> tst = new Pair<>(new ArrayList(), null);
//        ArrayList<String> key = tst.getKey();
//        key.add("esv1_sip_agent_1_b,PRIMARY");
//        key.add("esv1_sip_agent_1_p,BACKUP");
//        settingsPanel.setUncheckNonPrimary(tst);
        settingsPanel.setUncheckNonPrimary(ce.uncheckNonPrimary());
    }

    private void pasteFiles() throws IOException, InterruptedException {
        prepareDS();
//        Pair<ArrayList<String>, ArrayList<String>> tst = new Pair<>(new ArrayList(), null);
//        ArrayList<String> key = tst.getKey();
//        key.add("esv1_sip_agent_1_b,PRIMARY");
//        key.add("esv1_sip_agent_1_p,BACKUP");
//        settingsPanel.setUncheckNonPrimary(tst);
        ce.pasteFiles(this, settingsPanel.getDs());
    }

    private void closeForm() {
        settingsPanel.saveConfig();
        setVisible(false);
        dispose();
        System.exit(0);
    }

    private void logWindowToggled(ActionEvent e) {
        lw.doShow(((JToggleButton) e.getSource()).isSelected());
    }

    void doShow() {
        pack();
        ScreenInfo.CenterWindow(this);
        setVisible(true);

    }

}
