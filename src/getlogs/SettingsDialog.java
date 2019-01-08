/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import static com.jidesoft.dialog.StandardDialog.RESULT_CANCELLED;
import java.awt.event.ActionEvent;
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

    private final SettingsPanel settingsPanel;
    private final CommandExecutor ce;
private LogWindow lw;

    public SettingsDialog(DownloadSettings ds) {
        super();
        settingsPanel = new SettingsPanel(ds);
        ce = new CommandExecutor(this);
        lw=new LogWindow();
        lw.doShow();
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
            public void actionPerformed(ActionEvent e) {
                settingsPanel.saveConfig();

                closeDialog(RESULT_CANCELLED);

//                    cancelButtonAction(e);
            }

        });
        buttonPanel.addButton(cancelButton);

        JButton jbRun = new JButton(new AbstractAction("Start!") {
            public void actionPerformed(ActionEvent e) {
                try {
                    executeCommand(e);
                } catch (IOException ex) {
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
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

    private void executeCommand(ActionEvent e) throws IOException, InterruptedException {
        settingsPanel.saveConfig();
        ce.setDs(settingsPanel.getDs());
        ce.executeCmd();
    }

    private void closeDialog(int dialogResult) {
        settingsPanel.saveConfig();
        setDialogResult(dialogResult);
        setVisible(false);
        dispose();
    }

}
