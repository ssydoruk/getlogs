/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.*;
import Utils.swing.*;
import com.jidesoft.swing.*;

import static com.myutils.getlogs.GetLogs.logger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * @author stepan_sydoruk
 */
public class StringListEdit extends JPanel {

    private Utils.swing.ValuesEditor.IAddChoices addChoices;

    private final DefaultListModel<Object> lmItems;
    private final JButton btChange;
    private Utils.swing.ValuesEditor stringsEditor;
    private final String columnTitle;
    private ArrayList<Pair<String, Boolean>> data;
    CheckBoxList clbItems;
    private IDataChangedFun updatedFun = null;

    public StringListEdit(String columnTitle) {
        super();

        this.columnTitle = columnTitle;

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        lmItems = new DefaultListModel<>();
        clbItems = new CheckBoxList(lmItems);
        add(new JScrollPane(clbItems));
        clbItems.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        SearchableUtils.installSearchable(clbItems);

        clbItems.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                clbItemsCheckedChanged(evt);
            }

        });
        clbItems.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                clbItemsSelectionChanged(evt);
            }

        });

        btChange = new JButton();
        add(btChange);
        btChange.setAction(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                btChangePressed(e);
            }

        });
        btChange.setText("...");

    }

    @Override
    public void setEnabled(boolean enabled) {
        btChange.setEnabled(enabled);
        clbItems.setEnabled(enabled);
        super.setEnabled(enabled); //To change body of generated methods, choose Tools | Templates.
    }

    private void btChangePressed(ActionEvent e) {

        ArrayList<EditableValue[]> values = new ArrayList<>();

        for (Object entry : lmItems.toArray()) {
            if (!entry.equals(CheckBoxList.ALL_ENTRY)) {
                values.add(
                        new EditableValue[]{new StringValue(entry.toString())});
            }

        }

        getStringsEditor().setData(new Object[]{columnTitle},
                values
        );
        getStringsEditor()
                .setAddChoices(addChoices);
        if (getStringsEditor().doShow()) {

            ArrayList<EditableValue[]> data1 = getStringsEditor().getData();
            ArrayList<Pair<String, Boolean>> itemsState = new ArrayList<>();

            for (int i = 0; i < data1.size(); i++) {
                EditableValue[] objArr = data1.get(i);
                if (objArr != null && objArr.length > 0) {
                    String elem = objArr[0].toString();
                    ListEntry le = leIdx(elem);
                    if (le != null) {
                        itemsState.add(new Pair(elem, le.isSelected()));
                    } else {
                        itemsState.add(new Pair(elem, true));
                    }
                }

            }

            setData(itemsState);

            dataChanged();
        }
//            ds.loadLFMTs(stringsEditor.getData());
    }

    private ListEntry leIdx(String item) {

        for (Object object
                : lmItems.toArray()) {
            if (!object.equals(CheckBoxList.ALL_ENTRY)) {
                ListEntry le = (ListEntry) object;
                if (le.getKey().equals(item)) {
                    return le;
                }

            }
        }

        return null;

    }

    private void clbItemsCheckedChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting()) {
            logger.debug("Item checked");
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();

            for (int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++) {
                if (i < lmItems.getSize()) {
                    if (!lmItems.getElementAt(i).equals(CheckBoxList.ALL_ENTRY)) {
                        ((ListEntry) lmItems.getElementAt(i))
                                .setSelected(lsm.isSelectedIndex(i));
                    }
                }

            }
            dataChanged();
        }
    }

    private void clbItemsSelectionChanged(ListSelectionEvent evt) {
        logger.debug("Item selection changed");
    }

    public void setUpdatedFun(IDataChangedFun updatedFun) {
        this.updatedFun = updatedFun;
    }

    public ArrayList<Pair<String, Boolean>> getData() {
        return data;
    }

    void setData(HashMap<String, Boolean> nameSuffixes) {
        if (nameSuffixes != null && !nameSuffixes.isEmpty()) {
            ArrayList<Pair<String, Boolean>> ar = new ArrayList<>(nameSuffixes.size());
            for (Map.Entry<String, Boolean> entry : nameSuffixes.entrySet()) {
                String key = entry.getKey();
                Boolean value = entry.getValue();
                ar.add(new Pair(key, value));
            }
            setData(ar);
        } else {
            setData((ArrayList<Pair<String, Boolean>>) null);
        }
    }

    void setData(ArrayList<Pair<String, Boolean>> nameSuffixes) {
        this.data = nameSuffixes;

        clbItems.setValueIsAdjusting(true);
        CheckBoxListSelectionModel clbItemsSelectionModel = clbItems.getCheckBoxListSelectionModel();
        ListSelectionListener[] listSelectionListeners = clbItemsSelectionModel.getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            clbItemsSelectionModel.removeListSelectionListener(listSelectionListener);
        }

//                        clbAppSelectionModel.addSelectionInterval(maxIndex, maxIndex);
        clbItemsSelectionModel.clearSelection();
        lmItems.clear();

        if (data != null) {
            ArrayList<ListEntry> selected = new ArrayList<>();
            for (Pair<String, Boolean> entry : data) {
                ListEntry le = new ListEntry(entry.getKey(), entry.getValue());
                lmItems.addElement(le);
                if (le.isSelected()) {
                    selected.add(le);

                }
            }
            lmItems.insertElementAt(CheckBoxList.ALL_ENTRY, 0);

            clbItems.addCheckBoxListSelectedValues(selected.toArray(new ListEntry[selected.size()]));

        }
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            clbItemsSelectionModel.addListSelectionListener(listSelectionListener);
        }
    }

    void noSelection() {
        setData((ArrayList<Pair<String, Boolean>>) null);
    }

    private int leIdx(ArrayList<Object[]> data1, ListEntry le) {
        for (int i = 0; i < data1.size(); i++) {
            Object[] objArr = data1.get(i);
            if (objArr != null && objArr.length > 0) {
                String elem = (String) objArr[0];
                logger.debug(elem + "-" + le.getKey());
                if (elem.equals(le.getKey())) {
                    return i;
                }
            }

        }
        return -1;

    }

    private void dataChanged() {
        if (updatedFun == null) {
            logger.info("Update function not defined");
        } else {
            ArrayList<Pair<String, Boolean>> d = new ArrayList<>(lmItems.size());
            for (Object object : lmItems.toArray()) {
                if (!object.equals(CheckBoxList.ALL_ENTRY)) {
                    ListEntry le = (ListEntry) object;
                    d.add(new Pair(le.getKey(), le.isSelected()));
                }
            }
            updatedFun.dataChanged(d);
        }
    }

    void setAddChoices(Utils.swing.ValuesEditor.IAddChoices iAddChoices) {
        this.addChoices = iAddChoices;
    }

    private Utils.swing.ValuesEditor getStringsEditor() {
        if (stringsEditor == null) {
            stringsEditor = new Utils.swing.ValuesEditor(SwingUtilities.getWindowAncestor(this), "Edit list",
                    "Select %d LFMTs");

        }
        return stringsEditor;

    }

    void setData(ArrayList<Pair<String, Boolean>> afterActions, boolean b) {
        setData(afterActions);
        clbItems.setVisibleRowCount(lmItems.getSize());
        clbItems.setMaximumSize(new Dimension(clbItems.getMaximumSize().width, clbItems.getMinimumSize().height));
    }

    @FunctionalInterface
    public interface IDataChangedFun {

        public void dataChanged(ArrayList<Pair<String, Boolean>> newData);

    }

    public class ListEntry extends Pair<String, Boolean> {

        public ListEntry(String key, Boolean value) {
            super(key, value);
        }

        @Override
        public String toString() {
            return getKey();
        }

        public boolean isSelected() {
            return getValue();
        }

        private void setSelected(boolean selectedIndex) {
            setValue(selectedIndex);
        }

    }

}
