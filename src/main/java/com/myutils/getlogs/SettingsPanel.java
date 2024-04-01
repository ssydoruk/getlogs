/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.*;
import Utils.TDateRange;
import Utils.swing.*;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import com.github.lgooddatepicker.optionalusertools.PickerUtilities;
import com.google.gson.*;
import com.jidesoft.swing.*;
import static com.myutils.getlogs.GetLogs.logger;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Stepan
 */
public class SettingsPanel extends javax.swing.JPanel {

    private final CheckBoxList clbProfile;
    private final CheckBoxList clbApps;
    private final DefaultListModel<Object> lmProfile;
    private final DefaultListModel<Object> lmApps;
    private DownloadSettings ds;
    private InfoPanel p = null;
    private Utils.swing.ValuesEditor loginProfilesEditor = null;
    private final StringListEdit ext;
    private final StringListEdit afterActions;
    private final StringListEdit beforeActions;
    private final String profileTitleBase;
    private final String appTitleBase;
    private SettingsForm dlg;
    private final String profileAllTitleBase;
    private int appIdx = -1;
    private DateTimePicker dtFrom;
    final SimpleDateFormat simpleDateFormat;
    final SimpleDateFormat simpleTimeFormat;
    RegexType regexType;

    public DownloadSettings getDs() {
        return ds;
    }

    private DateTimePicker newPicker(String dateFormat) {
        DateTimePicker dateTimePicker1 = new DateTimePicker();
//        dateTimePicker1.datePicker.setDate(LocalDate.now());
//        dateTimePicker1.timePicker.setTimeToNow();
        dateTimePicker1.getDatePicker().setBorder(null);
        TimePickerSettings timeSettings = dateTimePicker1.getTimePicker().getSettings();
        timeSettings.setFormatForDisplayTime(PickerUtilities.createFormatterFromPatternString(
                dateFormat, timeSettings.getLocale()));
        timeSettings.setInitialTimeToNow();
        timeSettings.setFormatForMenuTimes(PickerUtilities.createFormatterFromPatternString(
                dateFormat, timeSettings.getLocale()));
        timeSettings.setInitialTimeToNow();
        return dateTimePicker1;
    }

    /**
     * Creates new form SettingsPanel
     */
    public SettingsPanel() {
        initComponents();

        String dateFormat = "HH:mm:ss";
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        simpleTimeFormat = new SimpleDateFormat("HHmmss");

        dtFrom = newPicker(dateFormat);
        dtFrom.getTimePicker().getSettings().setDisplayToggleTimeMenuButton(true);

        TitledBorder border = (TitledBorder) jpProfileBase.getBorder();
        profileTitleBase = border.getTitle();

        border = (TitledBorder) jpAllProfiles.getBorder();
        profileAllTitleBase = border.getTitle();

        appTitleBase = ((TitledBorder) jpProfileBase.getBorder()).getTitle();

        lmProfile = new DefaultListModel<>();
        clbProfile = new CheckBoxList(lmProfile);
        jpProfile.add(new JScrollPane(clbProfile));
        clbProfile.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        lmApps = new DefaultListModel<>();
        clbApps = new CheckBoxList(lmApps);
        clbApps.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        clbApps.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    doPop(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    doPop(e);
                }
            }

            private void doPop(MouseEvent e) {
                cbmCopyHostName.setEnabled(clbApps.getSelectedValue() != null);
                jpmAppSettings.show(e.getComponent(), e.getX(), e.getY());
            }

        });

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
                AppProfile sel = (AppProfile) clbProfile.getSelectedValue();
                if (sel != null) {
                    sel.setNameSuffixes(newData);
                }
            }
        });

        ext.setAddChoices(new Utils.swing.ValuesEditor.IAddChoices() {
            @Override
            public HashSet<String> getAddChoices() {
                HashSet<String> ret = new HashSet<>();
                for (AppProfile appProfile : ds.getAppProfiles()) {
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

        tfGrepText.setMaximumSize(new Dimension(tfGrepText.getMaximumSize().width, tfGrepText.getMinimumSize().height));
        jtfOutputDir.setMaximumSize(new Dimension(jtfOutputDir.getMaximumSize().width, jtfOutputDir.getMinimumSize().height));
        jtfStatusScript.setMaximumSize(new Dimension(jtfStatusScript.getMaximumSize().width, jtfStatusScript.getMinimumSize().height));
        tfDateRegex.setMaximumSize(new Dimension(tfDateRegex.getMaximumSize().width, tfDateRegex.getMinimumSize().height));
        tfTimeRegex.setMaximumSize(new Dimension(tfTimeRegex.getMaximumSize().width, tfTimeRegex.getMinimumSize().height));
        jpCommandParams.setMaximumSize(new Dimension(jpCommandParams.getMaximumSize().width, jpCommandParams.getMinimumSize().height));

        jtfLogDirectory.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                jtfLogDirectoryTextChanged(e);
            }
        });

        jtfFileNameBase.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                jtfFileNameBaseTextChanged(e);
            }
        });

        JComponent comp = jsMaxThreads.getEditor();
        JFormattedTextField field = (JFormattedTextField) comp.getComponent(0);
        DefaultFormatter formatter = (DefaultFormatter) field.getFormatter();
        formatter.setCommitsOnValidEdit(true);
        jsMaxThreads.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                ds.setMaxThreads((int) jsMaxThreads.getValue());
            }
        });
    }

    private void jtfLogDirectoryTextChanged(FocusEvent e) {
        AppProfile prof = getActiveAppProfile();
        if (prof != null) {
            prof.setLogDirectory(((JTextField) e.getSource()).getText());
        }

    }

    private void jtfFileNameBaseTextChanged(FocusEvent e) {
        AppProfile prof = getActiveAppProfile();
        if (prof != null) {
            prof.setLogFileNameBase(((JTextField) e.getSource()).getText());
        }
    }

    private void tfFilenameSuffixesChanged() {
        AppProfile pr = (AppProfile) clbProfile.getSelectedValue();
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
                    Object elementAt = lmProfile.getElementAt(i);
                    if (elementAt instanceof AppProfile) {
                        ((AppProfile) elementAt)
                                .setSelected(lsm.isSelectedIndex(i));
                    }
                }
            }
            updateProfileTitle();
            updateStartButton();
//                }
//            });
        }
    }

    private boolean canRunProfiles() {
//(AppProfile) lmProfile.get(minIndex);

        CheckBoxListSelectionModel lsm = clbProfile.getCheckBoxListSelectionModel();
        DefaultListModel<Object> lm = (DefaultListModel<Object>) clbProfile.getModel();
        int allEntryIndex = lsm.getAllEntryIndex();
        for (int i = 0; i < lm.getSize(); i++) {
            if (lsm.isSelectedIndex(i) && i != allEntryIndex) {
                AppProfile elementAt = (AppProfile) lm.getElementAt(i);
                if (elementAt != null && hasCheckedApp(elementAt)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getSelectedNum(CheckBoxList clb) {
        int numSelected = 0;
//(AppProfile) lmProfile.get(minIndex);

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
            @Override
            public void run() {

                int numSelected = getSelectedNum(clbApps);
                int allSize = getAllSize(clbApps);

                TitledBorder border = (TitledBorder) jpAppsBase.getBorder();
                if (allSize <= 0) {
                    border.setTitle(appTitleBase);
                } else {
                    border.setTitle(appTitleBase + " (" + numSelected + "/" + (getAllSize(clbApps)) + ")");
                    updateTotalApps();
                }

                jpAppsBase.repaint();
                updateStartButton();
            }
        });

    }

    private void updateTotalApps() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((TitledBorder) jpAllProfiles.getBorder()).setTitle(profileAllTitleBase + " (apps selected " + getSelectedApp() + "/" + ds.getTotalApps() + ")");
                jpAllProfiles.repaint();
            }

        });

    }

    private void updateProfileTitle() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                ((TitledBorder) jpProfileBase.getBorder()).setTitle(profileTitleBase + " (" + getSelectedNum(clbProfile) + "/" + (getAllSize(clbProfile)) + ")");
                jpProfileBase.repaint();

            }

        });
        updateTotalApps();
    }

    private int getSelectedApp() {
        int numSelected = 0;
//(AppProfile) lmProfile.get(minIndex);

        CheckBoxListSelectionModel lsm = clbProfile.getCheckBoxListSelectionModel();
        DefaultListModel<Object> lm = (DefaultListModel<Object>) clbProfile.getModel();
        int allEntryIndex = lsm.getAllEntryIndex();
        for (int i = 0; i < lm.getSize(); i++) {
            if (lsm.isSelectedIndex(i) && i != allEntryIndex) {
                Object elementAt = lm.getElementAt(i);
                if (elementAt instanceof AppProfile) {
                    AppProfile pr = (AppProfile) elementAt;
                    List<App> apps = new ArrayList<>(pr.getApps());
                    if (apps != null) {
                        for (App app : apps) {
                            if (app.isChecked()) {
                                numSelected++;
                            }
                        }
                    }

                }

            }
        }

        return numSelected;
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
                @Override
                public void run() {
                    int minIndex = lsm.getMinSelectionIndex();
                    int maxIndex = lsm.getMaxSelectionIndex();
                    boolean singleSelection = (minIndex == maxIndex && minIndex >= 0);
                    int numSelected = (minIndex >= 0) ? (maxIndex - minIndex + 1) : 0;
                    if (singleSelection) { //one item selected
                        Object theProfile = lmProfile.get(minIndex);
                        profileSelectionChanged(((theProfile instanceof AppProfile)) ? (AppProfile) theProfile : null);
                        if (!(theProfile instanceof AppProfile)) {
                            numSelected = 0;
                        }
                    } else {
                        clbApps.setValueIsAdjusting(true);
                        DefaultListModel lm = (DefaultListModel) clbApps.getModel();
                        lm.removeAllElements();
                        clbApps.setValueIsAdjusting(false);
                        ext.noSelection();
                    }
                    profileSelected(numSelected);

                }

            });

        }
    }

    private void profileSelectionChanged(AppProfile pr) {

        clbApps.setValueIsAdjusting(true);
        CheckBoxListSelectionModel clbAppSelectionModel = clbApps.getCheckBoxListSelectionModel();
        ListSelectionListener[] listSelectionListeners = clbAppSelectionModel.getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            clbAppSelectionModel.removeListSelectionListener(listSelectionListener);
        }

//                        clbAppSelectionModel.addSelectionInterval(maxIndex, maxIndex);
        clbAppSelectionModel.clearSelection();
        lmApps.clear();
        if (pr != null) {
            List<App> apps = new ArrayList<>(pr.getApps());
            Collections.sort(apps);
            for (App app : apps) {
                lmApps.addElement(app);
            }
//        Collections.sort( lmApps);

            if (!lmApps.isEmpty()) {
                lmApps.insertElementAt(CheckBoxList.ALL_ENTRY, 0);
                for (int i = 1; i < lmApps.getSize(); i++) {
                    if (((App) lmApps.getElementAt(i)).isChecked()) {
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
            jtfLogDirectory.setText(pr.getLogDirectory());
            jtfFileNameBase.setText(pr.getLogFileNameBase());
            if (StringUtils.isNotEmpty(pr.getLoginProfile())) {
                cbLoginProfile.setSelectedItem(pr.getLoginProfile());
            } else {
                if (cbLoginProfile.getItemCount() > 0) {
                    cbLoginProfile.setSelectedIndex(0);
                    pr.setLoginProfile((String) cbLoginProfile.getSelectedItem());
                }
            }

//        tfFilenameSuffixes.setText(pr.getNameSuffixes());
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
                @Override
                public void run() {

                    appSelected(lsm);
                }

            });

        }
    }

    private void clbAppsCheckedChanged(ListSelectionEvent evt) {

        if (!evt.getValueIsAdjusting()) {
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
            int maxSelectionIndex = lsm.getMaxSelectionIndex();
            int minSelectionIndex = lsm.getMinSelectionIndex();
            logger.debug("-1-" + evt
                    + " f: " + evt.getFirstIndex()
                    + " l: " + evt.getLastIndex()
                    + " min: " + minSelectionIndex + " max: " + maxSelectionIndex
            );
            for (int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++) {
                if (!lmApps.isEmpty() && i < lmApps.getSize()) {
                    if (lmApps.getElementAt(i) instanceof App) {
                        logger.debug("lmApps: " + lmApps);
                        ((App) lmApps.getElementAt(i))
                                .setChecked(lsm.isSelectedIndex(i));
                    }
                }

            }
//                }
//            });
            updateAppTitle();
        }

    }

    private void appSelected(ListSelectionModel lsm) {
        jbAppDelete.setEnabled(!lsm.isSelectionEmpty());
        jpProfileProperties.setEnabled(!lsm.isSelectionEmpty());

        this.appIdx = (!lsm.isSelectionEmpty() && lsm.getMaxSelectionIndex() == lsm.getMinSelectionIndex()
                && lmApps.getElementAt(lsm.getMinSelectionIndex()) != CheckBoxList.ALL_ENTRY)
                ? lsm.getMinSelectionIndex()
                : -1;
        jlbAppFileNameBase.setEnabled(appIdx >= 0);
        jlbAppLogDirectory.setEnabled(appIdx >= 0);
        jtfAppFileNameBase.setEnabled(appIdx >= 0);
        jrbOSLinux.setEnabled(appIdx >= 0);
        jrbOSWindows.setEnabled(appIdx >= 0);
        jtfAppFileNameBase.setText((appIdx >= 0)
                ? ((App) lmApps.getElementAt(appIdx)).getAppPrefix()
                : "");
        jtfAppLogDirectory.setEnabled(appIdx >= 0);
        jtfAppLogDirectory.setText((appIdx >= 0)
                ? ((App) lmApps.getElementAt(appIdx)).getAppDir()
                : "");
        if (appIdx >= 0) {
            if (((App) lmApps.getElementAt(appIdx)).isIsWindows()) {
                jrbOSWindows.setSelected(true);
            } else {
                jrbOSLinux.setSelected(true);
            }
        }
//        rbCloudLogs.setEnabled(singleSelection);
//        rbGenesysLogs.setEnabled(singleSelection);
//        cbLFMTs.setEnabled(singleSelection);
//        btEditLFMTs.setEnabled(singleSelection);
    }

    private AppProfile getActiveAppProfile() {

        Object selectedValue = clbProfile.getSelectedValue();
        if (selectedValue != null && selectedValue instanceof AppProfile) {
            return (AppProfile) selectedValue;
        }
        return null;
    }

    private void profileSelected(int numSelected) {
        jpAppsBase.setEnabled(numSelected == 1);
        jpProfileProperties.setEnabled(numSelected == 1);
        jbProfileDelete.setEnabled(numSelected > 0);
        jbProfileRename.setEnabled(numSelected == 1);
        jbProfileSaveAs.setEnabled(numSelected == 1);
        jbAppAdd.setEnabled(numSelected == 1);
        rbCloudLogs.setEnabled(numSelected == 1);
        rbGenesysLogs.setEnabled(numSelected == 1);
        ext.setEnabled(numSelected == 1);
        jlbFileNameBase.setEnabled(numSelected == 1);
        jtfFileNameBase.setEnabled(numSelected == 1);
        jlbLogDirectory.setEnabled(numSelected == 1);
        jtfLogDirectory.setEnabled(numSelected == 1);
        cbLoginProfile.setEnabled(numSelected == 1);

        appSelected(clbApps.getSelectionModel());

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

    SettingsPanel(DownloadSettings ds, SettingsForm dlg) {
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
        jpRegEx = new javax.swing.JPanel();
        jPanel25 = new javax.swing.JPanel();
        lbDateRegex = new javax.swing.JLabel();
        tfDateRegex = new javax.swing.JTextField();
        jPanel26 = new javax.swing.JPanel();
        lbTimeRegex = new javax.swing.JLabel();
        tfTimeRegex = new javax.swing.JTextField();
        jpFindAny = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        tfFindAnyDirectory = new javax.swing.JTextField();
        jPanel39 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        tfFindAnyRegex = new javax.swing.JTextField();
        jpmAppSettings = new javax.swing.JPopupMenu();
        cbmShowHosts = new javax.swing.JCheckBoxMenuItem();
        cbmCopyHostName = new javax.swing.JMenuItem();
        bgOS = new javax.swing.ButtonGroup();
        bgSelectionType = new javax.swing.ButtonGroup();
        jpAllProfiles = new javax.swing.JPanel();
        jpProfileBase = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jpProfile = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jbProfileAdd = new javax.swing.JButton();
        jbProfileDelete = new javax.swing.JButton();
        jbProfileRename = new javax.swing.JButton();
        jbProfileSaveAs = new javax.swing.JButton();
        jPanel15 = new javax.swing.JPanel();
        jpProfileProperties = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        rbGenesysLogs = new javax.swing.JRadioButton();
        rbCloudLogs = new javax.swing.JRadioButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jlbLogDirectory = new javax.swing.JLabel();
        jtfLogDirectory = new javax.swing.JTextField();
        jPanel12 = new javax.swing.JPanel();
        jlbFileNameBase = new javax.swing.JLabel();
        jtfFileNameBase = new javax.swing.JTextField();
        jPanel31 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cbLoginProfile = new javax.swing.JComboBox<>();
        btEditLoginProfile = new javax.swing.JButton();
        pExtensions = new javax.swing.JPanel();
        jpAppsBase = new javax.swing.JPanel();
        jPanel19 = new javax.swing.JPanel();
        jpApps = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jbAppAdd = new javax.swing.JButton();
        jbAppDelete = new javax.swing.JButton();
        jPanel14 = new javax.swing.JPanel();
        jpAppProperties = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        jlbAppLogDirectory = new javax.swing.JLabel();
        jtfAppLogDirectory = new javax.swing.JTextField();
        jPanel29 = new javax.swing.JPanel();
        jlbAppFileNameBase = new javax.swing.JLabel();
        jtfAppFileNameBase = new javax.swing.JTextField();
        jPanel36 = new javax.swing.JPanel();
        jlbAppLogDirectory1 = new javax.swing.JLabel();
        tfAnsibleBecomeUser = new javax.swing.JTextField();
        jPanel37 = new javax.swing.JPanel();
        jlbAppLogDirectory2 = new javax.swing.JLabel();
        tfDefaultRX = new javax.swing.JTextField();
        jPanel30 = new javax.swing.JPanel();
        jrbOSLinux = new javax.swing.JRadioButton();
        jrbOSWindows = new javax.swing.JRadioButton();
        jPanel34 = new javax.swing.JPanel();
        jPanel33 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        cbProdLog = new javax.swing.JCheckBox();
        cbLCALogs = new javax.swing.JCheckBox();
        cbAppLogs = new javax.swing.JCheckBox();
        cbZipDest = new javax.swing.JCheckBox();
        cbParseWhileDownload = new javax.swing.JCheckBox();
        jpRangeSelect = new javax.swing.JPanel();
        jPanel38 = new javax.swing.JPanel();
        jlbAppLogDirectory3 = new javax.swing.JLabel();
        spMaxFiles = new javax.swing.JSpinner();
        jPanel11 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        rbDefaultMask = new javax.swing.JRadioButton();
        rbDateTime = new javax.swing.JRadioButton();
        rbSearchAny = new javax.swing.JRadioButton();
        jpRangeParams = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jpCommandParams = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jPanel23 = new javax.swing.JPanel();
        lCommand = new javax.swing.JLabel();
        cbCommand = new javax.swing.JComboBox();
        jPanel32 = new javax.swing.JPanel();
        jlbLogDirectory1 = new javax.swing.JLabel();
        jsMaxThreads = new javax.swing.JSpinner();
        jPanel18 = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        jPanel22 = new javax.swing.JPanel();
        cbUseRSync = new javax.swing.JCheckBox();
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
        jpStatusScript = new javax.swing.JPanel();
        jPanel27 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jtfStatusScript = new javax.swing.JTextField();
        jbSelectScript = new javax.swing.JButton();

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

        jpFindAny.setLayout(new javax.swing.BoxLayout(jpFindAny, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel13.setLayout(new javax.swing.BoxLayout(jPanel13, javax.swing.BoxLayout.LINE_AXIS));

        jLabel3.setText("Search start directory");
        jPanel13.add(jLabel3);

        tfFindAnyDirectory.setMaximumSize(new java.awt.Dimension(2147483647, 22));
        jPanel13.add(tfFindAnyDirectory);

        jpFindAny.add(jPanel13);

        jPanel39.setLayout(new javax.swing.BoxLayout(jPanel39, javax.swing.BoxLayout.LINE_AXIS));

        jLabel5.setText("file regex for full name");
        jPanel39.add(jLabel5);

        tfFindAnyRegex.setMaximumSize(new java.awt.Dimension(2147483647, 22));
        jPanel39.add(tfFindAnyRegex);

        jpFindAny.add(jPanel39);

        cbmShowHosts.setText("Show host");
        cbmShowHosts.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbmShowHostsItemStateChanged(evt);
            }
        });
        jpmAppSettings.add(cbmShowHosts);

        cbmCopyHostName.setText("Copy host name");
        cbmCopyHostName.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbmCopyHostNameItemStateChanged(evt);
            }
        });
        cbmCopyHostName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbmCopyHostNameActionPerformed(evt);
            }
        });
        jpmAppSettings.add(cbmCopyHostName);

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));

        jpAllProfiles.setBorder(javax.swing.BorderFactory.createTitledBorder("Application profiles"));
        jpAllProfiles.setLayout(new javax.swing.BoxLayout(jpAllProfiles, javax.swing.BoxLayout.LINE_AXIS));

        jpProfileBase.setBorder(javax.swing.BorderFactory.createTitledBorder("Profile"));
        jpProfileBase.setLayout(new javax.swing.BoxLayout(jpProfileBase, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel17.setLayout(new javax.swing.BoxLayout(jPanel17, javax.swing.BoxLayout.LINE_AXIS));

        jpProfile.setLayout(new java.awt.BorderLayout());
        jPanel17.add(jpProfile);

        jPanel8.setLayout(new java.awt.GridLayout(0, 1));

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

        jPanel15.setLayout(new java.awt.BorderLayout());
        jPanel8.add(jPanel15);

        jPanel17.add(jPanel8);

        jpProfileBase.add(jPanel17);

        jpProfileProperties.setLayout(new javax.swing.BoxLayout(jpProfileProperties, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.LINE_AXIS));

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
        jPanel5.add(rbGenesysLogs);

        bgFileNaming.add(rbCloudLogs);
        rbCloudLogs.setText("Cloud file name");
        jPanel5.add(rbCloudLogs);

        jPanel1.add(jPanel5);

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.LINE_AXIS));

        jlbLogDirectory.setText("Log files directory");
        jPanel4.add(jlbLogDirectory);
        jPanel4.add(jtfLogDirectory);

        jPanel3.add(jPanel4);

        jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12, javax.swing.BoxLayout.LINE_AXIS));

        jlbFileNameBase.setText("Log files name base");
        jPanel12.add(jlbFileNameBase);
        jPanel12.add(jtfFileNameBase);

        jPanel3.add(jPanel12);

        jPanel1.add(jPanel3);

        jpProfileProperties.add(jPanel1);

        jPanel31.setLayout(new javax.swing.BoxLayout(jPanel31, javax.swing.BoxLayout.LINE_AXIS));

        jLabel1.setText("Login profile");
        jPanel31.add(jLabel1);

        cbLoginProfile.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbLoginProfileItemStateChanged(evt);
            }
        });
        jPanel31.add(cbLoginProfile);

        btEditLoginProfile.setText("...");
        btEditLoginProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btEditLoginProfileActionPerformed(evt);
            }
        });
        jPanel31.add(btEditLoginProfile);

        jpProfileProperties.add(jPanel31);

        pExtensions.setBorder(javax.swing.BorderFactory.createTitledBorder("Name suffixes (.=empty)"));
        pExtensions.setLayout(new java.awt.BorderLayout());
        jpProfileProperties.add(pExtensions);

        jpProfileBase.add(jpProfileProperties);

        jpAllProfiles.add(jpProfileBase);

        jpAppsBase.setBorder(javax.swing.BorderFactory.createTitledBorder("Apps"));
        jpAppsBase.setMinimumSize(new java.awt.Dimension(250, 53));
        jpAppsBase.setLayout(new javax.swing.BoxLayout(jpAppsBase, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel19.setLayout(new javax.swing.BoxLayout(jPanel19, javax.swing.BoxLayout.LINE_AXIS));

        jpApps.setMinimumSize(new java.awt.Dimension(250, 0));
        jpApps.setLayout(new java.awt.BorderLayout());
        jPanel19.add(jpApps);

        jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.PAGE_AXIS));

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

        jPanel14.setLayout(new javax.swing.BoxLayout(jPanel14, javax.swing.BoxLayout.LINE_AXIS));
        jPanel10.add(jPanel14);

        jPanel19.add(jPanel10);

        jpAppsBase.add(jPanel19);

        jpAppProperties.setLayout(new javax.swing.BoxLayout(jpAppProperties, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel28.setLayout(new java.awt.GridLayout(1, 0));

        jlbAppLogDirectory.setText("Log files directory");
        jPanel28.add(jlbAppLogDirectory);

        jtfAppLogDirectory.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtfAppLogDirectoryFocusLost(evt);
            }
        });
        jPanel28.add(jtfAppLogDirectory);

        jpAppProperties.add(jPanel28);

        jPanel29.setLayout(new java.awt.GridLayout(1, 0));

        jlbAppFileNameBase.setText("Log files name base");
        jPanel29.add(jlbAppFileNameBase);

        jtfAppFileNameBase.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtfAppFileNameBaseFocusLost(evt);
            }
        });
        jPanel29.add(jtfAppFileNameBase);

        jpAppProperties.add(jPanel29);

        jPanel36.setLayout(new java.awt.GridLayout(1, 0));

        jlbAppLogDirectory1.setText("ansible become user");
        jPanel36.add(jlbAppLogDirectory1);

        tfAnsibleBecomeUser.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                tfAnsibleBecomeUserFocusLost(evt);
            }
        });
        jPanel36.add(tfAnsibleBecomeUser);

        jpAppProperties.add(jPanel36);

        jPanel37.setLayout(new java.awt.GridLayout(1, 0));

        jlbAppLogDirectory2.setText("default rx");
        jPanel37.add(jlbAppLogDirectory2);

        tfDefaultRX.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                tfDefaultRXFocusLost(evt);
            }
        });
        jPanel37.add(tfDefaultRX);

        jpAppProperties.add(jPanel37);

        jPanel30.setLayout(new javax.swing.BoxLayout(jPanel30, javax.swing.BoxLayout.LINE_AXIS));

        bgOS.add(jrbOSLinux);
        jrbOSLinux.setSelected(true);
        jrbOSLinux.setText("Linux");
        jrbOSLinux.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jrbOSLinuxItemStateChanged(evt);
            }
        });
        jPanel30.add(jrbOSLinux);

        bgOS.add(jrbOSWindows);
        jrbOSWindows.setText("Windows");
        jrbOSWindows.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jrbOSWindowsItemStateChanged(evt);
            }
        });
        jPanel30.add(jrbOSWindows);

        jpAppProperties.add(jPanel30);

        jpAppsBase.add(jpAppProperties);

        jPanel34.setLayout(new java.awt.BorderLayout());
        jpAppsBase.add(jPanel34);

        jpAllProfiles.add(jpAppsBase);

        jPanel33.setLayout(new java.awt.BorderLayout());
        jpAllProfiles.add(jPanel33);

        add(jpAllProfiles);

        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        jPanel7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel7.setLayout(new java.awt.GridLayout(0, 2));

        cbProdLog.setText("logs from prod");
        jPanel7.add(cbProdLog);

        cbLCALogs.setText("LCA logs");
        cbLCALogs.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbLCALogsItemStateChanged(evt);
            }
        });
        jPanel7.add(cbLCALogs);

        cbAppLogs.setText("Application logs");
        cbAppLogs.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbAppLogsItemStateChanged(evt);
            }
        });
        cbAppLogs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbAppLogsActionPerformed(evt);
            }
        });
        jPanel7.add(cbAppLogs);

        cbZipDest.setText("Zip destination");
        cbZipDest.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbZipDestItemStateChanged(evt);
            }
        });
        jPanel7.add(cbZipDest);

        cbParseWhileDownload.setText("Parse while download");
        jPanel7.add(cbParseWhileDownload);

        jPanel2.add(jPanel7);

        jpRangeSelect.setBorder(javax.swing.BorderFactory.createTitledBorder("Files selection"));
        jpRangeSelect.setLayout(new javax.swing.BoxLayout(jpRangeSelect, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel38.setLayout(new java.awt.GridLayout(1, 0));

        jlbAppLogDirectory3.setText("max files");
        jPanel38.add(jlbAppLogDirectory3);
        jPanel38.add(spMaxFiles);

        jpRangeSelect.add(jPanel38);

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        jPanel11.setLayout(new javax.swing.BoxLayout(jPanel11, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        bgSelectionType.add(rbDefaultMask);
        rbDefaultMask.setSelected(true);
        rbDefaultMask.setText("Use default file masks");
        rbDefaultMask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbDefaultMaskActionPerformed(evt);
            }
        });
        jPanel6.add(rbDefaultMask);

        bgSelectionType.add(rbDateTime);
        rbDateTime.setText("Date/Time shell regex");
        rbDateTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbDateTimeActionPerformed(evt);
            }
        });
        jPanel6.add(rbDateTime);

        bgSelectionType.add(rbSearchAny);
        rbSearchAny.setText("Find any");
        rbSearchAny.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbSearchAnyActionPerformed(evt);
            }
        });
        jPanel6.add(rbSearchAny);

        jPanel11.add(jPanel6);

        jpRangeParams.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jpRangeParams.setMinimumSize(new java.awt.Dimension(6, 50));
        jpRangeParams.setPreferredSize(new java.awt.Dimension(6, 50));
        jpRangeParams.setLayout(new java.awt.BorderLayout());
        jPanel11.add(jpRangeParams);

        jpRangeSelect.add(jPanel11);

        jPanel2.add(jpRangeSelect);

        add(jPanel2);

        jPanel9.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.PAGE_AXIS));

        jpCommandParams.setLayout(new javax.swing.BoxLayout(jpCommandParams, javax.swing.BoxLayout.LINE_AXIS));

        jPanel16.setLayout(new javax.swing.BoxLayout(jPanel16, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel23.setLayout(new javax.swing.BoxLayout(jPanel23, javax.swing.BoxLayout.LINE_AXIS));

        lCommand.setText("Command");
        jPanel23.add(lCommand);

        cbCommand.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbCommandActionPerformed(evt);
            }
        });
        jPanel23.add(cbCommand);

        jPanel16.add(jPanel23);

        jPanel32.setLayout(new javax.swing.BoxLayout(jPanel32, javax.swing.BoxLayout.LINE_AXIS));

        jlbLogDirectory1.setText("Max threads");
        jPanel32.add(jlbLogDirectory1);
        jPanel32.add(jsMaxThreads);

        jPanel16.add(jPanel32);

        jpCommandParams.add(jPanel16);

        jPanel18.setLayout(new javax.swing.BoxLayout(jPanel18, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel20.setLayout(new javax.swing.BoxLayout(jPanel20, javax.swing.BoxLayout.LINE_AXIS));

        jPanel22.setLayout(new java.awt.BorderLayout());

        cbUseRSync.setText("Use RSync");
        jPanel22.add(cbUseRSync, java.awt.BorderLayout.CENTER);

        jPanel20.add(jPanel22);

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

        jpStatusScript.setLayout(new javax.swing.BoxLayout(jpStatusScript, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel27.setLayout(new javax.swing.BoxLayout(jPanel27, javax.swing.BoxLayout.LINE_AXIS));

        jLabel4.setText("App status script");
        jPanel27.add(jLabel4);
        jPanel27.add(jtfStatusScript);

        jbSelectScript.setText("...");
        jbSelectScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSelectScriptActionPerformed(evt);
            }
        });
        jPanel27.add(jbSelectScript);

        jpStatusScript.add(jPanel27);

        jPanel9.add(jpStatusScript);

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
        List<AppProfile> selectedValuesList = clbProfile.getSelectedValuesList();
        if (selectedValuesList != null && !selectedValuesList.isEmpty()) {
            StringBuilder sPrompt = new StringBuilder();
            sPrompt.append("Do you really want to delete ");
            if (selectedValuesList.size() == 1) {
                sPrompt.append("profile [").append(selectedValuesList.get(0).getName()).append("]");
            } else {
                sPrompt.append(selectedValuesList.size()).append(" profiles ");
            }

            if (JOptionPane.showConfirmDialog((Window) this.getRootPane().getParent(),
                    sPrompt.toString(), "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                for (AppProfile appProfile : selectedValuesList) {
                    ds.removeProfile(appProfile);
                }
                loadProfile(null);
            }
        }

    }//GEN-LAST:event_jbProfileDeleteActionPerformed

    private void jbProfileRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileRenameActionPerformed
        AppProfile appPr = (AppProfile) clbProfile.getSelectedValue();
        if (appPr != null) {
            String name = getProfileName("Edit the profile name", appPr.getName());
            if (name != null) {
                appPr.setName(name);
                clbProfile.updateUI();
            }
        }
    }//GEN-LAST:event_jbProfileRenameActionPerformed

    private void jbProfileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbProfileSaveAsActionPerformed

        Object selectedValue = clbProfile.getSelectedValue();
        if (selectedValue instanceof AppProfile) {
            AppProfile appPr = (AppProfile) selectedValue;
            String name = getProfileName("Enter new profile name", appPr.getName());
            if (name != null) {
                addProfile(name, appPr);
            }
        }
    }//GEN-LAST:event_jbProfileSaveAsActionPerformed

    private void rbGenesysLogsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_rbGenesysLogsItemStateChanged
        AppProfile prof = getActiveAppProfile();
        if (prof != null) {
            prof.setIsGenesysName(evt.getStateChange() == ItemEvent.SELECTED);
        }
    }//GEN-LAST:event_rbGenesysLogsItemStateChanged

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
            p.showButtonAll(false);
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
            Object selectedValue = clbProfile.getSelectedValue();
            if (selectedValue instanceof AppProfile) {
                AppProfile profile = (AppProfile) selectedValue;
                for (int selectedRow : selectedRows) {
                    String app = (String) infoTableModel.getValueAt(selectedRow, 0);

                    App addApp = profile.addApp(app, GetLogs.getHosts().getAppDir(app));
//                lmApps.addElement(addApp);
//                selValues.add((String) infoTableModel.getValueAt(selectedRow, 0));
                }
                profileSelectionChanged(profile);
            } else {
                profileSelectionChanged(null);
            }
//            profileSelected(true);
        }

    }//GEN-LAST:event_jbAppAddActionPerformed

    private void jbAppDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAppDeleteActionPerformed
        AppProfile appPr = (AppProfile) clbProfile.getSelectedValue();
        List<App> selectedValuesList = clbApps.getSelectedValuesList();
        if (appPr != null && selectedValuesList != null) {
            StringBuilder sPrompt = new StringBuilder();
            sPrompt.append("Do you really want to delete ");
            if (selectedValuesList.size() == 1) {
                sPrompt.append("app [").append(selectedValuesList.get(0).getName()).append("]");
            } else {
                sPrompt.append(selectedValuesList.size()).append(" applications ");
            }
            sPrompt.append(" from profile [")
                    .append(appPr.getName())
                    .append("]");
            if (JOptionPane.showConfirmDialog(this, sPrompt, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                for (App app : selectedValuesList) {
                    appPr.removeApp(app);
                    lmApps.removeElement(app);
                    if (lmApps.size() == 1 && (lmApps.getElementAt(0).equals(CheckBoxList.ALL_ENTRY))) {
                        lmApps.remove(0);
                    }

                }
            }
        }
    }//GEN-LAST:event_jbAppDeleteActionPerformed

    private void cbCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbCommandActionPerformed
        cbCommandSelectionChanged((GetCommand) ((JComboBox) evt.getSource()).getSelectedItem());
    }//GEN-LAST:event_cbCommandActionPerformed

    private void cbAppLogsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbAppLogsItemStateChanged
        updateStartButton();

    }//GEN-LAST:event_cbAppLogsItemStateChanged

    private void cbLCALogsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbLCALogsItemStateChanged
        updateStartButton();
    }//GEN-LAST:event_cbLCALogsItemStateChanged

    private void cbmShowHostsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbmShowHostsItemStateChanged
        JCheckBoxMenuItem source = (JCheckBoxMenuItem) evt.getSource();
        GetLogs.setHostsVisible(source.isSelected());
        Object selectedValue = clbProfile.getSelectedValue();
        if (selectedValue instanceof AppProfile) {
            AppProfile prof = (AppProfile) selectedValue;
            profileSelectionChanged(prof);
        } else {
            profileSelectionChanged(null);
        }
    }//GEN-LAST:event_cbmShowHostsItemStateChanged

    private void cbmCopyHostNameItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbmCopyHostNameItemStateChanged
    }//GEN-LAST:event_cbmCopyHostNameItemStateChanged

    private void cbmCopyHostNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbmCopyHostNameActionPerformed
        App selectedApp = (App) clbApps.getSelectedValue();
        if (selectedApp != null) {
            Utils.SystemClipboard.copy(selectedApp.getHost());
        }
    }//GEN-LAST:event_cbmCopyHostNameActionPerformed

    private void jbSelectScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSelectScriptActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_jbSelectScriptActionPerformed

    private void jtfAppLogDirectoryFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtfAppLogDirectoryFocusLost
        // TODO add your handling code here:
        JTextField fld = (JTextField) evt.getSource();
        if (fld.isEnabled() && appIdx >= 0) {//single app selected
            ((App) clbApps.getModel().getElementAt(appIdx)).setAppDir(
                    fld.getText()
            );
        }
    }//GEN-LAST:event_jtfAppLogDirectoryFocusLost

    private void jtfAppFileNameBaseFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtfAppFileNameBaseFocusLost
        JTextField fld = (JTextField) evt.getSource();
        if (fld.isEnabled() && appIdx >= 0) {//single app selected
            ((App) clbApps.getModel().getElementAt(appIdx)).setAppPrefix(
                    fld.getText()
            );
        }
    }//GEN-LAST:event_jtfAppFileNameBaseFocusLost

    private void jrbOSLinuxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jrbOSLinuxItemStateChanged

    }//GEN-LAST:event_jrbOSLinuxItemStateChanged

    private void jrbOSWindowsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jrbOSWindowsItemStateChanged
        if (jrbOSWindows.isEnabled() && appIdx >= 0) {//single app selected
            ((App) clbApps.getModel().getElementAt(appIdx)).setIsWindows(evt.getStateChange() == ItemEvent.SELECTED);
        }
    }//GEN-LAST:event_jrbOSWindowsItemStateChanged

    private void btEditLoginProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btEditLoginProfileActionPerformed
        editLoginProfiles();
    }//GEN-LAST:event_btEditLoginProfileActionPerformed

    private void cbLoginProfileItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbLoginProfileItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            Object item = evt.getItem();
            AppProfile prof = (AppProfile) clbProfile.getSelectedValue();
            if (prof != null) {
                prof.setLoginProfile(item.toString());
            }
        }
    }//GEN-LAST:event_cbLoginProfileItemStateChanged

    private void cbZipDestItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbZipDestItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_cbZipDestItemStateChanged

    private void cbAppLogsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbAppLogsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cbAppLogsActionPerformed

    private void tfAnsibleBecomeUserFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfAnsibleBecomeUserFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_tfAnsibleBecomeUserFocusLost

    private void tfDefaultRXFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfDefaultRXFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_tfDefaultRXFocusLost

    private void rbDefaultMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbDefaultMaskActionPerformed
        jpRangeParams.removeAll();
        jpRangeParams.repaint();
        jpRangeParams.revalidate();
        setRxType(regexType.Default);

    }//GEN-LAST:event_rbDefaultMaskActionPerformed

    private void rbDateTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbDateTimeActionPerformed
        jpRangeParams.removeAll();
        jpRangeParams.add(jpRegEx);
        jpRangeParams.repaint();
        jpRangeParams.revalidate();
        setRxType(regexType.ShellRegex);
    }//GEN-LAST:event_rbDateTimeActionPerformed

    private void rbSearchAnyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbSearchAnyActionPerformed
        jpRangeParams.removeAll();
        jpRangeParams.add(jpFindAny);
        jpRangeParams.repaint();
        jpRangeParams.revalidate();
        setRxType(regexType.Any);

    }//GEN-LAST:event_rbSearchAnyActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgFileNaming;
    private javax.swing.ButtonGroup bgOS;
    private javax.swing.ButtonGroup bgSelectionType;
    private javax.swing.JButton btEditLoginProfile;
    private javax.swing.JCheckBox cbAppLogs;
    private javax.swing.JComboBox cbCommand;
    private javax.swing.JCheckBox cbLCALogs;
    private javax.swing.JComboBox<String> cbLoginProfile;
    private javax.swing.JCheckBox cbParseWhileDownload;
    private javax.swing.JCheckBox cbProdLog;
    private javax.swing.JCheckBox cbUseRSync;
    private javax.swing.JCheckBox cbZipDest;
    private javax.swing.JMenuItem cbmCopyHostName;
    private javax.swing.JCheckBoxMenuItem cbmShowHosts;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
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
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel36;
    private javax.swing.JPanel jPanel37;
    private javax.swing.JPanel jPanel38;
    private javax.swing.JPanel jPanel39;
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
    private javax.swing.JButton jbSelectScript;
    private javax.swing.JLabel jlbAppFileNameBase;
    private javax.swing.JLabel jlbAppLogDirectory;
    private javax.swing.JLabel jlbAppLogDirectory1;
    private javax.swing.JLabel jlbAppLogDirectory2;
    private javax.swing.JLabel jlbAppLogDirectory3;
    private javax.swing.JLabel jlbFileNameBase;
    private javax.swing.JLabel jlbLogDirectory;
    private javax.swing.JLabel jlbLogDirectory1;
    private javax.swing.JPanel jpAllProfiles;
    private javax.swing.JPanel jpAppProperties;
    private javax.swing.JPanel jpApps;
    private javax.swing.JPanel jpAppsBase;
    private javax.swing.JPanel jpCommandParams;
    private javax.swing.JPanel jpDownloadParams;
    private javax.swing.JPanel jpFindAny;
    private javax.swing.JPanel jpProfile;
    private javax.swing.JPanel jpProfileBase;
    private javax.swing.JPanel jpProfileProperties;
    private javax.swing.JPanel jpRangeParams;
    private javax.swing.JPanel jpRangeSelect;
    private javax.swing.JPanel jpRegEx;
    private javax.swing.JPanel jpStatusScript;
    private javax.swing.JPopupMenu jpmAppSettings;
    private javax.swing.JRadioButton jrbOSLinux;
    private javax.swing.JRadioButton jrbOSWindows;
    private javax.swing.JSpinner jsMaxThreads;
    private javax.swing.JTextField jtfAppFileNameBase;
    private javax.swing.JTextField jtfAppLogDirectory;
    private javax.swing.JTextField jtfFileNameBase;
    private javax.swing.JTextField jtfLogDirectory;
    private javax.swing.JTextField jtfOutputDir;
    private javax.swing.JTextField jtfStatusScript;
    private javax.swing.JLabel lCommand;
    private javax.swing.JLabel lGrepText;
    private javax.swing.JLabel lbDateRegex;
    private javax.swing.JLabel lbTimeRegex;
    private javax.swing.JPanel pAfterActions;
    private javax.swing.JPanel pBeforeActions;
    private javax.swing.JPanel pExtensions;
    private javax.swing.JRadioButton rbCloudLogs;
    private javax.swing.JRadioButton rbDateTime;
    private javax.swing.JRadioButton rbDefaultMask;
    private javax.swing.JRadioButton rbGenesysLogs;
    private javax.swing.JRadioButton rbSearchAny;
    private javax.swing.JSpinner spMaxFiles;
    private javax.swing.JTextField tfAnsibleBecomeUser;
    private javax.swing.JTextField tfDateRegex;
    private javax.swing.JTextField tfDefaultRX;
    private javax.swing.JTextField tfFindAnyDirectory;
    private javax.swing.JTextField tfFindAnyRegex;
    private javax.swing.JTextField tfGrepText;
    private javax.swing.JTextField tfTimeRegex;
    // End of variables declaration//GEN-END:variables

    private void loadProfile(AppProfile activeProfile) {
        CheckBoxListSelectionModel checkBoxListSelectionModel = clbProfile.getCheckBoxListSelectionModel();
        ListSelectionListener[] listSelectionListeners = checkBoxListSelectionModel.getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            checkBoxListSelectionModel.removeListSelectionListener(listSelectionListener);
        }
        lmProfile.clear();
        int selIdx = -1;
        ArrayList<AppProfile> appProfilesSorted = ds.getAppProfilesSorted();
        if (appProfilesSorted.size() > 0) {
            lmProfile.insertElementAt(CheckBoxList.ALL_ENTRY, 0);

            for (AppProfile appProfile : appProfilesSorted) {
                lmProfile.addElement(appProfile);
                int idx = lmProfile.size() - 1;
                if (activeProfile == appProfile) {
                    selIdx = idx;
                }
                if (appProfile.isSelected()) {
                    checkBoxListSelectionModel.addSelectionInterval(idx, idx);
                }
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
        Dimension minSize = jpProfileBase.getPreferredSize();
        Dimension maximumSize = jpProfileBase.getMaximumSize();
        Dimension maxSize = new Dimension(minSize.width, maximumSize.height);
        jpProfileBase.setMaximumSize(maxSize);
        profileSelected(selectedIndices.length);

    }

    private void loadConfig() {
        loadProfile(null);

//        cbListFiles.setSelected(ds.isListFiles());
        cbProdLog.setSelected(ds.isProd());
        cbAppLogs.setSelected(ds.isAppLogs());
        cbZipDest.setSelected(ds.isZipDest());
        cbParseWhileDownload.setSelected(ds.isParserWhileDownload());
        cbLCALogs.setSelected(ds.isLcaLogs());
        spMaxFiles.setValue(ds.getMaxFiles());
        jtfOutputDir.setText(ds.getOutputDir());
        cbUseRSync.setSelected(ds.isUseRSync());
        afterActions.setData(ds.getAfterActions(), true);
        beforeActions.setData(ds.getBeforeActions(), true);
        jtfStatusScript.setText(ds.getStatusScript());
//        afterActions.setMaximumSize(new Dimension(afterActions.getMaximumSize().width, afterActions.getHeight()));
//        pAfterActions.setMaximumSize(new Dimension(pAfterActions.getMaximumSize().width, afterActions.getHeight()));

        tfDateRegex.setText(simpleDateFormat.format(Calendar.getInstance().getTime()));
        tfTimeRegex.setText(simpleTimeFormat.format(Calendar.getInstance().getTime()));
        GetCommand actionCommand = ds.getActionCommand();
        if (actionCommand == null || actionCommand == GetCommand.Unknown) {
            actionCommand = GetCommand.LS;
        }
        RegexType rxType = ds.getRxType();
        if (rxType == null) {
            rxType = RegexType.Default;
        }
        switch (rxType) {
            case Any:
                rbSearchAny.setSelected(true);
                break;

            case Default:
                rbDefaultMask.setSelected(true);
                break;

            case ShellRegex:
                rbDateTime.setSelected(true);
                break;
        }
        setRxType(rxType);
        initCB(cbCommand, actionCommand, GetCommand.values(), new Object[]{GetCommand.Unknown});
//        cbCommand.addItemListener(aListener);
        tfGrepText.setText(ds.getGrepText());
        reloadLoginProfiles();
        updateProfileTitle();
        jsMaxThreads.setValue(ds.getMaxThreads());
        tfFindAnyDirectory.setText(ds.getFindAnyDir());
        tfFindAnyRegex.setText(ds.getFindAnyRx());
    }

    private void addProfile(String showInputDialog) {
//        AppProfile addProfile = ds.addProfile(showInputDialog); //To change body of generated methods, choose Tools | Templates.
//        lmProfile.addElement(addProfile);
        AppProfile addProfile = ds.addProfile(showInputDialog);
        loadProfile(addProfile);
    }

    private void addProfile(String showInputDialog, AppProfile appPr) {
//        AppProfile addProfile = ds.addProfile(showInputDialog, appPr); //To change body of generated methods, choose Tools | Templates.
//        lmProfile.addElement(addProfile);
        AppProfile addProfile = ds.addProfile(showInputDialog, appPr);
        loadProfile(addProfile);
    }

    public void saveConfig() {
        ds.setMaxFiles(((Integer) spMaxFiles.getValue()).intValue());
        ds.setUseRSync(cbUseRSync.isSelected());
//        ds.setListFiles(cbListFiles.isSelected());
        ds.setGrepText(tfGrepText.getText());
        ds.setOutputDir(jtfOutputDir.getText());
        ds.setProd(cbProdLog.isSelected());
        ds.setLcaLogs(cbLCALogs.isSelected());
        ds.setAppLogs(cbAppLogs.isSelected());
        ds.setZipDest(cbZipDest.isSelected());
        ds.setParserWhileDownload(cbParseWhileDownload.isSelected());
        ds.setActionCommand((GetCommand) cbCommand.getSelectedItem());
        ds.setCMDDate(tfDateRegex.getText());
        ds.setCMDTime(tfTimeRegex.getText());
        ds.setStatusScript(jtfStatusScript.getText());

        ds.setFindAnyDir(tfFindAnyDirectory.getText());
        ds.setFindAnyRx(tfFindAnyRegex.getText());
        
        if (rbDefaultMask.isSelected()) {
            ds.setRxType(RegexType.Default);
        } else if (rbDateTime.isSelected()) {
            ds.setRxType(RegexType.ShellRegex);
        } else if (rbDateTime.isSelected()) {
            ds.setRxType(RegexType.Any);
        }

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

    private boolean hasCheckedApp(AppProfile elementAt) {
        for (App app : elementAt.getApps()) {
            if (app.isChecked()) {
                return true;
            }
        }
        return false;
    }

    void setUncheckNonPrimary(Pair<ArrayList<String>, ArrayList<String>> cmdOuts) {
        if (cmdOuts != null) {
            for (String string : cmdOuts.getKey()) {
                String[] split = StringUtils.split(string, ",", 3);
                logger.debug(StringUtils.join(split, " - "));
                boolean appFound = false;
                for (AppProfile appProfile : ds.getAppProfiles()) {
                    for (App app : appProfile.getApps()) {
                        if (app.getName().equals(StringUtils.trimToEmpty(split[0]))) {
                            appFound = true;
                            if (!StringUtils.trimToEmpty(split[1]).equals("PRIMARY")) {
                                SettingsForm.info("Unchecking " + appProfile.getName() + "\\" + app.getName());
                                app.setChecked(false);
                            }
                        }

                    }

                }
                if (!appFound) {
                    SettingsForm.info("Not found app for " + string);
                }
            }
            int idx = clbProfile.getSelectedIndex();
            if (idx >= 0) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        clbProfile.clearSelection();
                        clbProfile.setSelectedIndex(idx);
                    }
                });
            }
        }

    }

    void editLoginProfiles() {
        if (loginProfilesEditor == null) {
            FieldProfile[] fieldProfiles = new FieldProfile[3];
            loginProfilesEditor = new Utils.swing.ValuesEditor(
                    (Window) this.getRootPane().getParent(),
                    "Login profiles",
                    "Select %d profiles",
                    new FieldProfile("Name", EditType.COMBOBOX, StringValue.class),
                    new FieldProfile("Username", EditType.COMBOBOX, StringValue.class),
                    new FieldProfile("Password", EditType.PASSWORD, PasswordValue.class)
            );

        }
        ArrayList<EditableValue[]> loginProfiles = new ArrayList<>();
        for (LoginProfile lp : ds.getLoginProfiles()) {
            loginProfiles.add(new EditableValue[]{new StringValue(lp.getName()),
                new StringValue(lp.getUsername()),
                new PasswordValue(lp.getPassword())});
        }
        loginProfilesEditor.setData(loginProfiles);
        if (loginProfilesEditor.doShow()) {
            ds.setLoginProfiles(loginProfilesEditor.getData());
            reloadLoginProfiles();
        }

    }

    private void reloadLoginProfiles() {
        cbLoginProfile.removeAllItems();
        for (LoginProfile loginProfile : ds.getLoginProfiles()) {
            cbLoginProfile.addItem(loginProfile.getName());
        }
    }

    private void setRxType(RegexType regexType) {
        this.regexType = regexType;

        jpRangeParams.removeAll();
        switch (regexType) {
            case Any:
                jpRangeParams.add(jpFindAny);
                break;
            case ShellRegex:
                jpRangeParams.add(jpRegEx);
                break;
        }
        jpRangeParams.repaint();
        jpRangeParams.revalidate();

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
