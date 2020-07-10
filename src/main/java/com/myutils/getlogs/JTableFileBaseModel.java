/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author stepan_sydoruk
 */
abstract class JTableFileBaseModel<EntryType> extends AbstractTableModel {

    private ArrayList<EntryType> tabRows = new ArrayList<>();

    public JTableFileBaseModel() {

    }

    @Override
    public int getRowCount() {
        return tabRows.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

    }

    private void addRow(AppProfile appProfile, App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog) {
//        tabRows.add(new CommandExecutor.JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, searchFile, logsDir, lfmt, lcaLog), searchFile));

    }

    private void addRow(AppProfile appProfile, App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog,
            String errorMessage) {
//        tabRows.add(new CommandExecutor.JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, searchFile, logsDir, lfmt, lcaLog), searchFile));

    }

    public void setData(ArrayList<EntryType> lsFilesLast) {

        tabRows = lsFilesLast;
        fireTableDataChanged();
    }

    public void setFiles(ArrayList<EntryType> lsFilesLast) {
        setData(lsFilesLast);

    }

    public EntryType get(int i) {
        return (EntryType) tabRows.get(i);
    }

    void clear() {
        tabRows.clear();
    }

    public abstract ArrayList<EntryType> getSelectedRows(int[] selectedRows);

}
