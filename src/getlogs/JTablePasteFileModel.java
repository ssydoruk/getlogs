/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import java.util.ArrayList;

/**
 *
 * @author stepan_sydoruk
 */
class JTablePasteFileModel extends JTableFileBaseModel<JTableFileEntryGeneral> {

    @Override
    public int getColumnCount() {
        return JTableFileEntryGeneral.fileTableColls.size();

    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        JTableFileEntryGeneral get = get(rowIndex);
        if (get != null) {
            return get.getColumn(columnIndex);
        } else {
            return null;
        }
    }

    @Override
    public ArrayList<JTableFileEntryGeneral> getSelectedRows(int[] selectedRows) {
        ArrayList<JTableFileEntryGeneral> ret1 = new ArrayList<>(selectedRows.length);
        for (int row : selectedRows) {
            ret1.add(get(row));
        }
        return ret1;
    }

    @Override
    public String getColumnName(int column) {
        return JTableFileEntryGeneral.getColumnName(column);
    }

}
