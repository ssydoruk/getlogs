/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.io.IoBuilder;

/**
 *
 * @author stepan_sydoruk
 */
public class LogWindow extends StandardDialog {

    static void info(String command_executed) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public LogWindow() {
        setModal(false);
        setMinimumSize(new Dimension(300, 200));
        setMaximumSize(new Dimension(500, 200));
        setFocusable(true);

    }

    public void doShow() {
        pack();
        setVisible(true);
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    JTextArea jt;

    @Override
    public JComponent createContentPanel() {
        jt = new JTextArea();

        JScrollPane jScrollPane = new JScrollPane(jt);

        JPanel listPane = new JPanel(new BorderLayout());

        listPane.add(new JPanel(new BorderLayout()).add(jScrollPane));
        jScrollPane.setMinimumSize(new Dimension(300, 200));
        jScrollPane.setMaximumSize(new Dimension(500, 200));
        return listPane;

    }

    @Override
    public ButtonPanel createButtonPanel() {
        return null;
    }

    void addMsg(String str) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jt.append(str + "\n");
                pack();
            }
        });
    }

}
