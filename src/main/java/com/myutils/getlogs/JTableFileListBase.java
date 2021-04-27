/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import Utils.*;
import static com.myutils.getlogs.AColumnFilter.RECORD_EMPTY;
import com.myutils.getlogs.InfoPanel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 *
 * @author stepan_sydoruk
 */
abstract class JTableFileListBase<EntryType> extends JTablePopup {

    private final HashMap<Integer, AColumnFilter> columnFilters = new HashMap<>();
    private final JMenuItem miCancelFilters;
    private final JMenuItem miCancelColumnFilter;
    private final TableCellRenderer savedHeaderRenderer;
    TableRowSorter<TableModel> sorter = null;

    public JTableFileListBase(JTableFileBaseModel mod) {
        super();
        savedHeaderRenderer = getTableHeader().getDefaultRenderer();
        setModel(mod);
        sorter = new TableRowSorter<>(mod);

        popupMenu.add(new UniqueColumnsShown());
        popupMenu.add(new UniqueColumnsAll());
        miCancelFilters = popupMenu.add(new CancelFilters());
        miCancelColumnFilter = popupMenu.add(new CancelColumnFilter());

    }

    private void doCancelFilters() {
        int sel = popupRow;
        if (sel >= 0) {
            sel = convertRowIndexToModel(sel);
        }
        for (Integer key : columnFilters.keySet()) {
            getColumnModel().getColumn(key).setHeaderRenderer(savedHeaderRenderer);
        }

        getTableHeader().repaint();
        setRowSorter(null);
        columnFilters.clear();
        ((AbstractTableModel) getModel()).fireTableDataChanged();
        if (sel >= 0) {
            setRowSelectionInterval(sel, sel);
            scrollRectToVisible(new Rectangle(getCellRect(sel, 0, true)));
        }
    }

    public boolean isEmpty() {
        return getModel().getRowCount() == 0;
    }

    @Override
    void theMousePressed(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void callingPopup() {
        miCancelColumnFilter.setEnabled(popupCol > 0 && columnFilters.containsKey(popupCol));
        miCancelFilters.setEnabled(!columnFilters.isEmpty());
    }

    public void clearTable() {
        ((JTableFileBaseModel) getModel()).clear();
    }

    public ArrayList<EntryType> getSelectedFiles() {
        int[] selectedRows = getSelectedRows();
        if (selectedRows != null && selectedRows.length > 0) {
            return ((JTableFileBaseModel) getModel()).getSelectedRows(selectedRows);
        } else {
            return null;
        }
    }

    ArrayList<Pair<String, Integer>> getUniqueVals(int popupCol, boolean allValues) {
        HashMap<String, AtomicInteger> hsTmp = new HashMap<>();
        if (allValues) {
            TableModel model = getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object valueAt = model.getValueAt((i), popupCol);
                String name = (valueAt == null || valueAt.toString().isEmpty()) ? RECORD_EMPTY : valueAt.toString();
                AtomicInteger cnt = hsTmp.get(name);
                if (cnt == null) {
                    hsTmp.put(name, new AtomicInteger(1));
                } else {
                    cnt.incrementAndGet();
                }
            }

        } else {
            for (int i = 0; i < getRowCount(); i++) {
                Object valueAt = getValueAt((i), popupCol);
                String name = (valueAt == null || valueAt.toString().isEmpty()) ? RECORD_EMPTY : valueAt.toString();
                AtomicInteger cnt = hsTmp.get(name);
                if (cnt == null) {
                    hsTmp.put(name, new AtomicInteger(1));
                } else {
                    cnt.incrementAndGet();
                }
            }
        }
        ArrayList<String> sorted = new ArrayList<>(hsTmp.keySet());
        Collections.sort(sorted);
        ArrayList<Pair<String, Integer>> ret = new ArrayList<>(sorted.size());
        for (String string : sorted) {
            ret.add(new Pair(string, hsTmp.get(string)));
        }
        return ret;
    }

    private void applyFilter() {

        ArrayList<RowSetFilter> andFilters = new ArrayList<>();
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(Color.RED);
//        headerRenderer.setForeground(Color.RED);
//        getTableHeader().setDefaultRenderer(headerRenderer);
        for (Map.Entry<Integer, AColumnFilter> entrySet : columnFilters.entrySet()) {
            Integer key = entrySet.getKey();
            AColumnFilter value = entrySet.getValue();

            getColumnModel().getColumn(key).setHeaderRenderer(headerRenderer);

//            for (int i = 0; i < getModel().getColumnCount(); i++) {
//        getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
//}
            andFilters.add(new RowSetFilter(key, value));

        }
        sorter.setRowFilter(RowFilter.andFilter(andFilters));
        setRowSorter(sorter);
        ((AbstractTableModel) getModel()).fireTableDataChanged();
//        getTableHeader().invalidate();
//        getTableHeader().repaint();

    }

    public void setFiles(ArrayList<EntryType> lsFilesLast) {
        ((JTableFileBaseModel) getModel()).setData(lsFilesLast);

    }

    protected class UniqueColumns extends AbstractAction {

        private final boolean showAll;
        InfoPanel p = null;
        private JTablePopup uniquePopup = null;

        public UniqueColumns(String menuTitle, boolean showAll) {
            super(menuTitle);
            this.showAll = showAll;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            uniqueColumnValues(e);
        }

        public void uniqueColumnValues(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            JTablePopup table = (JTablePopup) popup.getInvoker();

            ArrayList<Pair<String, Integer>> uniqueVals = getUniqueVals(popupCol, showAll);
            Object valueAt = getValueAt(popupRow, popupCol);

            JTablePopup tab = getJTablePopup();

            DefaultTableModel infoTableModel = new DefaultTableModel();
            infoTableModel.addColumn("Values");
            infoTableModel.addColumn("Count");
            int grandTotal = 0;
            int selectedRowIdx = -1;
            for (int i = 0; i < uniqueVals.size(); i++) {
                Pair<String, Integer> val = uniqueVals.get(i);
                infoTableModel.addRow(new Object[]{val.getKey(), val.getValue()});
                if (selectedRowIdx < 0) {
                    if (valueAt != null) {
                        if (valueAt.equals(val.getKey())) {
                            selectedRowIdx = i;

                        }
                    }
                }
                grandTotal++;
            }

//        infoTableModel.addRow(new Object[]{"TOTAL(" + grandTotal + ")"});
            tab.setModel(infoTableModel);
            if (selectedRowIdx >= 0) {
                ListSelectionModel selectionModel1 = tab.getSelectionModel();
                selectionModel1.setSelectionInterval(selectedRowIdx, selectedRowIdx);
            }

            String theTitle = "Unique values in column (total " + grandTotal + ")";
            p = new InfoPanel( (JFrame) table.getRootPane().getParent(), theTitle, tab,
                    "Download %d files");

            p.doShow();
            if (p.getCloseCause() == JOptionPane.OK_OPTION) { //apply filter
                int[] selectedRows = tab.getSelectedRows();
                if (selectedRows.length == uniqueVals.size()) {
                    doCancelFilters();
                } else {
                    HashSet<String> selValues = new HashSet<>(selectedRows.length);
                    for (int selectedRow : selectedRows) {
                        selValues.add((String) infoTableModel.getValueAt(selectedRow, 0));
                    }
                    columnFilters.put(table.convertColumnIndexToModel(popupCol), new AColumnFilter.StringsFilter(selValues));
                }
                applyFilter();
            }
            GetLogs.logger.trace(table.getSelectedRow() + " : " + table.getSelectedColumn());
        }

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
                popupMenu1.add(new CopyAll());
                popupMenu1.add(new CopySelected());

            }
            return uniquePopup;
        }

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

    class CopyAll extends AbstractAction {

        public CopyAll() {
            super("Copy all rows");
            putValue(SHORT_DESCRIPTION, "Search in the table (Ctrl-F)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).copyAll();

        }

    }

    class CopySelected extends AbstractAction {

        public CopySelected() {
            super("Copy selected");
            putValue(SHORT_DESCRIPTION, "Search in the table (Ctrl-F)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).copySelected();

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

    protected class CancelColumnFilter extends AbstractAction {

        public CancelColumnFilter() {
            super("Cancel column filter");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int selRow = popupRow;
            if (selRow >= 0) {
                selRow = convertRowIndexToModel(selRow);
            }
            int selCol = popupCol;
            if (selCol >= 0) {
                selCol = convertColumnIndexToModel(selCol);
            }
            if (columnFilters.containsKey(selCol)) {
                columnFilters.remove(selCol);

//                getTableHeader().repaint();
//                setRowSorter(null);
//                columnFilters.clear();
//                ((AbstractTableModel) getModel()).fireTableDataChanged();
//                if (sel >= 0) {
//                    setRowSelectionInterval(sel, sel);
//                    scrollRectToVisible(new Rectangle(getCellRect(sel, 0, true)));
//                } else {
//                    sel = -1;
//                }
//                filterChanged();
                TableColumn column = getColumnModel().getColumn(selCol);
                if (column != null) {
                    column.setHeaderRenderer(savedHeaderRenderer);
                }
                applyFilter();
                if (selRow >= 0) {
                    selRow = convertRowIndexToView(selRow);
                    if (selRow >= 0) {
                        setRowSelectionInterval(selRow, selRow);
                        scrollRectToVisible(new Rectangle(getCellRect(selRow, 0, true)));
                    }
                }
            }
        }
    }

    protected class CancelFilters extends AbstractAction {

        public CancelFilters() {
            super("Cancel filters");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doCancelFilters();

        }

    }

    protected class UniqueColumnsAll extends UniqueColumns {

        public UniqueColumnsAll() {
            super("Unique values in column (all)", true);
        }
    }

    protected class UniqueColumnsShown extends UniqueColumns {

        public UniqueColumnsShown() {
            super("Unique values in column as shown", false);
        }
    }

    private class RowSetFilter extends RowFilter<TableModel, Object> {

        private final Integer col;
        private final AColumnFilter filter;

        public RowSetFilter(Integer col, AColumnFilter filter) {
            this.col = col;
            this.filter = filter;
        }

        @Override
        public boolean include(RowFilter.Entry<? extends TableModel, ? extends Object> entry) {
            return filter.showValue(entry.getValue(col));
        }

    }

}
