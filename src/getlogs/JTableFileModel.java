/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author stepan_sydoruk
 */
class JTableFileModel extends AbstractTableModel {

    private ArrayList<CommandExecutor.JTableFileEntry> tabRows = new ArrayList<>();

    public JTableFileModel() {

    }

    @Override
    public int getRowCount() {
        return tabRows.size();
    }

    @Override
    public int getColumnCount() {
        return CommandExecutor.fileTableColls.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return CommandExecutor.fileTableColls.get(columnIndex);
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        CommandExecutor.JTableFileEntry get = tabRows.get(rowIndex);
        if (get != null) {
            return get.getColumn(columnIndex);
        } else {
            return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

    }

    public ArrayList<CommandExecutor.JTableFileEntry> getSelectedRows(int[] selectedRows) {
        ArrayList<CommandExecutor.JTableFileEntry> ret1 = new ArrayList<>(selectedRows.length);
        for (int row : selectedRows) {
            ret1.add(tabRows.get(row));
        }
        return ret1;
    }

    private void addRow(DownloadSettings.AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog) {
//        tabRows.add(new CommandExecutor.JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, searchFile, logsDir, lfmt, lcaLog), searchFile));

    }

    private void addRow(DownloadSettings.AppProfile appProfile, DownloadSettings.App ap, String theAppHost, String searchFile, String logsDir, boolean lfmt, boolean lcaLog,
            String errorMessage) {
//        tabRows.add(new CommandExecutor.JTableFileEntry(appProfile, getStorage(appProfile, ap, theAppHost, searchFile, logsDir, lfmt, lcaLog), searchFile));

    }

    public void setData(ArrayList<CommandExecutor.JTableFileEntry> lsFilesLast) {

        tabRows = lsFilesLast;
        fireTableDataChanged();
    }

    void clear() {
        tabRows.clear();
    }

}
