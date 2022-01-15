/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.Pair;
import Utils.ScreenInfo;
import Utils.UnixProcess.ExtProcess;
import com.jidesoft.dialog.ButtonPanel;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.myutils.getlogs.GetLogs.logger;

/**
 * @author Stepan
 */
public final class SettingsForm extends JFrame {

    static final FixedDateFormat fixedDateFormat = FixedDateFormat.create(FixedDateFormat.FixedFormat.ABSOLUTE_PERIOD);
    private static LogWindow lw;
    private final SettingsPanel settingsPanel;
    private final CommandExecutor ce;
    private JButton jbRun;
    private JToggleButton showLog;

    public SettingsForm(DownloadSettings ds, String guiProfile) {

        super();

        initLogging();
        logger.fatal("testing logger");
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

    static void info(String str) {
        logger.info(str);
    }

    static void error(String str) {
        logger.error(str);
    }

    private void initLogging() {
        /*
         * from https://logging.apache.org/log4j/2.x/manual/customconfig.html#AddingToCurrent
         * */
        final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        Appender appender = new LogWindowAppender(CommandExecutor.class.getName(), null,
                this);
        appender.start();
        LoggerConfig rootLogger = config.getRootLogger();

        rootLogger.addAppender(appender, org.apache.logging.log4j.Level.INFO, null);
        ctx.updateLoggers(config);
        logger.fatal("fatal error");
        org.apache.logging.log4j.Logger logger1 = org.apache.logging.log4j.LogManager.getLogger(GetLogs.class);
        logger1.info("hello");
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
        ExtProcess.ExecutionResult executionResult = ce.uncheckNonPrimary();
        if (executionResult != null && executionResult.hashCode() == 0)
            settingsPanel.setUncheckNonPrimary(new Pair<>(executionResult.getStdOut(), executionResult.getStdErr()));
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

    public void postLogEvent(LogEvent event) {
        if (lw != null) {
            StringBuilder msg = new StringBuilder().append(
                    fixedDateFormat.formatInstant(event.getInstant())).append(" ");
            if (event.getLevel() == org.apache.logging.log4j.Level.ERROR)
                msg.append("!ERROR! ");
            else if (event.getLevel() == org.apache.logging.log4j.Level.WARN)
                msg.append("!WARN! ");
            else if (event.getLevel() == org.apache.logging.log4j.Level.FATAL)
                msg.append("!FATAL! ");
            msg.append(event.getMessage().getFormattedMessage());
            lw.addMsg(msg.toString());
        }
    }
}
