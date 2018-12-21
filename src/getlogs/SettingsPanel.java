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
import java.awt.event.ItemListener;
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
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
    private InfoPanel lfmtPanes = null;

    public DownloadSettings getDs() {
        return ds;
    }

    /**
     * Creates new form SettingsPanel
     */
    public SettingsPanel() {
        initComponents();
//        dtRange = new TDateRange(false);
//        jpRange.add(dtRange);
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
                clbProfileCheckedChanged(evt);
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

    private void clbProfileCheckedChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting()) {
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    int maxSelectionIndex = lsm.getMaxSelectionIndex();
                    int minSelectionIndex = lsm.getMinSelectionIndex();
                    System.out.println("-1-" + evt
                            + " f: " + evt.getFirstIndex()
                            + " l: " + evt.getLastIndex()
                            + " min: " + minSelectionIndex + " max: " + maxSelectionIndex
                    );
                    for (int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++) {
                        ((DownloadSettings.AppProfile) lmProfile.getElementAt(i))
                                .setSelected(lsm.isSelectedIndex(i));

                    }
                }
            });
        }
    }

    /**
     * called when new profile selected
     *
     * @param evt
     */
    private void clbProfileSelectionChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting()) {
            System.out.println("clbProfileSelectionChanged List item changed - " + evt);
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
        ListSelectionListener[] listSelectionListeners = clbAppSelectionModel.getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            clbAppSelectionModel.removeListSelectionListener(listSelectionListener);
        }

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
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            clbAppSelectionModel.addListSelectionListener(listSelectionListener);
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
        DownloadSettings.LFMTHostInstance lfmtHostInstance = ap.getSettings().getLfmtHostInstance();
        int sel = -1;
        if (lfmtHostInstance != null) {
            DefaultComboBoxModel<DownloadSettings.LFMTHostInstance> mod = (DefaultComboBoxModel<DownloadSettings.LFMTHostInstance>) cbLFMTs.getModel();
            for (int i = 0; i < mod.getSize(); i++) {
                DownloadSettings.LFMTHostInstance inst = mod.getElementAt(i);
                if (inst.getKey() == lfmtHostInstance.getKey()
                        && inst.getValue() == lfmtHostInstance.getValue()) {
                    sel = i;
                    break;
                }
            }
        }
        if (sel >= 0) {
            cbLFMTs.setSelectedIndex(sel);
        } else {
//            cbLFMTs.setSelectedIndex(0);
            ap.getSettings().setLfmtHostInstance((DownloadSettings.LFMTHostInstance) cbLFMTs.getSelectedItem());
        }
    }

    private void clbAppsCheckedChanged(ListSelectionEvent evt) {

        if (!evt.getValueIsAdjusting()) {
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    int maxSelectionIndex = lsm.getMaxSelectionIndex();
                    int minSelectionIndex = lsm.getMinSelectionIndex();
                    System.out.println("-1-" + evt
                            + " f: " + evt.getFirstIndex()
                            + " l: " + evt.getLastIndex()
                            + " min: " + minSelectionIndex + " max: " + maxSelectionIndex
                    );
                    for (int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++) {
                        ((DownloadSettings.App) lmApps.getElementAt(i))
                                .setChecked(lsm.isSelectedIndex(i));

                    }
                }
            });
        }

    }

    private void appSelected(boolean singleSelection) {
        jbAppDelete.setEnabled(singleSelection);
        jpAppProperties.setEnabled(singleSelection);
        rbCloudLogs.setEnabled(singleSelection);
        rbGenesysLogs.setEnabled(singleSelection);
        cbLFMTs.setEnabled(singleSelection);
        btEditLFMTs.setEnabled(singleSelection);
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
        jPanel5 = new javax.swing.JPanel();
        rbGenesysLogs = new javax.swing.JRadioButton();
        rbCloudLogs = new javax.swing.JRadioButton();
        jPanel6 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        cbLFMTs = new javax.swing.JComboBox();
        btEditLFMTs = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        cbLfmtLog = new javax.swing.JCheckBox();
        cbProdLog = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        ftHours = new javax.swing.JFormattedTextField();
        lHours = new javax.swing.JLabel();
        cbTimeProfile = new javax.swing.JComboBox<>();
        jPanel12 = new javax.swing.JPanel();
        jpRange = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        lbDateRegex = new javax.swing.JLabel();
        tfDateRegex = new javax.swing.JTextField();
        jPanel14 = new javax.swing.JPanel();
        lbTimeRegex = new javax.swing.JLabel();
        tfTimeRegex = new javax.swing.JTextField();
        jPanel9 = new javax.swing.JPanel();
        jPanel19 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        lCommand = new javax.swing.JLabel();
        cbCommand = new javax.swing.JComboBox();
        jPanel18 = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        cbUseRSync = new javax.swing.JCheckBox();
        cbListFiles = new javax.swing.JCheckBox();
        jPanel21 = new javax.swing.JPanel();
        lGrepText = new javax.swing.JLabel();
        tfGrepText = new javax.swing.JTextField();
        jPanel17 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jtfOutputDir = new javax.swing.JTextField();
        jbSelectDirectory = new javax.swing.JButton();

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
        jpAppProperties.setLayout(new javax.swing.BoxLayout(jpAppProperties, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

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

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbGenesysLogs)
                    .addComponent(rbCloudLogs))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rbGenesysLogs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbCloudLogs)
                .addContainerGap())
        );

        jpAppProperties.add(jPanel5);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("LFMT"));

        jPanel13.setLayout(new javax.swing.BoxLayout(jPanel13, javax.swing.BoxLayout.LINE_AXIS));

        cbLFMTs.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbLFMTsItemStateChanged(evt);
            }
        });
        jPanel13.add(cbLFMTs);

        btEditLFMTs.setText("...");
        btEditLFMTs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btEditLFMTsActionPerformed(evt);
            }
        });
        jPanel13.add(btEditLFMTs);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addGap(0, 50, Short.MAX_VALUE)
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 83, Short.MAX_VALUE))
        );

        jpAppProperties.add(jPanel6);

        jPanel1.add(jpAppProperties);

        add(jPanel1);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        jPanel7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
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

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Time select"));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        ftHours.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0"))));

        lHours.setText("hours");

        cbTimeProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbTimeProfileActionPerformed(evt);
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
                .addContainerGap(47, Short.MAX_VALUE))
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

        jpRange.setLayout(new javax.swing.BoxLayout(jpRange, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel15.setLayout(new javax.swing.BoxLayout(jPanel15, javax.swing.BoxLayout.LINE_AXIS));

        lbDateRegex.setText("Date regex(digits, [-])");
        jPanel15.add(lbDateRegex);
        jPanel15.add(tfDateRegex);

        jpRange.add(jPanel15);

        jPanel14.setLayout(new javax.swing.BoxLayout(jPanel14, javax.swing.BoxLayout.LINE_AXIS));

        lbTimeRegex.setText("Time regex(digits, [-])");
        jPanel14.add(lbTimeRegex);
        jPanel14.add(tfTimeRegex);

        jpRange.add(jPanel14);

        jPanel12.add(jpRange);

        jPanel3.add(jPanel12);

        jPanel2.add(jPanel3);

        add(jPanel2);

        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel19.setLayout(new javax.swing.BoxLayout(jPanel19, javax.swing.BoxLayout.LINE_AXIS));

        jPanel16.setLayout(new javax.swing.BoxLayout(jPanel16, javax.swing.BoxLayout.LINE_AXIS));

        lCommand.setText("Command");
        jPanel16.add(lCommand);

        cbCommand.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbCommandActionPerformed(evt);
            }
        });
        jPanel16.add(cbCommand);

        jPanel19.add(jPanel16);

        jPanel18.setLayout(new javax.swing.BoxLayout(jPanel18, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel20.setLayout(new java.awt.BorderLayout());

        cbUseRSync.setText("Use RSync");
        jPanel20.add(cbUseRSync, java.awt.BorderLayout.CENTER);

        cbListFiles.setText("list files (directories only if unchecked)");
        jPanel20.add(cbListFiles, java.awt.BorderLayout.PAGE_START);

        jPanel18.add(jPanel20);

        jPanel21.setLayout(new javax.swing.BoxLayout(jPanel21, javax.swing.BoxLayout.LINE_AXIS));

        lGrepText.setText("text to grep");
        jPanel21.add(lGrepText);
        jPanel21.add(tfGrepText);

        jPanel18.add(jPanel21);

        jPanel19.add(jPanel18);

        jPanel9.add(jPanel19);

        jPanel17.setLayout(new javax.swing.BoxLayout(jPanel17, javax.swing.BoxLayout.LINE_AXIS));

        jLabel2.setText("Output directory");
        jPanel17.add(jLabel2);
        jPanel17.add(jtfOutputDir);

        jbSelectDirectory.setText("...");
        jbSelectDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSelectDirectoryActionPerformed(evt);
            }
        });
        jPanel17.add(jbSelectDirectory);

        jPanel9.add(jPanel17);

        add(jPanel9);
    }// </editor-fold>//GEN-END:initComponents

    private void jbProfileAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileAddActionPerformed
        String name = getProfileName("Enter new profile name", null);

        if (name != null) {
            addProfile(name);
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

    private void btEditLFMTsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btEditLFMTsActionPerformed
        if (lfmtPanes == null) {
            tabLFMT = getJTablePopup();
            lfmtPanes = new InfoPanel((Window) this.getRootPane().getParent(), "List of LFMTs", tabLFMT);
        }
        DefaultTableModel infoTableModel = new DefaultTableModel();
        infoTableModel.addColumn("LFMT host");
        infoTableModel.addColumn("LFMT instance");
        for (DownloadSettings.LFMTHostInstance hi : ds.getLfmtHostInstances()) {
            infoTableModel.addRow(new Object[]{hi.getKey(), hi.getValue()});
        }
        tabLFMT.setModel(infoTableModel);
        JButton bt;
        bt = new JButton(new AbstractAction("add") {
            private EditLFMTDialog dlg = null;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (dlg == null) {
                    dlg = new EditLFMTDialog();
                }
                if (dlg.doShow()) {
                    newLFMTInstance(dlg.getLfmt().tbValue.getText(), dlg.getLfmtInstance().tbValue.getText());
                }
            }

            private void newLFMTInstance(String text, String text0) {
                DownloadSettings.LFMTHostInstance lfmt = ds.addLFMTPair(text, text0);
                DefaultTableModel mod = (DefaultTableModel) tabLFMT.getModel();
                mod.addRow(new Object[]{lfmt.getKey(), lfmt.getValue()});
            }
        });
        lfmtPanes.addButton(bt);

        lfmtPanes.doShow();
        updateLFMTs();

    }//GEN-LAST:event_btEditLFMTsActionPerformed

    private void cbLFMTsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbLFMTsItemStateChanged

        if (evt.getStateChange() == ItemEvent.SELECTED) {
            Object item = evt.getItem();
            DownloadSettings.App app = (DownloadSettings.App) clbApps.getSelectedValue();
            if (app != null) {
                app.getSettings().setLfmtHostInstance((DownloadSettings.LFMTHostInstance) item);
            }
        }
    }//GEN-LAST:event_cbLFMTsItemStateChanged

    private void cbCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbCommandActionPerformed
        cbCommandSelectionChanged((GetCommand) ((JComboBox) evt.getSource()).getSelectedItem());
    }//GEN-LAST:event_cbCommandActionPerformed

    private void cbTimeProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbTimeProfileActionPerformed
        timeProfileChanged((TimeProfile) ((JComboBox) evt.getSource()).getSelectedItem());
//            timeProfileChanged((TimeProfile) evt.getSource());
    }//GEN-LAST:event_cbTimeProfileActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgFileNaming;
    private javax.swing.JButton btEditLFMTs;
    private javax.swing.JComboBox cbCommand;
    private javax.swing.JComboBox cbLFMTs;
    private javax.swing.JCheckBox cbLfmtLog;
    private javax.swing.JCheckBox cbListFiles;
    private javax.swing.JCheckBox cbProdLog;
    private javax.swing.JComboBox<String> cbTimeProfile;
    private javax.swing.JCheckBox cbUseRSync;
    private javax.swing.JFormattedTextField ftHours;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
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
    private javax.swing.JLabel lCommand;
    private javax.swing.JLabel lGrepText;
    private javax.swing.JLabel lHours;
    private javax.swing.JLabel lbDateRegex;
    private javax.swing.JLabel lbTimeRegex;
    private javax.swing.JRadioButton rbCloudLogs;
    private javax.swing.JRadioButton rbGenesysLogs;
    private javax.swing.JTextField tfDateRegex;
    private javax.swing.JTextField tfGrepText;
    private javax.swing.JTextField tfTimeRegex;
    // End of variables declaration//GEN-END:variables

    private void loadProfiles() {
        CheckBoxListSelectionModel checkBoxListSelectionModel = clbProfile.getCheckBoxListSelectionModel();
        ListSelectionListener[] listSelectionListeners = checkBoxListSelectionModel.getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            checkBoxListSelectionModel.removeListSelectionListener(listSelectionListener);
        }
        lmProfile.clear();
        for (DownloadSettings.AppProfile appProfile : ds.getAppProfiles()) {
            lmProfile.addElement(appProfile);
            int idx = lmProfile.size() - 1;
            if (appProfile.isSelected()) {
                checkBoxListSelectionModel.addSelectionInterval(idx, idx);
            }
        }
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            checkBoxListSelectionModel.addListSelectionListener(listSelectionListener);
        }
        clbProfile.setCheckBoxListSelectionModel(checkBoxListSelectionModel);

        cbListFiles.setSelected(ds.isListFiles());
        int[] selectedIndices = clbProfile.getSelectedIndices();
        profileSelected(selectedIndices != null && selectedIndices.length == 1);
        cbLfmtLog.setSelected(ds.isLfmt());
        cbProdLog.setSelected(ds.isProd());
        jtfOutputDir.setText(ds.getOutputDir());
        cbUseRSync.setSelected(ds.isUseRSync());

        initCB(cbTimeProfile, ds.getTimeProfile(), new TimeProfile[]{TimeProfile.VALUE, TimeProfile.REGEX}, null);

        ftHours.setText(ds.getHours());
//        dtRange.setTimeRange(ds.getTimeRange());
        timeProfileChanged((TimeProfile) cbTimeProfile.getSelectedItem());
        tfDateRegex.setText(ds.getDateSpec());
        tfTimeRegex.setText(ds.getTimeSpec());
        GetCommand actionCommand = ds.getActionCommand();
        if (actionCommand == null || actionCommand == GetCommand.Unknown) {
            actionCommand = GetCommand.LS;
        }
        initCB(cbCommand, actionCommand, GetCommand.values(), new Object[]{GetCommand.Unknown});
//        cbCommand.addItemListener(aListener);
        tfGrepText.setText(ds.getGrepText());
        updateLFMTs();
    }

    private void addProfile(String showInputDialog) {
        DownloadSettings.AppProfile addProfile = ds.addProfile(showInputDialog); //To change body of generated methods, choose Tools | Templates.
        lmProfile.addElement(addProfile);
    }

    private void addProfile(String showInputDialog, DownloadSettings.AppProfile appPr) {
        DownloadSettings.AppProfile addProfile = ds.addProfile(showInputDialog, appPr); //To change body of generated methods, choose Tools | Templates.
        lmProfile.addElement(addProfile);
    }

    public void saveConfig() {
        ds.setUseRSync(cbUseRSync.isSelected());
        ds.setListFiles(cbListFiles.isSelected());
        ds.setGrepText(tfGrepText.getText());
        ds.setOutputDir(jtfOutputDir.getText());
        ds.setLfmt(cbLfmtLog.isSelected());
        ds.setProd(cbProdLog.isSelected());
        ds.setTimeProfile((TimeProfile) cbTimeProfile.getSelectedItem());
        ds.setHours(ftHours.getText());
        ds.setActionCommand((GetCommand) cbCommand.getSelectedItem());
//        ds.setTimeRange(dtRange.getTimeRangeAlways());
        ds.setCMDDate(tfDateRegex.getText());
        ds.setCMDTime(tfTimeRegex.getText());
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
        lbDateRegex.setEnabled(timeProfile == TimeProfile.REGEX);
        lbTimeRegex.setEnabled(timeProfile == TimeProfile.REGEX);
        tfDateRegex.setEnabled(timeProfile == TimeProfile.REGEX);
        tfTimeRegex.setEnabled(timeProfile == TimeProfile.REGEX);
    }

    private void updateLFMTs() {
        DefaultComboBoxModel<DownloadSettings.LFMTHostInstance> cb
                = new DefaultComboBoxModel(ds.getLfmtHostInstances().toArray());
        cbLFMTs.setModel(cb);
    }

    private void cbCommandSelectionChanged(GetCommand getCommand) {
        cbUseRSync.setEnabled(getCommand == GetCommand.GET || getCommand == GetCommand.GREPGET);
        cbListFiles.setEnabled(getCommand == GetCommand.LS);
        tfGrepText.setEnabled(getCommand == GetCommand.GREP || getCommand == GetCommand.GREPGET);
        lGrepText.setEnabled(getCommand == GetCommand.GREP || getCommand == GetCommand.GREPGET);

    }

    private void initCB(JComboBox cbCommand, Object selectedObject, Object[] values, Object[] exceptValues) {
        ActionListener[] itemListeners = cbCommand.getActionListeners();
        for (ActionListener itemListener : itemListeners) {
            cbCommand.removeActionListener(itemListener);
        }
        cbCommand.removeAllItems();
        for (Object value : values) {
            boolean skip = false;
            if (exceptValues != null) {
                for (Object exceptValue : exceptValues) {
                    if (exceptValue.equals(value)) {
                        skip = true;
                        break;
                    }
                }
            }
            if (!skip) {
                cbCommand.addItem(value);
            }
        }
        for (ActionListener itemListener : itemListeners) {
            cbCommand.addActionListener(itemListener);
        }
        cbCommand.setSelectedItem(selectedObject);
    }

    public enum TimeProfile {

        VALUE("Last..."),
        REGEX("Date/Time shell regex");

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
        private ArrayList<JButton> addButtons;

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
            this.addButtons = new ArrayList<>();
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
        ButtonPanel buttonPanel;

        @Override
        public ButtonPanel createButtonPanel() {
            buttonPanel = new ButtonPanel();
            for (JButton addButton : addButtons) {
                buttonPanel.add(addButton);
            }
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

        private void addButton(JButton jButton) {
            addButtons.add(jButton);
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
    JTablePopup tabLFMT;

    class EditLFMTDialog extends StandardDialog {

        public EditLFMTDialog() {
            super();
            setTitle("Edit LFMT host");
        }

        class EnterPanel {

            public JPanel getEnterPanel() {
                return enterPanel;
            }

            private final JTextField tbValue;
            private JPanel enterPanel;

            public String getText() {
                return tbValue.getText();
            }

            public void setText(String txt) {
                tbValue.setText(txt);
            }

            EnterPanel(String title) {
                enterPanel = new JPanel();
                enterPanel.setLayout(new BoxLayout(enterPanel, BoxLayout.LINE_AXIS));
                enterPanel.add(new JLabel(title));
                tbValue = new JTextField();
                enterPanel.add(tbValue);
            }
        }

        EnterPanel lfmt;
        EnterPanel lfmtInstance;

        public EnterPanel getLfmt() {
            return lfmt;
        }

        public EnterPanel getLfmtInstance() {
            return lfmtInstance;
        }

        @Override
        public JComponent createBannerPanel() {
            return null;
        }

        @Override
        public JComponent createContentPanel() {
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
            lfmt = new EnterPanel("LFMT host");
            content.add(lfmt.getEnterPanel());
            lfmtInstance = new EnterPanel("LFMT instance");
            content.add(lfmtInstance.getEnterPanel());
            return content;
        }

        @Override
        public ButtonPanel createButtonPanel() {
            ButtonPanel buttonPanel = new ButtonPanel();
            JButton cancelButton = new JButton();
            buttonPanel.addButton(cancelButton);

            cancelButton.setAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    setDialogResult(RESULT_CANCELLED);
                    setVisible(false);
                    dispose();
                }
            });
            cancelButton.setText("Close");

            JButton jbOK = new JButton("OK");
            buttonPanel.addButton(jbOK);

//            listPane.add(jbFilter);
            jbOK.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setDialogResult(RESULT_AFFIRMED);
                    dispose();
                }
            });

            String act = "OK";

            setDefaultCancelAction(cancelButton.getAction());
            setDefaultAction(jbOK.getAction());
            getRootPane().setDefaultButton(jbOK);

            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
            return buttonPanel;
        }

        public boolean doShow() {

            setModal(true);

            pack();

//            ScreenInfo.CenterWindow(this);
            setLocationRelativeTo(getParent());
            setVisible(true);
            return getDialogResult() == StandardDialog.RESULT_AFFIRMED;
        }
    }
}
