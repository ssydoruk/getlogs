/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.ValuesEditor;
import Utils.Pair;
import static Utils.ScreenInfo.CenterWindow;
import Utils.TDateRange;
import Utils.TableColumnAdjuster;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import static com.jidesoft.dialog.StandardDialog.RESULT_CANCELLED;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.FolderChooser;
import com.jidesoft.swing.SearchableUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Stepan
 */
public class SettingsPanel extends javax.swing.JPanel {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private final CheckBoxList clbProfile;
    private final CheckBoxList clbApps;
    private final DefaultListModel<Object> lmProfile;
    private final DefaultListModel<Object> lmApps;
    private TDateRange dtRange;
    private DownloadSettings ds;
    private InfoPanel p = null;
    private InfoPanel lfmtPanes = null;
    private ValuesEditor lfmtEditor = null;
    private final StringListEdit ext;
    private final StringListEdit afterActions;
    private final StringListEdit beforeActions;
    private final String profileTitleBase;
    private final String appTitleBase;
    private SettingsDialog dlg;

    public DownloadSettings getDs() {
        return ds;
    }

    /**
     * Creates new form SettingsPanel
     */
    public SettingsPanel() {
        initComponents();
        dtRange = new TDateRange(true);
        jpRange.add(dtRange);

        TitledBorder border = (TitledBorder) jpProfileBase.getBorder();
        profileTitleBase = border.getTitle();
        appTitleBase = ((TitledBorder) jpProfileBase.getBorder()).getTitle();

        lmProfile = new DefaultListModel<Object>();
        clbProfile = new CheckBoxList(lmProfile);
        jpProfile.add(new JScrollPane(clbProfile));
        clbProfile.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        lmApps = new DefaultListModel<Object>();
        clbApps = new CheckBoxList(lmApps);
        clbApps.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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
        ext = new StringListEdit("Extention");
        ext.setUpdatedFun(new StringListEdit.IDataChangedFun() {
            @Override
            public void dataChanged(ArrayList<Pair<String, Boolean>> newData) {
                DownloadSettings.AppProfile sel = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
                if (sel != null) {
                    sel.setNameSuffixes(newData);
                }
            }
        });

        ext.setAddChoices(new ValuesEditor.IAddChoices() {
            @Override
            public HashSet<String> getAddChoices() {
                HashSet<String> ret = new HashSet<>();
                for (DownloadSettings.AppProfile appProfile : ds.getAppProfiles()) {
                    HashMap<String, Boolean> sfx = appProfile.getNameSuffixes();
                    if (sfx != null) {
                        for (String key : sfx.keySet()) {
                            ret.add(key);
                        }
                    }
                }
                return ret;

            }
        });

        pExtensions.add(ext);
        afterActions = new StringListEdit("After actions");
        afterActions.setUpdatedFun(new StringListEdit.IDataChangedFun() {
            @Override
            public void dataChanged(ArrayList<Pair<String, Boolean>> newData) {
                getDs().setAfterActions(newData);
            }
        });
        pAfterActions.add(afterActions);

        beforeActions = new StringListEdit("Before actions");
        beforeActions.setUpdatedFun(new StringListEdit.IDataChangedFun() {
            @Override
            public void dataChanged(ArrayList<Pair<String, Boolean>> newData) {
                getDs().setBeforeActions(newData);
            }
        });
        pBeforeActions.add(beforeActions);
//        tfFilenameSuffixes.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                tfFilenameSuffixesChanged();
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//                tfFilenameSuffixesChanged();
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//                tfFilenameSuffixesChanged();
//            }
//        });
//        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        tfGrepText.setMaximumSize(new Dimension(tfGrepText.getMaximumSize().width, tfGrepText.getMinimumSize().height));
        jtfOutputDir.setMaximumSize(new Dimension(jtfOutputDir.getMaximumSize().width, jtfOutputDir.getMinimumSize().height));
        tfDateRegex.setMaximumSize(new Dimension(tfDateRegex.getMaximumSize().width, tfDateRegex.getMinimumSize().height));
        tfTimeRegex.setMaximumSize(new Dimension(tfTimeRegex.getMaximumSize().width, tfTimeRegex.getMinimumSize().height));
        ftHours.setMaximumSize(new Dimension(ftHours.getMaximumSize().width, ftHours.getMinimumSize().height));
        jpCommandParams.setMaximumSize(new Dimension(jpCommandParams.getMaximumSize().width, jpCommandParams.getMinimumSize().height));
    }

    private void tfFilenameSuffixesChanged() {
        DownloadSettings.AppProfile pr = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
        if (pr != null) {
//            pr.setNameSuffixes(tfFilenameSuffixes.getText());
        }
    }

    private void clbProfileCheckedChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting()) {
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
            int maxSelectionIndex = lsm.getMaxSelectionIndex();
            int minSelectionIndex = lsm.getMinSelectionIndex();
//                    System.out.println("-1-" + evt
//                            + " f: " + evt.getFirstIndex()
//                            + " l: " + evt.getLastIndex()
//                            + " min: " + minSelectionIndex + " max: " + maxSelectionIndex
//                    );
            for (int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++) {
                if (i < lmProfile.getSize()) {
                    ((DownloadSettings.AppProfile) lmProfile.getElementAt(i))
                            .setSelected(lsm.isSelectedIndex(i));
                }
            }
            updateProfileTitle();
            updateStartButton();
//                }
//            });
        }
    }

    private boolean canRunProfiles() {
//(DownloadSettings.AppProfile) lmProfile.get(minIndex);

        CheckBoxListSelectionModel lsm = clbProfile.getCheckBoxListSelectionModel();
        DefaultListModel<Object> lm = (DefaultListModel<Object>) clbProfile.getModel();
        int allEntryIndex = lsm.getAllEntryIndex();
        for (int i = 0; i < lm.getSize(); i++) {
            if (lsm.isSelectedIndex(i) && i != allEntryIndex) {
                DownloadSettings.AppProfile elementAt = (DownloadSettings.AppProfile) lm.getElementAt(i);
                if (elementAt != null && hasCheckedApp(elementAt)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getSelectedNum(CheckBoxList clb) {
        int numSelected = 0;
//(DownloadSettings.AppProfile) lmProfile.get(minIndex);

        CheckBoxListSelectionModel lsm = clb.getCheckBoxListSelectionModel();
        DefaultListModel<Object> lm = (DefaultListModel<Object>) clb.getModel();
        int allEntryIndex = lsm.getAllEntryIndex();
        for (int i = 0; i < lm.getSize(); i++) {
            if (lsm.isSelectedIndex(i) && i != allEntryIndex) {
                numSelected++;
            }
        }
        return numSelected;
    }

    private int getAllSize(CheckBoxList clb) {
        DefaultListModel<Object> lm = (DefaultListModel<Object>) clb.getModel();
        return (clb.getCheckBoxListSelectionModel().getAllEntryIndex() >= 0) ? lm.size() - 1 : lm.size();

    }

    private void updateAppTitle() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                int numSelected = getSelectedNum(clbApps);
                int allSize = getAllSize(clbApps);

                TitledBorder border = (TitledBorder) jpAppsBase.getBorder();
                if (allSize <= 0) {
                    border.setTitle(appTitleBase);
                } else {
                    border.setTitle(appTitleBase + " (" + numSelected + "/" + (getAllSize(clbApps)) + ")");
                }

                jpAppsBase.repaint();
                updateStartButton();
            }
        });

    }

    private void updateProfileTitle() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                ((TitledBorder) jpProfileBase.getBorder()).setTitle(profileTitleBase + " (" + getSelectedNum(clbProfile) + "/" + (getAllSize(clbProfile)) + ")");

                jpProfileBase.repaint();
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
//            System.out.println("clbProfileSelectionChanged List item changed - " + evt);
            ListSelectionModel lsm = (ListSelectionModel) evt.getSource();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    clbApps.setValueIsAdjusting(true);
                    int minIndex = lsm.getMinSelectionIndex();
                    int maxIndex = lsm.getMaxSelectionIndex();
                    boolean singleSelection = (minIndex == maxIndex && minIndex >= 0);
                    if (singleSelection) { //one item selected
                        profileSelectionChanged((DownloadSettings.AppProfile) lmProfile.get(minIndex));
                    } else {
                        clbApps.setValueIsAdjusting(true);
                        DefaultListModel lm = (DefaultListModel) clbApps.getModel();
                        lm.removeAllElements();
                        clbApps.setValueIsAdjusting(false);
                        ext.noSelection();
                    }
                    int numSelected = (minIndex >= 0) ? (maxIndex - minIndex + 1) : 0;
                    profileSelected(numSelected);
                    clbApps.setValueIsAdjusting(false);

                }

            });

        }
    }

    private void profileSelectionChanged(DownloadSettings.AppProfile pr) {

        clbApps.setValueIsAdjusting(true);
        CheckBoxListSelectionModel clbAppSelectionModel = clbApps.getCheckBoxListSelectionModel();
        ListSelectionListener[] listSelectionListeners = clbAppSelectionModel.getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            clbAppSelectionModel.removeListSelectionListener(listSelectionListener);
        }

//                        clbAppSelectionModel.addSelectionInterval(maxIndex, maxIndex);
        clbAppSelectionModel.clearSelection();
        lmApps.clear();
        List<DownloadSettings.App> apps = new ArrayList<>(pr.getApps());
        Collections.sort(apps);
        for (DownloadSettings.App app : apps) {
            lmApps.addElement(app);
        }
//        Collections.sort( lmApps);

        if (!lmApps.isEmpty()) {
            lmApps.insertElementAt(CheckBoxList.ALL_ENTRY, 0);
            for (int i = 1; i < lmApps.getSize(); i++) {
                if (((DownloadSettings.App) lmApps.getElementAt(i)).isChecked()) {
                    clbAppSelectionModel.addSelectionInterval(i, i);
                }
            }
        }
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            clbAppSelectionModel.addListSelectionListener(listSelectionListener);
        }

        rbGenesysLogs.setSelected(pr.isIsGenesysName());
        rbCloudLogs.setSelected(!pr.isIsGenesysName());
        ext.setData(pr.getNameSuffixes());
//        tfFilenameSuffixes.setText(pr.getNameSuffixes());

        DownloadSettings.LFMTHostInstance lfmtHostInstance = pr.getLFMT();
        int sel = -1;
        if (lfmtHostInstance != null) {
            DefaultComboBoxModel<DownloadSettings.LFMTHostInstance> mod = (DefaultComboBoxModel<DownloadSettings.LFMTHostInstance>) cbLFMTs.getModel();
            for (int i = 0; i < mod.getSize(); i++) {
                DownloadSettings.LFMTHostInstance inst = mod.getElementAt(i);
                logger.info(inst);
                if (inst != null && inst.getHost() != null
                        && inst.getInstance() != null
                        && inst.getBaseDir() != null
                        && inst.getHost().equals(lfmtHostInstance.getHost())
                        && inst.getInstance().equals(lfmtHostInstance.getInstance())
                        && inst.getBaseDir().equals(lfmtHostInstance.getBaseDir())) {
                    sel = i;
                    break;
                }
            }
        }
        if (sel >= 0) {
            cbLFMTs.setSelectedIndex(sel);
        } else {
//            cbLFMTs.setSelectedIndex(0);
            pr.setLFMT((DownloadSettings.LFMTHostInstance) cbLFMTs.getSelectedItem());
//                        clbApps.setCheckBoxListSelectionModel(clbAppSelectionModel);
//                                clbApps.selectAll();
        }
        updateAppTitle();
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

                    appSelected((minIndex >= 0));
                }

            });

        }
    }

    private void clbAppsCheckedChanged(ListSelectionEvent evt) {

        if (!evt.getValueIsAdjusting()) {
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    int maxSelectionIndex = lsm.getMaxSelectionIndex();
                    int minSelectionIndex = lsm.getMinSelectionIndex();
//                    System.out.println("-1-" + evt
//                            + " f: " + evt.getFirstIndex()
//                            + " l: " + evt.getLastIndex()
//                            + " min: " + minSelectionIndex + " max: " + maxSelectionIndex
//                    );
                    for (int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++) {
                        if (!lmApps.isEmpty() && i < lmApps.getSize()) {
                            if (lmApps.getElementAt(i) instanceof DownloadSettings.App) {
                                ((DownloadSettings.App) lmApps.getElementAt(i))
                                        .setChecked(lsm.isSelectedIndex(i));
                            }
                        }

                    }
                }
            });
            updateAppTitle();
        }

    }

    private void appSelected(boolean itemsSelected) {
        jbAppDelete.setEnabled(itemsSelected);
        jpAppProperties.setEnabled(itemsSelected);
//        rbCloudLogs.setEnabled(singleSelection);
//        rbGenesysLogs.setEnabled(singleSelection);
//        cbLFMTs.setEnabled(singleSelection);
//        btEditLFMTs.setEnabled(singleSelection);
    }

    private void profileSelected(int numSelected) {
        jpAppsBase.setEnabled(numSelected == 1);
        jpAppProperties.setEnabled(numSelected == 1);
        jbProfileDelete.setEnabled(numSelected > 0);
        jbProfileRename.setEnabled(numSelected == 1);
        jbProfileSaveAs.setEnabled(numSelected == 1);
        jbAppAdd.setEnabled(numSelected == 1);
        rbCloudLogs.setEnabled(numSelected == 1);
        rbGenesysLogs.setEnabled(numSelected == 1);
        cbLFMTs.setEnabled(numSelected == 1);
        btEditLFMTs.setEnabled(numSelected == 1);
        ext.setEnabled(numSelected == 1);

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

    SettingsPanel(DownloadSettings ds, SettingsDialog dlg) {
        this();
        this.ds = ds;
        this.dlg = dlg;
        loadConfig();
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
        jpLastFiles = new javax.swing.JPanel();
        ftHours = new javax.swing.JFormattedTextField();
        lHours = new javax.swing.JLabel();
        jpRegEx = new javax.swing.JPanel();
        jPanel25 = new javax.swing.JPanel();
        lbDateRegex = new javax.swing.JLabel();
        tfDateRegex = new javax.swing.JTextField();
        jPanel26 = new javax.swing.JPanel();
        lbTimeRegex = new javax.swing.JLabel();
        tfTimeRegex = new javax.swing.JTextField();
        jpRange = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jpProfileBase = new javax.swing.JPanel();
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
        jLabel3 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        cbLFMTs = new javax.swing.JComboBox();
        btEditLFMTs = new javax.swing.JButton();
        pExtensions = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        cbLfmtLog = new javax.swing.JCheckBox();
        cbProdLog = new javax.swing.JCheckBox();
        jpRangeSelect = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        cbTimeProfile = new javax.swing.JComboBox<>();
        jpRangeParams = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jpCommandParams = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        lCommand = new javax.swing.JLabel();
        cbCommand = new javax.swing.JComboBox();
        jPanel18 = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        jPanel22 = new javax.swing.JPanel();
        cbUseRSync = new javax.swing.JCheckBox();
        jPanel23 = new javax.swing.JPanel();
        cbLCALogs = new javax.swing.JCheckBox();
        cbAppLogs = new javax.swing.JCheckBox();
        jPanel21 = new javax.swing.JPanel();
        lGrepText = new javax.swing.JLabel();
        tfGrepText = new javax.swing.JTextField();
        jpDownloadParams = new javax.swing.JPanel();
        jPanel24 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jtfOutputDir = new javax.swing.JTextField();
        jbSelectDirectory = new javax.swing.JButton();
        pBeforeActions = new javax.swing.JPanel();
        pAfterActions = new javax.swing.JPanel();

        jpLastFiles.setLayout(new javax.swing.BoxLayout(jpLastFiles, javax.swing.BoxLayout.LINE_AXIS));

        ftHours.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0"))));
        jpLastFiles.add(ftHours);
        jpLastFiles.add(lHours);

        jpRegEx.setLayout(new javax.swing.BoxLayout(jpRegEx, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel25.setLayout(new javax.swing.BoxLayout(jPanel25, javax.swing.BoxLayout.LINE_AXIS));

        lbDateRegex.setText("Date shell regex(YYYYMMDD digits, [-])");
        jPanel25.add(lbDateRegex);
        jPanel25.add(tfDateRegex);

        jpRegEx.add(jPanel25);

        jPanel26.setLayout(new javax.swing.BoxLayout(jPanel26, javax.swing.BoxLayout.LINE_AXIS));

        lbTimeRegex.setText("Time shell regex(HHMMSS digits, [-])");
        jPanel26.add(lbTimeRegex);
        jPanel26.add(tfTimeRegex);

        jpRegEx.add(jPanel26);

        jpRange.setLayout(new java.awt.BorderLayout());

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Application profiles"));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        jpProfileBase.setBorder(javax.swing.BorderFactory.createTitledBorder("Profile"));
        jpProfileBase.setLayout(new javax.swing.BoxLayout(jpProfileBase, javax.swing.BoxLayout.PAGE_AXIS));

        jpProfile.setLayout(new java.awt.BorderLayout());
        jpProfileBase.add(jpProfile);

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

        jpProfileBase.add(jPanel8);

        jPanel1.add(jpProfileBase);

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

        jpAppProperties.setLayout(new javax.swing.BoxLayout(jpAppProperties, javax.swing.BoxLayout.PAGE_AXIS));

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

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        jLabel3.setText("lfmt");
        jPanel6.add(jLabel3);

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

        jPanel6.add(jPanel13);

        jpAppProperties.add(jPanel6);

        pExtensions.setBorder(javax.swing.BorderFactory.createTitledBorder("Name suffixes (.=empty)"));
        pExtensions.setLayout(new java.awt.BorderLayout());
        jpAppProperties.add(pExtensions);

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

        jpRangeSelect.setBorder(javax.swing.BorderFactory.createTitledBorder("Range select"));
        jpRangeSelect.setLayout(new javax.swing.BoxLayout(jpRangeSelect, javax.swing.BoxLayout.LINE_AXIS));

        jPanel11.setLayout(new javax.swing.BoxLayout(jPanel11, javax.swing.BoxLayout.LINE_AXIS));

        cbTimeProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbTimeProfileActionPerformed(evt);
            }
        });
        jPanel11.add(cbTimeProfile);

        jpRangeSelect.add(jPanel11);

        jpRangeParams.setLayout(new java.awt.BorderLayout());
        jpRangeSelect.add(jpRangeParams);

        jPanel2.add(jpRangeSelect);

        add(jPanel2);

        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.PAGE_AXIS));

        jpCommandParams.setLayout(new javax.swing.BoxLayout(jpCommandParams, javax.swing.BoxLayout.LINE_AXIS));

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

        jpCommandParams.add(jPanel16);

        jPanel18.setLayout(new javax.swing.BoxLayout(jPanel18, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel20.setLayout(new javax.swing.BoxLayout(jPanel20, javax.swing.BoxLayout.LINE_AXIS));

        jPanel22.setLayout(new java.awt.BorderLayout());

        cbUseRSync.setText("Use RSync");
        jPanel22.add(cbUseRSync, java.awt.BorderLayout.CENTER);

        jPanel20.add(jPanel22);

        jPanel23.setLayout(new java.awt.BorderLayout());

        cbLCALogs.setText("LCA logs");
        cbLCALogs.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbLCALogsItemStateChanged(evt);
            }
        });
        jPanel23.add(cbLCALogs, java.awt.BorderLayout.CENTER);

        cbAppLogs.setText("Application logs");
        cbAppLogs.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbAppLogsItemStateChanged(evt);
            }
        });
        jPanel23.add(cbAppLogs, java.awt.BorderLayout.PAGE_START);

        jPanel20.add(jPanel23);

        jPanel18.add(jPanel20);

        jPanel21.setLayout(new javax.swing.BoxLayout(jPanel21, javax.swing.BoxLayout.LINE_AXIS));

        lGrepText.setText("text to grep");
        jPanel21.add(lGrepText);
        jPanel21.add(tfGrepText);

        jPanel18.add(jPanel21);

        jpCommandParams.add(jPanel18);

        jPanel9.add(jpCommandParams);

        jpDownloadParams.setLayout(new javax.swing.BoxLayout(jpDownloadParams, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel24.setLayout(new javax.swing.BoxLayout(jPanel24, javax.swing.BoxLayout.LINE_AXIS));

        jLabel2.setText("Output directory");
        jPanel24.add(jLabel2);
        jPanel24.add(jtfOutputDir);

        jbSelectDirectory.setText("...");
        jbSelectDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSelectDirectoryActionPerformed(evt);
            }
        });
        jPanel24.add(jbSelectDirectory);

        jpDownloadParams.add(jPanel24);

        pBeforeActions.setBorder(javax.swing.BorderFactory.createTitledBorder("Before download actions"));
        pBeforeActions.setLayout(new javax.swing.BoxLayout(pBeforeActions, javax.swing.BoxLayout.LINE_AXIS));
        jpDownloadParams.add(pBeforeActions);

        pAfterActions.setBorder(javax.swing.BorderFactory.createTitledBorder("Post download actions"));
        pAfterActions.setLayout(new javax.swing.BoxLayout(pAfterActions, javax.swing.BoxLayout.LINE_AXIS));
        jpDownloadParams.add(pAfterActions);

        jPanel9.add(jpDownloadParams);

        add(jPanel9);
    }// </editor-fold>//GEN-END:initComponents

    private void jbProfileAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileAddActionPerformed
        String name = getProfileName("Enter new profile name", null);

        if (name != null) {
            addProfile(name);
        }
    }//GEN-LAST:event_jbProfileAddActionPerformed

    private void jbSelectDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSelectDirectoryActionPerformed
        JFileChooser fc = null;
        String curDir = jtfOutputDir.getText();

        if (curDir != null && !curDir.isEmpty()) {
            File f = new File(curDir);
            if (f.isDirectory()) {
                fc = new FolderChooser(f);
            }

        }
        if (fc == null) {
            fc = new FolderChooser();
        }
//        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//In response to a button click:
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            jtfOutputDir.setText(fc.getSelectedFile().getAbsolutePath());
        }

    }//GEN-LAST:event_jbSelectDirectoryActionPerformed

    private void jbProfileDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileDeleteActionPerformed
        List<DownloadSettings.AppProfile> selectedValuesList = clbProfile.getSelectedValuesList();
        if (selectedValuesList != null && !selectedValuesList.isEmpty()) {
            StringBuilder sPrompt = new StringBuilder();
            sPrompt.append("Do you really want to delete ");
            if (selectedValuesList.size() == 1) {
                sPrompt.append("profile [").append(selectedValuesList.get(0).getName()).append("]");
            } else {
                sPrompt.append(selectedValuesList.size()).append(" profiles ");
            };

            if (JOptionPane.showConfirmDialog((Window) this.getRootPane().getParent(),
                    sPrompt.toString(), "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                for (DownloadSettings.AppProfile appProfile : selectedValuesList) {
                    ds.removeProfile(appProfile);
                }
                loadProfile(null);
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
        DownloadSettings.AppProfile prof = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
        if (prof != null) {
            prof.setIsGenesysName(evt.getStateChange() == ItemEvent.SELECTED);
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
            p = new InfoPanel((Window) this.getRootPane().getParent(), "Select applications", tab,
                    "Add %d apps");
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
                String app = (String) infoTableModel.getValueAt(selectedRow, 0);

                DownloadSettings.App addApp = profile.addApp(app, GetLogs.getHosts().getAppDir(app));
//                lmApps.addElement(addApp);
//                selValues.add((String) infoTableModel.getValueAt(selectedRow, 0));
            }
            profileSelectionChanged(profile);
//            profileSelected(true);
        }

    }//GEN-LAST:event_jbAppAddActionPerformed

    private void jbAppDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAppDeleteActionPerformed
        DownloadSettings.AppProfile appPr = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
        List<DownloadSettings.App> selectedValuesList = clbApps.getSelectedValuesList();
        if (appPr != null && selectedValuesList != null) {
            StringBuilder sPrompt = new StringBuilder();
            sPrompt.append("Do you really want to delete ");
            if (selectedValuesList.size() == 1) {
                sPrompt.append("app [" + selectedValuesList.get(0).getName() + "]");
            } else {
                sPrompt.append(selectedValuesList.size()).append(" applications ");
            }
            sPrompt.append(" from profile [")
                    .append(appPr.getName())
                    .append("]");
            if (JOptionPane.showConfirmDialog(this, sPrompt, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                for (DownloadSettings.App app : selectedValuesList) {
                    appPr.removeApp(app);
                    lmApps.removeElement(app);
                    if (lmApps.size() == 1 && (lmApps.getElementAt(0).equals(CheckBoxList.ALL_ENTRY))) {
                        lmApps.remove(0);
                    }

                }
            }
        }
    }//GEN-LAST:event_jbAppDeleteActionPerformed

    private void btEditLFMTsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btEditLFMTsActionPerformed

        if (lfmtEditor == null) {
            lfmtEditor = new ValuesEditor((Window) this.getRootPane().getParent(), "List of LFMTs",
                    "Select %d LFMTs");

        }
        ArrayList<Object[]> values = new ArrayList<>();
        for (DownloadSettings.LFMTHostInstance hi : ds.getLfmtHostInstances()) {
            values.add(new Object[]{hi.getHost(), hi.getInstance(), hi.getBaseDir()});
        }
        lfmtEditor.setData(new Object[]{"LFMT host", "LFMT instance", "Base directory"},
                values
        );
        if (lfmtEditor.doShow()) {
            ds.loadLFMTs(lfmtEditor.getData());
        }

//        if (lfmtPanes == null) {
//            tabLFMT = getJTablePopup();
//            lfmtPanes = new InfoPanel((Window) this.getRootPane().getParent(), "List of LFMTs", tabLFMT,
//                    "Select %d LFMTs");
//        }
//        DefaultTableModel infoTableModel = new DefaultTableModel();
//        infoTableModel.addColumn("LFMT host");
//        infoTableModel.addColumn("LFMT instance");
//        infoTableModel.addColumn("Base directory");
//        for (DownloadSettings.LFMTHostInstance hi : ds.getLfmtHostInstances()) {
//            infoTableModel.addRow(new Object[]{hi.getHost(), hi.getInstance(), hi.getBaseDir()});
//        }
//        tabLFMT.setModel(infoTableModel);
//        JButton bt;
//        bt = new JButton(new AbstractAction("add") {
//            private EditLFMTDialog dlg = null;
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (dlg == null) {
//                    dlg = new EditLFMTDialog();
//                }
//                if (dlg.doShow()) {
//                    newLFMTInstance(dlg.getLfmt().tbValue.getText(), dlg.getLfmtInstance().tbValue.getText(), dlg.getBaseDir().tbValue.getText());
//                }
//            }
//
//            private void newLFMTInstance(String host, String instance, String baseDir) {
//                DownloadSettings.LFMTHostInstance lfmt = ds.addLFMTInstance(host, instance, baseDir);
//                DefaultTableModel mod = (DefaultTableModel) tabLFMT.getModel();
//                mod.addRow(new Object[]{lfmt.getHost(), lfmt.getInstance(), lfmt.getBaseDir()});
//            }
//        });
//        lfmtPanes.addButton(bt);
//
//        lfmtPanes.doShow();
//        updateLFMTs();

    }//GEN-LAST:event_btEditLFMTsActionPerformed

    private void cbLFMTsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbLFMTsItemStateChanged

        if (evt.getStateChange() == ItemEvent.SELECTED) {
            Object item = evt.getItem();
            DownloadSettings.AppProfile prof = (DownloadSettings.AppProfile) clbProfile.getSelectedValue();
            if (prof != null) {
                prof.setLFMT((DownloadSettings.LFMTHostInstance) item);
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

    private void cbAppLogsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbAppLogsItemStateChanged
        updateStartButton();

    }//GEN-LAST:event_cbAppLogsItemStateChanged

    private void cbLCALogsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbLCALogsItemStateChanged
        updateStartButton();
    }//GEN-LAST:event_cbLCALogsItemStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgFileNaming;
    private javax.swing.JButton btEditLFMTs;
    private javax.swing.JCheckBox cbAppLogs;
    private javax.swing.JComboBox cbCommand;
    private javax.swing.JCheckBox cbLCALogs;
    private javax.swing.JComboBox cbLFMTs;
    private javax.swing.JCheckBox cbLfmtLog;
    private javax.swing.JCheckBox cbProdLog;
    private javax.swing.JComboBox<String> cbTimeProfile;
    private javax.swing.JCheckBox cbUseRSync;
    private javax.swing.JFormattedTextField ftHours;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
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
    private javax.swing.JPanel jpCommandParams;
    private javax.swing.JPanel jpDownloadParams;
    private javax.swing.JPanel jpLastFiles;
    private javax.swing.JPanel jpProfile;
    private javax.swing.JPanel jpProfileBase;
    private javax.swing.JPanel jpRange;
    private javax.swing.JPanel jpRangeParams;
    private javax.swing.JPanel jpRangeSelect;
    private javax.swing.JPanel jpRegEx;
    private javax.swing.JTextField jtfOutputDir;
    private javax.swing.JLabel lCommand;
    private javax.swing.JLabel lGrepText;
    private javax.swing.JLabel lHours;
    private javax.swing.JLabel lbDateRegex;
    private javax.swing.JLabel lbTimeRegex;
    private javax.swing.JPanel pAfterActions;
    private javax.swing.JPanel pBeforeActions;
    private javax.swing.JPanel pExtensions;
    private javax.swing.JRadioButton rbCloudLogs;
    private javax.swing.JRadioButton rbGenesysLogs;
    private javax.swing.JTextField tfDateRegex;
    private javax.swing.JTextField tfGrepText;
    private javax.swing.JTextField tfTimeRegex;
    // End of variables declaration//GEN-END:variables

    private void loadProfile(DownloadSettings.AppProfile activeProfile) {
        CheckBoxListSelectionModel checkBoxListSelectionModel = clbProfile.getCheckBoxListSelectionModel();
        ListSelectionListener[] listSelectionListeners = checkBoxListSelectionModel.getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            checkBoxListSelectionModel.removeListSelectionListener(listSelectionListener);
        }
        lmProfile.clear();
        int selIdx = -1;
        for (DownloadSettings.AppProfile appProfile : ds.getAppProfilesSorted()) {
            lmProfile.addElement(appProfile);
            int idx = lmProfile.size() - 1;
            if (activeProfile == appProfile) {
                selIdx = idx;
            }
            if (appProfile.isSelected()) {
                checkBoxListSelectionModel.addSelectionInterval(idx, idx);
            }
        }
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            checkBoxListSelectionModel.addListSelectionListener(listSelectionListener);
        }
        clbProfile.setCheckBoxListSelectionModel(checkBoxListSelectionModel);
        if (selIdx >= 0) {
            clbProfile.setSelectedIndex(selIdx);
        }

        int[] selectedIndices = clbProfile.getSelectedIndices();
        profileSelected(selectedIndices.length);

    }

    private void loadConfig() {
        loadProfile(null);

//        cbListFiles.setSelected(ds.isListFiles());
        cbLfmtLog.setSelected(ds.isLfmt());
        cbProdLog.setSelected(ds.isProd());
        cbAppLogs.setSelected(ds.isAppLogs());
        cbLCALogs.setSelected(ds.isLcaLogs());
        jtfOutputDir.setText(ds.getOutputDir());
        cbUseRSync.setSelected(ds.isUseRSync());
        afterActions.setData(ds.getAfterActions(), true);
        beforeActions.setData(ds.getBeforeActions(), true);
//        afterActions.setMaximumSize(new Dimension(afterActions.getMaximumSize().width, afterActions.getHeight()));
//        pAfterActions.setMaximumSize(new Dimension(pAfterActions.getMaximumSize().width, afterActions.getHeight()));

        initCB(cbTimeProfile, ds.getTimeProfile(), new TimeProfile[]{TimeProfile.VALUE_FILES, TimeProfile.REGEX, TimeProfile.RANGE}, null);

        ftHours.setText(ds.getHours());
        dtRange.setTimeRange(ds.getTimeRange());
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
        updateProfileTitle();
    }

    private void addProfile(String showInputDialog) {
//        DownloadSettings.AppProfile addProfile = ds.addProfile(showInputDialog); //To change body of generated methods, choose Tools | Templates.
//        lmProfile.addElement(addProfile);
        DownloadSettings.AppProfile addProfile = ds.addProfile(showInputDialog);
        loadProfile(addProfile);
    }

    private void addProfile(String showInputDialog, DownloadSettings.AppProfile appPr) {
//        DownloadSettings.AppProfile addProfile = ds.addProfile(showInputDialog, appPr); //To change body of generated methods, choose Tools | Templates.
//        lmProfile.addElement(addProfile);
        DownloadSettings.AppProfile addProfile = ds.addProfile(showInputDialog, appPr);
        loadProfile(addProfile);
    }

    public void saveConfig() {
        ds.setUseRSync(cbUseRSync.isSelected());
//        ds.setListFiles(cbListFiles.isSelected());
        ds.setGrepText(tfGrepText.getText());
        ds.setOutputDir(jtfOutputDir.getText());
        ds.setLfmt(cbLfmtLog.isSelected());
        ds.setProd(cbProdLog.isSelected());
        ds.setLcaLogs(cbLCALogs.isSelected());
        ds.setAppLogs(cbAppLogs.isSelected());
        ds.setTimeProfile((TimeProfile) cbTimeProfile.getSelectedItem());
        ds.setHours(ftHours.getText());
        ds.setActionCommand((GetCommand) cbCommand.getSelectedItem());
//        ds.setAfterActions(afterActions.getData());
        ds.setTimeRange(dtRange.getTimeRangeAlways());
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
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(GetLogs.getsGUIProfile()));
            gson.toJson(ds, writer);

            writer.close();
        } catch (FileNotFoundException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        } catch (IOException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        }

    }

    private void timeProfileChanged(TimeProfile timeProfile) {
        jpRangeParams.removeAll();
        JPanel jpToAdd = null;
        switch (timeProfile) {
            case REGEX:
                jpToAdd = jpRegEx;
                break;
            case RANGE:
                jpToAdd = jpRange;

                break;
            case VALUE_FILES:
                jpToAdd = jpLastFiles;
                break;
        }
        if (jpToAdd != null) {
            LayoutManager layout = jpRangeParams.getLayout();
            jpRangeParams.add(jpToAdd);
//            jpToAdd.setVisible(true);
//            jpToAdd.invalidate();
            jpRangeParams.setMaximumSize(new Dimension(jpRangeParams.getMaximumSize().width, jpRangeParams.getMinimumSize().height));

            jpRangeSelect.revalidate();
//            jpRangeSelect.repaint();
        }
//        ftHours.setEnabled(timeProfile == TimeProfile.VALUE_HOURS || timeProfile == TimeProfile.VALUE_FILES);
//        lHours.setEnabled(timeProfile == TimeProfile.VALUE_HOURS || timeProfile == TimeProfile.VALUE_FILES);
//        lHours.setText(((timeProfile == TimeProfile.REGEX) ? "" : ((timeProfile == TimeProfile.VALUE_FILES) ? "files" : "hours")));
//        lbDateRegex.setEnabled(timeProfile == TimeProfile.REGEX);
//        lbTimeRegex.setEnabled(timeProfile == TimeProfile.REGEX);
//        tfDateRegex.setEnabled(timeProfile == TimeProfile.REGEX);
//        tfTimeRegex.setEnabled(timeProfile == TimeProfile.REGEX);
    }

    private void updateLFMTs() {
        DefaultComboBoxModel<DownloadSettings.LFMTHostInstance> cb
                = new DefaultComboBoxModel(ds.getLfmtHostInstances().toArray());
        cbLFMTs.setModel(cb);
    }

    private void cbCommandSelectionChanged(GetCommand getCommand) {
        cbUseRSync.setEnabled(getCommand == GetCommand.GET || getCommand == GetCommand.GREPGET);
//        cbListFiles.setEnabled(getCommand == GetCommand.LS);
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

    private void updateStartButton() {
        dlg.setJBRunEnabled(
                canRun()
        );
    }

    boolean canRun() {
        return canRunProfiles()
                && (cbAppLogs.isSelected() || cbLCALogs.isSelected());
    }

    private boolean hasCheckedApp(DownloadSettings.AppProfile elementAt) {
        for (DownloadSettings.App app : elementAt.getApps()) {
            if (app.isChecked()) {
                return true;
            }
        }
        return false;
    }

    public enum TimeProfile {

        VALUE_HOURS("Last hours:"),
        VALUE_FILES("Last files:"),
        REGEX("Date/Time shell regex"),
        RANGE("Should cover range");

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

    public static class InfoPanel extends StandardDialog {

        private int closeCause = JOptionPane.CANCEL_OPTION;
        private JTable theTab;
        private ArrayList<JButton> addButtons;
        private final String selectedFormat;
        private TableColumnAdjuster tca;

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
                    : String.format(selectedFormat, rowsSelected));
        }

        InfoPanel(Window parent, String title, JTable tab, String selectedFormat) {
            super(parent, title);
            this.addButtons = new ArrayList<>();
            this.theTab = tab;
            this.selectedFormat = selectedFormat;
            tca = new TableColumnAdjuster(theTab);
            tca.setColumnHeaderIncluded(true);
            jScrollPane = new JScrollPane(theTab);
            theTab.getTableHeader().setVisible(true);
            theTab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        }

        public void doShow() {
            this.setLocationRelativeTo(getParent());
            tca.adjustColumns();

            Dimension d = theTab.getPreferredSize();

            d.height = 500;
            d.width += jScrollPane.getVerticalScrollBar().getMaximumSize().width + 4;
            jScrollPane.setPreferredSize(d);
            pack();

            CenterWindow(this);

            if (logger.isTraceEnabled()) {
                logger.trace("Show info PanelDialog; title=" + getTitle() + "; tab cols:" + theTab.getColumnCount() + " rows: " + theTab.getRowCount());
                StringBuilder s = new StringBuilder(512);
                for (int i = 0; i < theTab.getRowCount(); i++) {
                    s.setLength(0);
                    for (int j = 0; j < theTab.getColumnCount(); j++) {
                        s.append("[" + theTab.getValueAt(i, j) + "],");
                    }
                    logger.trace(s);
                }

            }

            setVisible(true);
        }

        @Override
        public JComponent createBannerPanel() {
            return null;
        }
        JScrollPane jScrollPane;

        @Override
        public JComponent createContentPanel() {
//                        JPanel panel = new JPanel(new BorderLayout(10, 10));
//            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

//            panel.add(mainPanel, BorderLayout.CENTER);
//            return panel;
            JPanel listPane = new JPanel(new BorderLayout());

            listPane.add(jScrollPane, BorderLayout.CENTER);

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
            theTab.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    selectionChanged(jbFilter, theTab);
                }
            });
            selectionChanged(jbFilter, theTab);

//            listPane.add(jbFilter);
            jbFilter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    theTab.editingCanceled(null);
                    setCloseCause(JOptionPane.OK_OPTION);
                    dispose();
                }
            });

            String act = "ApplyFilter";

            theTab.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), act);
            theTab.getActionMap().put(act, jbFilter.getAction());

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

        private EnterPanel lfmtBaseDir;

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

        public EnterPanel getBaseDir() {
            return lfmtBaseDir;
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
            lfmtBaseDir = new EnterPanel("Log base dir");
            content.add(lfmtBaseDir.getEnterPanel());
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
