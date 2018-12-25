/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.ScreenInfo;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import static com.jidesoft.dialog.StandardDialog.RESULT_CANCELLED;
import getlogs.DownloadSettings.App;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.HashSet;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Stepan
 */
public class SettingsDialog extends StandardDialog {

    private final SettingsPanel settingsPanel;

    public SettingsDialog(DownloadSettings ds) {
        super();
        settingsPanel = new SettingsPanel(ds);
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
            public void actionPerformed(ActionEvent e) {
                closeDialog(RESULT_CANCELLED);

//                    cancelButtonAction(e);
            }

        });
        buttonPanel.addButton(cancelButton);

        JButton jbRun = new JButton(new AbstractAction("Start!") {
            public void actionPerformed(ActionEvent e) {
                closeDialog(RESULT_AFFIRMED);
            }
        });
        buttonPanel.addButton(jbRun);

        setDefaultCancelAction(cancelButton.getAction());
        setDefaultAction(jbRun.getAction());
        getRootPane().setDefaultButton(jbRun);

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
        return buttonPanel;
    }

    private void closeDialog(int dialogResult) {
        settingsPanel.saveConfig();
        setDialogResult(dialogResult);
        setVisible(false);
        dispose();
    }

    public static void main(String args[]) {

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                File f = new File("aa.txt");
                DownloadSettings ds = null;
                if (f.exists()) {
//                Gson gson = new Gson();

                    Gson gson = new GsonBuilder()
                            .enableComplexMapKeySerialization()
                            .serializeNulls()
                            .setDateFormat(DateFormat.LONG)
                            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                            .setPrettyPrinting()
                            .setVersion(1.0)
                            .create();

                    try {
                        InputStreamReader reader = new InputStreamReader(new FileInputStream(f));
                        ds = gson.fromJson(reader, DownloadSettings.class);
                        reader.close();
                    } catch (JsonSyntaxException | JsonIOException | IOException ex) {
                                LogManager.getLogger().log(org.apache.logging.log4j.Level.FATAL,  ex);
                    }
                } else {
                    ds = new DownloadSettings();
                    HashSet<DownloadSettings.App> apps = ds.addProfile("SIP").getApps();
                    apps.add(new App("sip1", new DownloadSettings.AppSettings()));
                    apps.add(new App("sip2", new DownloadSettings.AppSettings()));

                    apps = ds.addProfile("Routing").getApps();
                    apps.add(new App("URS1", new DownloadSettings.AppSettings()));
                    apps.add(new App("ORS2", new DownloadSettings.AppSettings()));
                }

                SettingsDialog dlg = new SettingsDialog(ds);
                dlg.pack();
                dlg.invalidate();
                ScreenInfo.CenterWindow(dlg);
                dlg.setVisible(true);
            }
        });
    }

}
