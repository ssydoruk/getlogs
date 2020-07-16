/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import static com.jidesoft.dialog.StandardDialog.RESULT_CANCELLED;
import static com.myutils.getlogs.GetLogs.logger;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;

/**
 *
 * @author Stepan
 */
public class SettingsDialog extends StandardDialog {

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

    public SettingsDialog(DownloadSettings ds, String guiProfile) {
        super();
        settingsPanel = new SettingsPanel(ds, this);
        ce = new CommandExecutor(this);
        lw = new LogWindow();
        lw.doShow();
        setModal(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                closeDialog(RESULT_CANCELLED);
            }

            @Override
            public void windowClosing(WindowEvent e) {

            }
        });
        setTitle("GetLogs" + "(" + guiProfile + ")");
        this.setVisible(true);
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

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public JComponent createContentPanel() {
        return settingsPanel;
    }

    @Override
    public ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = new ButtonPanel();
        JButton cancelButton = new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsPanel.saveConfig();

                closeDialog(RESULT_CANCELLED);

//                    cancelButtonAction(e);
            }

        });
        buttonPanel.addButton(cancelButton);

        JButton pasteFiles = new JButton(new AbstractAction("Paste files") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    pasteFiles();
//                    cancelButtonAction(e);
                } catch (IOException ex) {
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
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

        setDefaultCancelAction(cancelButton.getAction());
        setDefaultAction(jbRun.getAction());
        getRootPane().setDefaultButton(jbRun);

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
        return buttonPanel;
    }

    private void prepareDS() {
        settingsPanel.saveConfig();
        ce.setDs(settingsPanel.getDs());

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

    private void closeDialog(int dialogResult) {
        settingsPanel.saveConfig();
        setDialogResult(dialogResult);
        setVisible(false);
        dispose();
        System.exit(0);
    }

}
