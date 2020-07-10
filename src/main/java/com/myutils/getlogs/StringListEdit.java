/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.ValuesEditor;
import Utils.Pair;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.SearchableUtils;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import static com.myutils.getlogs.GetLogs.logger;

/**
 *
 * @author stepan_sydoruk
 */
public class StringListEdit extends JPanel {


    private ValuesEditor.IAddChoices addChoices;

    @Override
    public void setEnabled(boolean enabled) {
        btChange.setEnabled(enabled);
        clbItems.setEnabled(enabled);
        super.setEnabled(enabled); //To change body of generated methods, choose Tools | Templates.
    }

    private final DefaultListModel<Object> lmItems;
    private final JButton btChange;
    private ValuesEditor stringsEditor;
    private final String columnTitle;
    private ArrayList<Pair<String, Boolean>> data;
    CheckBoxList clbItems;

    public StringListEdit(String columnTitle) {
        super();

        this.columnTitle = columnTitle;

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        lmItems = new DefaultListModel<Object>();
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

    private void btChangePressed(ActionEvent e) {

        ArrayList<Object[]> values = new ArrayList<>();
//        for (DownloadSettings.LFMTHostInstance hi : ds.getLfmtHostInstances()) {
//            values.add(new Object[]{hi.getHost(), hi.getInstance(), hi.getBaseDir()});
//        }
        for (Object entry : lmItems.toArray()) {
            if (!entry.equals(CheckBoxList.ALL_ENTRY)) {
                values.add(new String[]{((ListEntry) entry).toString()});
            }

        }

        getStringsEditor().setData(new Object[]{columnTitle
        },
                values
        );
        getStringsEditor()
                .setAddChoices(addChoices);
        getStringsEditor()
                .doShow();
//        ArrayList<Object[]> data1 = getStringsEditor().getData();
//        for (Object object
//                : lmItems.toArray()) {
//            if (!object.equals(CheckBoxList.ALL_ENTRY)) {
//                ListEntry le = (ListEntry) object;
//                int idx = leIdx(data1, le);
//                if (idx < 0) {
//                    lmItems.removeElement(le);
//                } else {
//                    data1.set(idx, null);
//                }
//            }
//        }
//        for (Object[] objects : data1) {
//            if (objects != null) {
//                lmItems.addElement(new ListEntry(objects[0].toString(), Boolean.TRUE));
//            }
//        }

        ArrayList<Object[]> data1 = getStringsEditor().getData();
        ArrayList<Pair<String, Boolean>> itemsState = new ArrayList<>();

        for (int i = 0; i < data1.size(); i++) {
            Object[] objArr = data1.get(i);
            if (objArr != null && objArr.length > 0) {
                String elem = (String) objArr[0];
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

    private IDataChangedFun updatedFun = null;

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

    void setAddChoices(ValuesEditor.IAddChoices iAddChoices) {
        this.addChoices = iAddChoices;
    }

    private ValuesEditor getStringsEditor() {
        if (stringsEditor == null) {
            stringsEditor = new ValuesEditor(SwingUtilities.getWindowAncestor(this), "Edit list",
                    "Select %d LFMTs");

        }
        return stringsEditor;

    }

    void setData(ArrayList<Pair<String, Boolean>> afterActions, boolean b) {
        setData(afterActions);
        clbItems.setVisibleRowCount(lmItems.getSize());
        clbItems.setMaximumSize(new Dimension(clbItems.getMaximumSize().width, clbItems.getMinimumSize().height));
    }

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
