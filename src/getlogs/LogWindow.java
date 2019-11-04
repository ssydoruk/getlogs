/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.ScreenInfo;
import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 *
 * @author stepan_sydoruk
 */
public class LogWindow extends JFrame {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

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

    public LogWindow() {
        super();
        add(createContentPanel());
//        setModal(false);
//        setMaximumSize(new Dimension(500, 200));
//        setFocusable(true);

    }

    public void doShow() {
        pack();
        ScreenInfo.windowOccupyTopThird(this);
//        setAutoRequestFocus(false);
//        setAlwaysOnTop(false);
//        setFocusable(false);
        setVisible(true);
    }

    JTextArea jt;

    public JComponent createContentPanel() {
        jt = new JTextArea();

        JScrollPane jScrollPane = new JScrollPane(jt);

        JPanel listPane = new JPanel(new BorderLayout());

        listPane.add(jScrollPane, BorderLayout.CENTER);
//        jScrollPane.setMinimumSize(new Dimension(300, 200));
//        jScrollPane.setMaximumSize(new Dimension(500, 200));
//        jScrollPane.setPreferredSize(new Dimension(500, 200));
        return listPane;

    }

    void addMsg(String str) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
//                setVisible(false);
                jt.append(str + "\n");
                jt.invalidate();
                jt.setCaretPosition(jt.getText().length() - 1);

//                jt.update(jt.getGraphics());
//                setVisible(true);
//                toBack();
            }
        });
    }

    public static class CustomAppender extends AbstractAppender {

        private List<String> list = new ArrayList<>();

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
