/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import static com.myutils.getlogs.GetLogs.logger;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.*;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.layout.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

/**
 *
 * @author stepan_sydoruk
 */
public final class LogWindow extends JDialog {
    
    static void info(String command_executed) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    static void initCustomLogger() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(true);
        final AbstractConfiguration config = (AbstractConfiguration) ctx.getConfiguration();

        // Create and add the appender
        CustomAppender appender = new CustomAppender("Custom", null, PatternLayout.createDefaultLayout());
        appender.start();
        config.addAppender(appender);

        // Create and add the logger
        AppenderRef[] refs = new AppenderRef[]{AppenderRef.createAppenderRef("Custom", null, null)};
        LoggerConfig loggerConfig = LoggerConfig.createLogger("false", Level.INFO, "com.company", "true", refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("com.company", loggerConfig);
        config.getRootLogger().addAppender(appender, null, null);
//        config.addAppender(appender);
//        ctx.getRootLogger().addAppender(appender);
//        ctx.updateLoggers();
        ctx.updateLoggers(config);

        // Run the job
        logger.info("Hello, World!");
        logger.info("This is awesome!");
        logger.info("Hope it works!");
        logger.info("Hope it helps!");

        // Remove the logger and appender
//        config.removeLogger("com.company");
//        config.removeAppender("Custom");
//        ctx.updateLoggers();
    }
    RSyntaxTextArea jt;
    private JCheckBoxMenuItem miContinuesUpdate;
    private JPopupMenu popup;
    private JCheckBoxMenuItem miLineWrap;
    private JMenuItem miClearLog;
    
    public LogWindow(Window parent) {
        super();
        setLayout(new BorderLayout());
        add(createContentPanel());
        setSize(new Dimension(600, 400));
        invalidate();
        setModal(false);
//        setModal(false);
//        setMaximumSize(new Dimension(500, 200));
//        setFocusable(true);

    }
    
    public void doShow(boolean visible) {

//        ScreenInfo.windowOccupyTopThird(this);
//        setAutoRequestFocus(false);
//        setAlwaysOnTop(false);
//        setFocusable(false);
        invalidate();
        setVisible(visible);
    }
    
    public JComponent createContentPanel() {
        jt = new RSyntaxTextArea();
        jt.setEditable(false);
        
        JScrollPane jScrollPane = new JScrollPane(jt);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jt.setCodeFoldingEnabled(false);
        jt.setWrapStyleWord(false);
        jt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        jt.setEditable(false);
        jt.setTabSize(2);

        JPanel listPane = new JPanel(new BorderLayout());
        
        listPane.add(jScrollPane, BorderLayout.CENTER);
        initPopupMenu();
        
        return listPane;
        
    }
    
    void addMsg(String str) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
//                setVisible(false);
                jt.append(str + "\n");
//                jt.setCaretPosition(jt.getText().length() - 1);
                jt.invalidate();

//                jt.update(jt.getGraphics());
//                setVisible(true);
//                toBack();
            }
        });
    }
    
    private void clearMessages(ActionEvent e) {
        jt.setText("");
    }
    
    private void lineWrap(ActionEvent e) {
        jt.setLineWrap(miLineWrap.isSelected());
        System.out.println("armed: "+miLineWrap.isSelected());
        jt.getParent().revalidate();
    }
    
    private void tailMessages(ActionEvent e) {
        ((DefaultCaret) jt.getCaret()).setUpdatePolicy((miContinuesUpdate.isSelected() ? DefaultCaret.ALWAYS_UPDATE : DefaultCaret.NEVER_UPDATE));
    }
    
    private void initPopupMenu() {
        popup = new JPopupMenu();
        
        miClearLog = new JMenuItem(new AbstractAction("Clear") {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearMessages(e);
            }
            
        });
        popup.add(miClearLog);
        
        miLineWrap = new JCheckBoxMenuItem(new AbstractAction("Line wrap") {
            @Override
            public void actionPerformed(ActionEvent e) {
                lineWrap(e);
            }
            
        });
        popup.add(miLineWrap);
        
        miContinuesUpdate = new JCheckBoxMenuItem(new AbstractAction("Tail messages") {
            @Override
            public void actionPerformed(ActionEvent e) {
                tailMessages(e);
            }
            
        });
        popup.add(miContinuesUpdate);

        //Add listener to components that can bring up popup menus.
        MouseListener popupListener = new PopupListener();
        jt.addMouseListener(popupListener);
    }
    
    class PopupListener extends MouseAdapter {
        
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(),
                        e.getX(), e.getY());
            }
        }
    }
    
    public static class CustomAppender extends AbstractAppender {
        
        final private List<String> list = new ArrayList<>();
        
        public CustomAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
            super(name, filter, layout);
        }
        
        public CustomAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
            super(name, filter, layout, ignoreExceptions);
        }
        
        @Override
        public void append(LogEvent event) {
            byte[] data = getLayout().toByteArray(event);
            System.out.println("-- adding " + new String(data).trim());
            list.add(new String(data).trim()); // optional trim
        }
        
        @Override
        public void stop() {
            // Write to the database
            System.out.println(list);
        }
        
    }
    
}
