package com.myutils.getlogs;

import static Utils.ScreenInfo.CenterWindow;
import com.jidesoft.dialog.*;
import static com.myutils.getlogs.GetLogs.logger;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class InfoPanel extends StandardDialog {

    private int closeCause = JOptionPane.CANCEL_OPTION;
    private final JTable theTab;
    private final ArrayList<JButton> addButtons;
    private final String selectedFormat;
    private final Utils.swing.TableColumnAdjuster tca;
    JScrollPane jScrollPane;
    JButton jbFilter;

    JButton jbAll;
    ButtonPanel buttonPanel;
    private String btAllName="Get all";

    InfoPanel(Window parent, String title, JTable tab, String selectedFormat) {
        super(parent, title);
//        setModalityType(ModalityType.APPLICATION_MODAL);
        this.addButtons = new ArrayList<>();
        this.theTab = tab;
        this.selectedFormat = selectedFormat;
        tca = new Utils.swing.TableColumnAdjuster(theTab);
        tca.setColumnHeaderIncluded(true);
        jScrollPane = new JScrollPane(theTab);
        theTab.getTableHeader().setVisible(true);
        theTab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setAlwaysOnTop(true);
        pack();
    }

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

    public void doShow() {
//            this.setLocationRelativeTo(getParent());
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
                    s.append("[").append(theTab.getValueAt(i, j)).append("],");
                }
                logger.trace(s);
            }

        }

//        this.toFront();
//        setModal(false);
        setVisible(true);
        toFront();
//        setModal(true);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
//                setVisible(true);
//                setAlwaysOnTop(true);
//                toBack();
//                toFront();
//                requestFocus();
//                setModal(true);

//                setAlwaysOnTop(false);
            }
        });
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
        JPanel listPane = new JPanel(new BorderLayout());

        listPane.add(jScrollPane, BorderLayout.CENTER);

        return listPane;
    }

    @Override
    public ButtonPanel createButtonPanel() {
        buttonPanel = new ButtonPanel();
        for (JButton addButton : addButtons) {
            buttonPanel.add(addButton);
        }
        JButton cancelButton = new JButton();
        buttonPanel.addButton(cancelButton);

        cancelButton.setAction(new AbstractAction() {
            @Override
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

        jbAll = new JButton(getBtAllName());
        buttonPanel.addButton(jbAll);

        jbAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                theTab.editingCanceled(null);
                theTab.selectAll();
                setCloseCause(JOptionPane.OK_OPTION);
                dispose();
            }
        });

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

    public String getBtAllName() {
        return btAllName;
    }

    public void setBtAllName(String btAllName) {
        this.btAllName = btAllName;
    }

    public void showButtonAll(boolean doShow) {
        jbAll.setVisible(doShow);
    }
}
