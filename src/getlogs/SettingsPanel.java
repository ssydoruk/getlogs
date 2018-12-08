/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.TDateRange;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import static com.jidesoft.dialog.StandardDialog.RESULT_CANCELLED;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.SearchableUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Stepan
 */
public class SettingsPanel extends javax.swing.JPanel {

    private final CheckBoxList clbProfile;
    private final CheckBoxList clbApps;
    private final DefaultListModel<Object> lmProfile;
    private final DefaultListModel<Object> lmApps;
    private TDateRange dtRange;
    private DownloadSettings ds;
    private InfoPanel p = null;

    public DownloadSettings getDs() {
        return ds;
    }

    /**
     * Creates new form SettingsPanel
     */
    public SettingsPanel() {
        initComponents();
        dtRange = new TDateRange(false);
        jpRange.add(dtRange);
//        dtTo = new TDateRange();
//        jpTo.add(dtTo);

        lmProfile = new DefaultListModel<Object>();
        clbProfile = new CheckBoxList(lmProfile);
        jpProfile.add(new JScrollPane(clbProfile));
        clbProfile.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        lmApps = new DefaultListModel<Object>();
        clbApps = new CheckBoxList(lmApps);
        jpApps.add(new JScrollPane(clbApps));

        SearchableUtils.installSearchable(clbProfile);

        clbProfile.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                if (!evt.getValueIsAdjusting()) {
//                    System.out.println("profile Item check state changed " + evt);
                    CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
                }
            }
        });
        clbProfile.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                clbProfileSelectionChanged(evt);
            }

        });

        clbApps.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                clbAppsSelectionChanged(evt);
            }

        });

        clbApps.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                clbAppsCheckedChanged(evt);
            }

        });

    }

    /**
     * called when new profile selected
     *
     * @param evt
     */
    private void clbProfileSelectionChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting()) {
//            System.out.println("profile List item changed - " + evt);
            ListSelectionModel lsm = (ListSelectionModel) evt.getSource();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    clbApps.setValueIsAdjusting(true);
                    int minIndex = lsm.getMinSelectionIndex();
                    int maxIndex = lsm.getMaxSelectionIndex();
                    boolean singleSelection = (minIndex == maxIndex && minIndex >= 0);
                    if (singleSelection) { //one item selected
                        profileChanged((DownloadSettings.AppProfile) lmProfile.get(minIndex));
                    } else {
                        clbApps.setValueIsAdjusting(true);
                        DefaultListModel lm = (DefaultListModel) clbApps.getModel();
                        lm.removeAllElements();
                        clbApps.setValueIsAdjusting(false);
                    }
                    profileSelected(singleSelection);
                    clbApps.setValueIsAdjusting(false);

                }

            });

        }
    }

    private void profileChanged(DownloadSettings.AppProfile pr) {

        clbApps.setValueIsAdjusting(true);
        CheckBoxListSelectionModel clbAppSelectionModel = clbApps.getCheckBoxListSelectionModel();
//                        clbAppSelectionModel.addSelectionInterval(maxIndex, maxIndex);
        clbAppSelectionModel.clearSelection();
        lmApps.clear();
        for (DownloadSettings.App app : pr.getApps()) {
            lmApps.addElement(app);
        }
        for (int i = 0; i < lmApps.getSize(); i++) {
            if (((DownloadSettings.App) lmApps.getElementAt(i)).isChecked()) {
                clbAppSelectionModel.addSelectionInterval(i, i);
            }
        }
//                        clbApps.setCheckBoxListSelectionModel(clbAppSelectionModel);
//                                clbApps.selectAll();
    }

    /**
     * Called when new app selected
     *
     * @param evt
     */
    private void clbAppsSelectionChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting()) {
//                    System.out.println("app List item changed - " + evt);
            ListSelectionModel lsm = (ListSelectionModel) evt.getSource();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
//                    inquirer.logger.info("changing item 1");
                    int minIndex = lsm.getMinSelectionIndex();
                    int maxIndex = lsm.getMaxSelectionIndex();
                    boolean singleSelection = (minIndex == maxIndex && minIndex >= 0);
                    if (singleSelection) {
                        DownloadSettings.App ap = (DownloadSettings.App) lmApps.get(minIndex);
                        setGenesysSelected(ap);
                    }
                    appSelected(singleSelection);
                }

            });

        }
    }

    private void setGenesysSelected(DownloadSettings.App ap) {
        if (ap.getSettings().isIsGenesys()) {
            rbGenesysLogs.setSelected(true);
        } else {
            rbCloudLogs.setSelected(true);
        }
    }

    private void clbAppsCheckedChanged(ListSelectionEvent evt) {
//                    System.out.println("app Item check state changed1 " + evt);
        if (!evt.getValueIsAdjusting()) {
//            System.out.println("app Item check state changed " + evt);
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();

            int maxSelectionIndex = lsm.getMaxSelectionIndex();
            int minSelectionIndex = lsm.getMinSelectionIndex();
            if (maxSelectionIndex == minSelectionIndex && minSelectionIndex >= 0) {
                ((DownloadSettings.App) lmApps.getElementAt(minSelectionIndex))
                        .setChecked(lsm.isSelectedIndex(minSelectionIndex));
            }

//            System.out.println(minSelectionIndex + "-+-" + minSelectionIndex + "=" + clbApps.getCheckBoxListSelectedIndex()
//                    + "@" + evt.getFirstIndex() + "#" + evt.getLastIndex());
        }
    }

    private void appSelected(boolean singleSelection) {
        jbAppDelete.setEnabled(singleSelection);
        jpAppProperties.setEnabled(singleSelection);
        rbCloudLogs.setEnabled(singleSelection);
        rbGenesysLogs.setEnabled(singleSelection);
    }

    private void profileSelected(boolean singleSelection) {
        jpAppsBase.setEnabled(singleSelection);
        jpAppProperties.setEnabled(singleSelection);
        jbProfileDelete.setEnabled(singleSelection);
        jbProfileRename.setEnabled(singleSelection);
        jbProfileSaveAs.setEnabled(singleSelection);
        jbAppAdd.setEnabled(singleSelection);

        int[] selectedIndices = clbApps.getSelectedIndices();
        appSelected(selectedIndices != null && selectedIndices.length == 1);
    }

    private String getProfileName(String winTitle, String initial) {
        while (true) {
            String showInputDialog = JOptionPane.showInputDialog(this, winTitle, initial);
            if (showInputDialog != null && !showInputDialog.isEmpty()) {
                if (ds.profileExists(showInputDialog)) {
                    JOptionPane.showMessageDialog(this, "Profile named [" + showInputDialog
                            + "] already exists", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    return (showInputDialog);
                }
            } else {
                break;
            }
        }
        return null;
    }

    SettingsPanel(DownloadSettings ds) {
        this();
        this.ds = ds;
        loadProfiles();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgFileNaming = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jpProfile = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jbProfileAdd = new javax.swing.JButton();
        jbProfileDelete = new javax.swing.JButton();
        jbProfileRename = new javax.swing.JButton();
        jbProfileSaveAs = new javax.swing.JButton();
        jpAppsBase = new javax.swing.JPanel();
        jpApps = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jbAppAdd = new javax.swing.JButton();
        jbAppDelete = new javax.swing.JButton();
        jpAppProperties = new javax.swing.JPanel();
        rbGenesysLogs = new javax.swing.JRadioButton();
        rbCloudLogs = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        cbLfmtLog = new javax.swing.JCheckBox();
        cbProdLog = new javax.swing.JCheckBox();
        jPanel9 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jtfOutputDir = new javax.swing.JTextField();
        jbSelectDirectory = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        ftHours = new javax.swing.JFormattedTextField();
        lHours = new javax.swing.JLabel();
        cbTimeProfile = new javax.swing.JComboBox<>();
        jPanel12 = new javax.swing.JPanel();
        jpRange = new javax.swing.JPanel();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Application profiles"));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Profile"));
        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.PAGE_AXIS));

        jpProfile.setLayout(new java.awt.BorderLayout());
        jPanel4.add(jpProfile);

        jPanel8.setLayout(new java.awt.GridLayout(1, 0));

        jbProfileAdd.setText("Add");
        jbProfileAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbProfileAddActionPerformed(evt);
            }
        });
        jPanel8.add(jbProfileAdd);

        jbProfileDelete.setText("Delete");
        jbProfileDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbProfileDeleteActionPerformed(evt);
            }
        });
        jPanel8.add(jbProfileDelete);

        jbProfileRename.setText("Rename");
        jbProfileRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbProfileRenameActionPerformed(evt);
            }
        });
        jPanel8.add(jbProfileRename);

        jbProfileSaveAs.setText("SaveAs");
        jbProfileSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbProfileSaveAsActionPerformed(evt);
            }
        });
        jPanel8.add(jbProfileSaveAs);

        jPanel4.add(jPanel8);

        jPanel1.add(jPanel4);

        jpAppsBase.setBorder(javax.swing.BorderFactory.createTitledBorder("Apps"));
        jpAppsBase.setLayout(new javax.swing.BoxLayout(jpAppsBase, javax.swing.BoxLayout.PAGE_AXIS));

        jpApps.setLayout(new java.awt.BorderLayout());
        jpAppsBase.add(jpApps);

        jPanel10.setLayout(new java.awt.GridLayout(1, 0));

        jbAppAdd.setText("Add");
        jbAppAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbAppAddActionPerformed(evt);
            }
        });
        jPanel10.add(jbAppAdd);

        jbAppDelete.setText("Delete");
        jbAppDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbAppDeleteActionPerformed(evt);
            }
        });
        jPanel10.add(jbAppDelete);

        jpAppsBase.add(jPanel10);

        jPanel1.add(jpAppsBase);

        jpAppProperties.setBorder(javax.swing.BorderFactory.createTitledBorder("app properties"));

        bgFileNaming.add(rbGenesysLogs);
        rbGenesysLogs.setText("Genesys file naming");
        rbGenesysLogs.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                rbGenesysLogsItemStateChanged(evt);
            }
        });
        rbGenesysLogs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbGenesysLogsStateChanged(evt);
            }
        });
        rbGenesysLogs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbGenesysLogsActionPerformed(evt);
            }
        });

        bgFileNaming.add(rbCloudLogs);
        rbCloudLogs.setText("Cloud file name");

        javax.swing.GroupLayout jpAppPropertiesLayout = new javax.swing.GroupLayout(jpAppProperties);
        jpAppProperties.setLayout(jpAppPropertiesLayout);
        jpAppPropertiesLayout.setHorizontalGroup(
            jpAppPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpAppPropertiesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpAppPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbGenesysLogs)
                    .addComponent(rbCloudLogs))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jpAppPropertiesLayout.setVerticalGroup(
            jpAppPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpAppPropertiesLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(rbGenesysLogs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbCloudLogs)
                .addContainerGap(76, Short.MAX_VALUE))
        );

        jPanel1.add(jpAppProperties);

        add(jPanel1);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.PAGE_AXIS));

        cbLfmtLog.setText("Logs from LFMT");
        cbLfmtLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbLfmtLogActionPerformed(evt);
            }
        });
        jPanel7.add(cbLfmtLog);

        cbProdLog.setText("logs from prod");
        jPanel7.add(cbProdLog);

        jPanel2.add(jPanel7);

        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.LINE_AXIS));

        jLabel2.setText("Output directory");
        jPanel9.add(jLabel2);
        jPanel9.add(jtfOutputDir);

        jbSelectDirectory.setText("...");
        jbSelectDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSelectDirectoryActionPerformed(evt);
            }
        });
        jPanel9.add(jbSelectDirectory);

        jPanel2.add(jPanel9);

        add(jPanel2);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Time select"));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        ftHours.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0"))));

        lHours.setText("hours");

        cbTimeProfile.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbTimeProfileItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cbTimeProfile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ftHours, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lHours)
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbTimeProfile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ftHours, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lHours))
                .addContainerGap())
        );

        jPanel3.add(jPanel11);

        jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12, javax.swing.BoxLayout.PAGE_AXIS));

        jpRange.setLayout(new java.awt.BorderLayout());
        jPanel12.add(jpRange);

        jPanel3.add(jPanel12);

        add(jPanel3);
    }// </editor-fold>//GEN-END:initComponents

    private void jbProfileAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileAddActionPerformed
        String name = getProfileName("Enter new profile name", null);

        if (name != null) {
            addProfile(name, null);
        }
    }//GEN-LAST:event_jbProfileAddActionPerformed

    private void jbSelectDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSelectDirectoryActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//In response to a button click:
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            jtfOutputDir.setText(fc.getSelectedFile().getAbsolutePath());
        }

    }//GEN-LAST:event_jbSelectDirectoryActionPerformed

    private void jbProfileDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileDeleteActionPerformed
        DownloadSettings.AppProfile appPr = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
        if (appPr != null) {
            if (JOptionPane.showConfirmDialog(this, "Do you really want to delete profile ["
                    + appPr.getName() + "]", "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                lmProfile.removeElement(appPr);
                ds.removeProfile(appPr);
            }
        }
    }//GEN-LAST:event_jbProfileDeleteActionPerformed

    private void jbProfileRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileRenameActionPerformed
        DownloadSettings.AppProfile appPr = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
        if (appPr != null) {
            String name = getProfileName("Edit the profile name", appPr.getName());
            if (name != null) {
                appPr.setName(name);
                clbProfile.updateUI();
            }
        }
    }//GEN-LAST:event_jbProfileRenameActionPerformed

    private void jbProfileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileSaveAsActionPerformed
        DownloadSettings.AppProfile appPr = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
        if (appPr != null) {
            String name = getProfileName("Enter new profile name", appPr.getName());
            if (name != null) {
                addProfile(name, appPr);
            }
        }
    }//GEN-LAST:event_jbProfileSaveAsActionPerformed

    private void rbGenesysLogsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_rbGenesysLogsItemStateChanged
        DownloadSettings.App app = (DownloadSettings.App) clbApps.getSelectedValue();
        if (app != null) {
            app.getSettings().setIsGenesys(evt.getStateChange() == ItemEvent.SELECTED);
        }
    }//GEN-LAST:event_rbGenesysLogsItemStateChanged

    private void cbLfmtLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbLfmtLogActionPerformed

    }//GEN-LAST:event_cbLfmtLogActionPerformed

    private void rbGenesysLogsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbGenesysLogsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbGenesysLogsActionPerformed

    private void rbGenesysLogsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbGenesysLogsStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_rbGenesysLogsStateChanged

    private void cbTimeProfileItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbTimeProfileItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            timeProfileChanged((TimeProfile) evt.getItem());
            // do something with object
        }
    }//GEN-LAST:event_cbTimeProfileItemStateChanged

    private void jbAppAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAppAddActionPerformed

        if (p == null) {
            tab = getJTablePopup();
            p = new InfoPanel((Window) this.getRootPane().getParent(), "aa", tab);
        }
        DefaultTableModel infoTableModel = new DefaultTableModel();
        infoTableModel.addColumn("Applications");
        ArrayList<String> apps = new ArrayList<>();
        ListModel mod = clbApps.getModel();
        HashSet<String> apps1 = new HashSet<>(mod.getSize());
        for (int i = 0; i < mod.getSize(); i++) {
            apps1.add(mod.getElementAt(i).toString());
        }
        for (String string : GetLogs.getHosts().keySet()) {
            if (!apps1.contains(string)) {
                apps.add(string);
            }
        }
        Collections.sort(apps);
        for (String app : apps) {
            infoTableModel.addRow(new Object[]{app});
        }
        tab.setModel(infoTableModel);

        p.doShow();

        if (p.getCloseCause() == JOptionPane.OK_OPTION) {
            int[] selectedRows = tab.getSelectedRows();
//            HashSet<String> selValues = new HashSet<>(selectedRows.length);
            DownloadSettings.AppProfile profile = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
            for (int selectedRow : selectedRows) {
                DownloadSettings.App addApp = profile.addApp((String) infoTableModel.getValueAt(selectedRow, 0));
//                lmApps.addElement(addApp);
//                selValues.add((String) infoTableModel.getValueAt(selectedRow, 0));
            }
            profileChanged(profile);
//            profileSelected(true);
        }

    }//GEN-LAST:event_jbAppAddActionPerformed

    private void jbAppDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAppDeleteActionPerformed
        DownloadSettings.AppProfile appPr = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
        DownloadSettings.App app = (DownloadSettings.App) clbApps.getSelectedValue();
        if (appPr != null && app != null) {
            if (JOptionPane.showConfirmDialog(this, "Do you really want to delete "
                    + "app [" + app.getName() + "]"
                    + " from profile ["
                    + appPr.getName() + "]", "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                appPr.removeApp(app);
                lmApps.removeElement(app);
            }
        }
    }//GEN-LAST:event_jbAppDeleteActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgFileNaming;
    private javax.swing.JCheckBox cbLfmtLog;
    private javax.swing.JCheckBox cbProdLog;
    private javax.swing.JComboBox<String> cbTimeProfile;
    private javax.swing.JFormattedTextField ftHours;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton jbAppAdd;
    private javax.swing.JButton jbAppDelete;
    private javax.swing.JButton jbProfileAdd;
    private javax.swing.JButton jbProfileDelete;
    private javax.swing.JButton jbProfileRename;
    private javax.swing.JButton jbProfileSaveAs;
    private javax.swing.JButton jbSelectDirectory;
    private javax.swing.JPanel jpAppProperties;
    private javax.swing.JPanel jpApps;
    private javax.swing.JPanel jpAppsBase;
    private javax.swing.JPanel jpProfile;
    private javax.swing.JPanel jpRange;
    private javax.swing.JTextField jtfOutputDir;
    private javax.swing.JLabel lHours;
    private javax.swing.JRadioButton rbCloudLogs;
    private javax.swing.JRadioButton rbGenesysLogs;
    // End of variables declaration//GEN-END:variables

    private void loadProfiles() {
        lmProfile.clear();
        for (DownloadSettings.AppProfile appProfile : ds.getAppProfiles()) {
            lmProfile.addElement(appProfile);
        }
        int[] selectedIndices = clbProfile.getSelectedIndices();
        profileSelected(selectedIndices != null && selectedIndices.length == 1);
        cbLfmtLog.setSelected(ds.isLfmt());
        cbProdLog.setSelected(ds.isProd());
        jtfOutputDir.setText(ds.getOutputDir());
        DefaultComboBoxModel mod = new DefaultComboBoxModel(new TimeProfile[]{
            TimeProfile.VALUE, TimeProfile.FROM, TimeProfile.FROM_TO});
        cbTimeProfile.setModel(mod);
        cbTimeProfile.setSelectedItem(ds.getTimeProfile());
        ftHours.setText(ds.getHours());
        dtRange.setTimeRange(ds.getTimeRange());
        timeProfileChanged((TimeProfile) cbTimeProfile.getSelectedItem());
    }

    private void addProfile(String showInputDialog, DownloadSettings.AppProfile appPr) {
        DownloadSettings.AppProfile addProfile = ds.addProfile(showInputDialog, appPr); //To change body of generated methods, choose Tools | Templates.
        lmProfile.addElement(addProfile);
    }

    public void saveConfig() {
        ds.setOutputDir(jtfOutputDir.getText());
        ds.setLfmt(cbLfmtLog.isSelected());
        ds.setProd(cbProdLog.isSelected());
        ds.setTimeProfile((TimeProfile) cbTimeProfile.getSelectedItem());
        ds.setHours(ftHours.getText());
        ds.setTimeRange(dtRange.getTimeRangeAlways());
        Gson gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setDateFormat(DateFormat.LONG)
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setPrettyPrinting()
                .setVersion(1.0)
                .create();

        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(ds.getSettingsFile()));
            gson.toJson(ds, writer);

            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void timeProfileChanged(TimeProfile timeProfile) {
        ftHours.setEnabled(timeProfile == TimeProfile.VALUE);
        lHours.setEnabled(timeProfile == TimeProfile.VALUE);
        dtRange.enableFrom(timeProfile == TimeProfile.FROM || timeProfile == TimeProfile.FROM_TO);
        dtRange.enableTo(timeProfile == TimeProfile.FROM_TO);
    }

    public enum TimeProfile {

        VALUE("Last..."),
        FROM("From"),
        FROM_TO("Range");

        private final String name;

        private TimeProfile(String s) {
            name = s;
        }

//    public boolean equals(DialogItem item) {
//        
//    }
        public boolean equalsName(String otherName) {
            return (otherName == null) ? false : name.toLowerCase().equals(otherName.toLowerCase());
        }

        public String toString() {
            return this.name;
        }

    }

    class InfoPanel extends StandardDialog {

        private int closeCause = JOptionPane.CANCEL_OPTION;
        private JTable tab;

        public int getCloseCause() {
            return closeCause;
        }

        public void setCloseCause(int closeCause) {
            this.closeCause = closeCause;
        }

        private void selectionChanged(JButton bt, JTable tab) {
            int rowsSelected = tab.getSelectedRows().length;
            bt.setEnabled(rowsSelected > 0);
            bt.setText((rowsSelected == 0)
                    ? "Empty selection"
                    : "Add " + rowsSelected + " apps");
        }

        InfoPanel(Window parent, String title, JTable tab) {
            super(parent, title);
            this.tab = tab;

        }

        public void doShow() {
            setModal(true);

            pack();
            if (LogManager.getLogger().isDebugEnabled()) {
                LogManager.getLogger().debug("Show info PanelDialog; title=" + getTitle() + "; tab cols:" + tab.getColumnCount() + " rows: " + tab.getRowCount());
                StringBuilder s = new StringBuilder(512);
                for (int i = 0; i < tab.getRowCount(); i++) {
                    s.setLength(0);
                    for (int j = 0; j < tab.getColumnCount(); j++) {
                        s.append("[" + tab.getValueAt(i, j) + "],");
                    }
                    LogManager.getLogger().debug(s);
                }

            }

//            ScreenInfo.CenterWindow(this);
            this.setLocationRelativeTo(getParent());
            setVisible(
                    true);
        }

        @Override
        public JComponent createBannerPanel() {
            return null;
        }

        @Override
        public JComponent createContentPanel() {
//                        JPanel panel = new JPanel(new BorderLayout(10, 10));
//            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

//            panel.add(mainPanel, BorderLayout.CENTER);
//            return panel;
            JScrollPane jScrollPane = new JScrollPane(tab);
            tab.getTableHeader().setVisible(true);

            JPanel listPane = new JPanel(new BorderLayout(10, 10));

            listPane.add(new JPanel(new BorderLayout()).add(jScrollPane));
            return listPane;
        }

        JButton jbFilter;

        @Override
        public ButtonPanel createButtonPanel() {
            ButtonPanel buttonPanel = new ButtonPanel();
            JButton cancelButton = new JButton();
            buttonPanel.addButton(cancelButton);

            cancelButton.setAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    setDialogResult(RESULT_CANCELLED);
                    setCloseCause(JOptionPane.CANCEL_OPTION);
                    setVisible(false);
                    dispose();
                }
            });
            cancelButton.setText("Close");

            jbFilter = new JButton("Use as filter");
            buttonPanel.addButton(jbFilter);
            tab.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    selectionChanged(jbFilter, tab);
                }
            });
            selectionChanged(jbFilter, tab);

//            listPane.add(jbFilter);
            jbFilter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tab.editingCanceled(null);
                    setCloseCause(JOptionPane.OK_OPTION);
                    dispose();
                }
            });

            String act = "ApplyFilter";

            tab.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), act);
            tab.getActionMap().put(act, jbFilter.getAction());

            setDefaultCancelAction(cancelButton.getAction());
            setDefaultAction(jbFilter.getAction());
            getRootPane().setDefaultButton(jbFilter);

            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
            return buttonPanel;
        }

        private void doShow(String theTitle) {
            this.setTitle(theTitle);
            doShow();
        }
    }

    private JTablePopup uniquePopup;

    private JTablePopup getJTablePopup() {
        if (uniquePopup == null) {

            uniquePopup = new JTablePopup() {
                @Override
                void theMousePressed(MouseEvent e) {

                }

                @Override
                void callingPopup() {

                }
            };
            uniquePopup.getTableHeader().setVisible(false);
            JPopupMenu popupMenu1 = uniquePopup.getPopupMenu();

            String act = "Search (Ctrl-F)";
            uniquePopup.getInputMap().put(KeyStroke.getKeyStroke('F', java.awt.event.InputEvent.CTRL_DOWN_MASK), act);
            uniquePopup.getActionMap().put(act, new FindKeys(uniquePopup));

            act = "SearchForward (F3)";
            uniquePopup.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), act);
            uniquePopup.getActionMap().put(act, new FindForward(uniquePopup));

            act = "SearchBack (Shift-F3)";
            uniquePopup.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, java.awt.event.InputEvent.SHIFT_DOWN_MASK), act);
            uniquePopup.getActionMap().put(act, new FindBack(uniquePopup));

            popupMenu1.add(new Find());
            popupMenu1.add(new FindNext());
            popupMenu1.add(new FindPrevius());
            popupMenu1.addSeparator();
            popupMenu1.add(new FindAndSelect());
            popupMenu1.add(new ReverseSelection());
            popupMenu1.addSeparator();

        }
        return uniquePopup;
    }

    protected class FindBack extends AbstractAction {

        private final JTablePopup tab;

        public FindBack(JTablePopup aThis) {
            tab = aThis;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tab.showFindDialog();
        }
    }

    protected class FindForward extends AbstractAction {

        private final JTablePopup tab;

        public FindForward(JTablePopup aThis) {
            tab = aThis;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tab.showFindDialog();

        }
    }

    class FindKeys extends AbstractAction {

        private final JTablePopup tab;

        public FindKeys(JTablePopup tab) {
            super();
            this.tab = tab;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tab.showFindDialog();
        }

    }

    class Find extends AbstractAction {

        public Find() {
            super("Find (Ctrl-F)");
            putValue(SHORT_DESCRIPTION, "Search in the table (Ctrl-F)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).showFindDialog();

        }

        private Frame getTheParent(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            return (Frame) ((JTable) popup.getInvoker()).getRootPane().getParent();
        }

    }

    class FindAndSelect extends AbstractAction {

        public FindAndSelect() {
            super("Find and select");
            putValue(SHORT_DESCRIPTION, "Find and select");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).findAndSelect();

        }

        private Frame getTheParent(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            return (Frame) ((JTable) popup.getInvoker()).getRootPane().getParent();
        }

    }

    class ReverseSelection extends AbstractAction {

        public ReverseSelection() {
            super("Reverse selection");
            putValue(SHORT_DESCRIPTION, "Find and select");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).reverseSelection();

        }

        private Frame getTheParent(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            return (Frame) ((JTable) popup.getInvoker()).getRootPane().getParent();
        }

    }

    class FindNext extends AbstractAction {

        public FindNext() {
            super("Find next (F3)");
            putValue(SHORT_DESCRIPTION, "Find next (F3)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    class FindPrevius extends AbstractAction {

        public FindPrevius() {
            super("Find previous (Shift-F3)");
            putValue(SHORT_DESCRIPTION, "Find previous (Shift-F3)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    JTablePopup tab;
}
