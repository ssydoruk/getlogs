/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import Utils.TableColumnAdjuster;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import static com.jidesoft.dialog.StandardDialog.RESULT_AFFIRMED;
import static com.jidesoft.dialog.StandardDialog.RESULT_CANCELLED;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author stepan_sydoruk
 */
public class ValuesEditor extends StandardDialog {

    private int closeCause = JOptionPane.CANCEL_OPTION;
    private JTable tab;
    private final String selectedFormat;
    private TableColumnAdjuster tca;

    public int getCloseCause() {
        return closeCause;
    }

    public void setCloseCause(int closeCause) {
        this.closeCause = closeCause;
    }

    private void selectionChanged() {
        int rowsSelected = tab.getSelectedRows().length;
        editButton.setEnabled(rowsSelected == 1);
        deleteButton.setEnabled(rowsSelected == 1);
    }

    ValuesEditor(Window parent, String title, String selectedFormat) {
        super(parent, title);
        this.tab = new JTable();
        this.selectedFormat = selectedFormat;
        tca = new TableColumnAdjuster(tab);

    }

    public boolean doShow() {
        setModal(true);

        tca.adjustColumns();
        pack();
        if (LogManager.getLogger().isTraceEnabled()) {
            LogManager.getLogger().trace("Show info PanelDialog; title=" + getTitle() + "; tab cols:" + tab.getColumnCount() + " rows: " + tab.getRowCount());
            StringBuilder s = new StringBuilder(512);
            for (int i = 0; i < tab.getRowCount(); i++) {
                s.setLength(0);
                for (int j = 0; j < tab.getColumnCount(); j++) {
                    s.append("[" + tab.getValueAt(i, j) + "],");
                }
                LogManager.getLogger().trace(s);
            }

        }

//            ScreenInfo.CenterWindow(this);
        this.setLocationRelativeTo(getParent());
        setVisible(
                true);
        return getDialogResult() == StandardDialog.RESULT_AFFIRMED;
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

    ButtonPanel buttonPanel;
    JButton cancelButton;
    JButton addButton;
    JButton editButton;
    JButton deleteButton;

    @Override
    public ButtonPanel createButtonPanel() {
        buttonPanel = new ButtonPanel();

        addButton = new JButton();
        buttonPanel.addButton(addButton);
        addButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                addValuePressed(e);

            }

        });
        addButton.setText("Add");

        editButton = new JButton();
        buttonPanel.addButton(editButton);
        editButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                editValuePressed(e);

            }

        });
        editButton.setText("Edit");

        deleteButton = new JButton();
        buttonPanel.addButton(deleteButton);
        deleteButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                deleteValuePressed(e);

            }

        });
        deleteButton.setText("Delete");

        cancelButton = new JButton();
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

        tab.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                selectionChanged();
            }
        });
        selectionChanged();

        String act = "Cancel";

        tab.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), act);
        tab.getActionMap().put(act, cancelButton.getAction());

        setDefaultCancelAction(cancelButton.getAction());
        setDefaultAction(cancelButton.getAction());
        getRootPane().setDefaultButton(cancelButton);

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
        return buttonPanel;
    }

    EditValuesDialog editDialog = null;

    private void addValuePressed(ActionEvent e) {
        if (editDialog == null) {
            editDialog = new EditValuesDialog(tab.getColumnModel());
        }
        ArrayList<String> vals;
        if ((vals = editDialog.doShow(null)) != null) {
            infoTableModel.addRow(vals.toArray(new String[vals.size()]));
        }

    }

    private void editValuePressed(ActionEvent e) {
        int selectedRow = tab.getSelectedRow();
        if (selectedRow >= 0) {
            ArrayList<String> vals = new ArrayList<>(tab.getColumnCount());
            for (int i = 0; i < tab.getColumnCount(); i++) {
                vals.add((String) tab.getValueAt(selectedRow, i));
            }
            if (editDialog == null) {
                editDialog = new EditValuesDialog(tab.getColumnModel());
            }
            if ((vals = editDialog.doShow(vals)) != null) {
                for (int i = 0; i < vals.size(); i++) {
                    infoTableModel.setValueAt(vals.get(i), selectedRow, i);
                }
            }
        }
    }

    private void deleteValuePressed(ActionEvent e) {
        int selectedRow = tab.getSelectedRow();
        if (selectedRow >= 0) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < tab.getColumnCount(); i++) {
                if (s.length() > 0) {
                    s.append(" - ");
                }
                s.append((String) tab.getValueAt(selectedRow, i));
            }
            if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete\n" + s, "Please confirm", YES_NO_OPTION, QUESTION_MESSAGE)
                    == JOptionPane.YES_OPTION) {
                infoTableModel.removeRow(selectedRow);
            }

        }
    }

    private boolean doShow(String theTitle) {
        this.setTitle(theTitle);
        return doShow();
    }

    private DefaultTableModel infoTableModel;

    void setData(Object[] columns, ArrayList<Object[]> values) {
        infoTableModel = new DefaultTableModel();
        for (Object column : columns) {
            infoTableModel.addColumn(column);
        }
        for (Object[] value : values) {
            infoTableModel.addRow(value);
        }
        tab.setModel(infoTableModel);
    }

    public ArrayList<Object[]> getData() {
        ArrayList<Object[]> ret = new ArrayList<>(infoTableModel.getRowCount());
        for (int i = 0; i < infoTableModel.getRowCount(); i++) {
            Object[] vals = new Object[infoTableModel.getColumnCount()];
            for (int j = 0; j < infoTableModel.getColumnCount(); j++) {
                vals[j] = infoTableModel.getValueAt(i, j);
            }
            ret.add(vals);

        }

        return ret;
    }

    class EditValuesDialog extends StandardDialog {

        private EnterPanel lfmtBaseDir;

        public EditValuesDialog() {
            super();
        }

        ArrayList col;
        ArrayList<EnterPanel> pan;

        private EditValuesDialog(TableColumnModel columnModel) {
            col = new ArrayList<>(columnModel.getColumnCount());
            pan = new ArrayList<>(columnModel.getColumnCount());
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                col.add(columnModel.getColumn(i).getHeaderValue());
                pan.add(new EnterPanel((String) columnModel.getColumn(i).getHeaderValue()));
            }

        }

        private ArrayList<String> doShow(ArrayList<String> vals) {
            if (vals != null) {
                setTitle("Edit entry");
                for (int i = 0; i < pan.size(); i++) {
                    pan.get(i).setText(vals.get(i));
                }
            } else {
                for (int i = 0; i < pan.size(); i++) {
                    pan.get(i).setText(null);
                }
                setTitle("New entry");
            }
            return doShow();
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

        @Override
        public JComponent createBannerPanel() {
            return null;
        }

        @Override
        public JComponent createContentPanel() {
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
            for (int i = 0; i < pan.size(); i++) {
                content.add(pan.get(i).getEnterPanel());

            }

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

        public ArrayList<String> doShow() {

            setModal(true);

            pack();

//            ScreenInfo.CenterWindow(this);
            setLocationRelativeTo(getParent());
            setVisible(true);
            setAlwaysOnTop(true);
            toFront();
            if (getDialogResult() == StandardDialog.RESULT_AFFIRMED) {
                ArrayList<String> ret = new ArrayList<>(pan.size());
                for (EnterPanel enterPanel : pan) {
                    ret.add(enterPanel.getText());
                }
                return ret;

            } else {
                return null;
            }

        }
    }

}
