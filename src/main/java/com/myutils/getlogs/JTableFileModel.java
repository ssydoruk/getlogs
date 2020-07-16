/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.util.ArrayList;

/**
 *
 * @author stepan_sydoruk
 */
class JTableFileModel extends JTableFileBaseModel<JTableFileEntry> {

    @Override
    public int getColumnCount() {
        return JTableFileEntry.fileTableColls.size();

    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        JTableFileEntry get = get(rowIndex);
        if (get != null) {
            return get.getColumn(columnIndex);
        } else {
            return null;
        }
    }

    @Override
    public ArrayList<JTableFileEntry> getSelectedRows(int[] selectedRows) {
        ArrayList<JTableFileEntry> ret1 = new ArrayList<>(selectedRows.length);
        for (int row : selectedRows) {
            ret1.add(get(row));
        }
        return ret1;
    }

    @Override
    public String getColumnName(int column) {
        return JTableFileEntry.getColumnName(column);

    }

}
