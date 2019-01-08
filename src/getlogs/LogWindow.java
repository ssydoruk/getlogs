/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import java.awt.BorderLayout;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.io.IoBuilder;

/**
 *
 * @author stepan_sydoruk
 */
public class LogWindow extends StandardDialog {
     
    

    public LogWindow() {
        setModal(false);

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
        jt=new JTextArea();
        
                    JScrollPane jScrollPane = new JScrollPane(jt);

            JPanel listPane = new JPanel(new BorderLayout(10, 10));

            listPane.add(new JPanel(new BorderLayout()).add(jScrollPane));
            return listPane;

    }

    @Override
    public ButtonPanel createButtonPanel() {
        return null;
    }
 

}
