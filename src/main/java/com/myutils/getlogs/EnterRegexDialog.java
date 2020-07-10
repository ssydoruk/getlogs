/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import static com.myutils.getlogs.GetLogs.logger;

/**
 *
 * @author ssydoruk
 */
public class EnterRegexDialog extends javax.swing.JDialog {

    /**
     * A return status code - returned if Cancel button has been pressed
     */
    public static final int RET_CANCEL = 0;
    /**
     * A return status code - returned if OK button has been pressed
     */
    public static final int RET_OK = 1;
    private Pattern selectedRegEx = null;

    /**
     * Creates new form EnterRegexDialog
     */
    public EnterRegexDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        // Close the dialog when Esc is pressed
        if (parent != null) {
            Window windowAncestor = (Window) SwingUtilities.getWindowAncestor(parent);
            Point location = parent.getMousePosition();
            setLocation(location);
            setLocationRelativeTo(windowAncestor);
        }
        group.add(jrbUp);
        group.add(jrbDown);
        jrbDown.setSelected(true);

        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put(cancelName, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        });

        String okName = "OK";
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), okName);
        actionMap.put(okName, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                OKPressed();
            }
        });

        setOKButton();
//        jcbRegex.getModel().addListDataListener(jcbRegex);

//        jtfRegex.getDocument().addDocumentListener(new DocumentListener() {
//
//            public void insertUpdate(DocumentEvent e) {
//                setOKButton();
//            }
//
//            public void removeUpdate(DocumentEvent e) {
//                setOKButton();
//            }
//
//            public void changedUpdate(DocumentEvent e) {
//                setOKButton();
//            }
//        });
    }

    private void setOKButton() {
//        okButton.setEnabled(!jtfRegex.getText().isEmpty());
    }

    EnterRegexDialog() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the return status of this dialog - one of RET_OK or RET_CANCEL
     */
    public int getReturnStatus() {
        return returnStatus;
    }

    public static Pattern getRegex(String lastRegEx, boolean wholeWord) {
        if (lastRegEx != null && !lastRegEx.isEmpty()) {
            if (wholeWord) {
                StringBuilder rx = new StringBuilder(lastRegEx.length() + 2);
                if (lastRegEx.charAt(0) != '^') {
                    rx.append('^');
                }
                rx.append(lastRegEx);
                if (lastRegEx.charAt(lastRegEx.length() - 1) != '$') {
                    rx.append('$');
                }
                logger.debug("Searching for [" + rx + "]");
                return Pattern.compile(rx.toString(), Pattern.CASE_INSENSITIVE);
            } else {
                return Pattern.compile(lastRegEx, Pattern.CASE_INSENSITIVE);
            }
        }
        return null;
    }

    public boolean isMatchWholeWordSelected() {
        return jcbMatchWholeWord.isSelected();
    }

    private void OKPressed() {
        editLine = (String) jcbRegex.getSelectedItem();
        if (isRegexChecked()) {
            try {
                this.selectedRegEx = getRegex(editLine, isMatchWholeWordSelected());
            } catch (PatternSyntaxException e) {
                logger.error(
                        "Cannot compile regex [" + editLine + "]:\n"
                        + e.getMessage(),
                        "Regex error");
                return;
            }
        } else {
            selectedRegEx = null;
        }
        doClose(RET_OK);
    }

    public String getSearch() {
        return editLine;
    }

    private String editLine;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        group = new javax.swing.ButtonGroup();
        jcbMatchWholeWord = new javax.swing.JCheckBox();
        jcbIsRegex = new javax.swing.JCheckBox();
        jpUps = new javax.swing.JPanel();
        jrbUp = new javax.swing.JRadioButton();
        jrbDown = new javax.swing.JRadioButton();
        jPanel1 = new javax.swing.JPanel();
        jcbRegex = new javax.swing.JComboBox<>();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setTitle("Enter regular expression");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        jcbMatchWholeWord.setText("match whole word");

        jcbIsRegex.setText("regex");

        jpUps.setBorder(javax.swing.BorderFactory.createTitledBorder("Direction"));

        jrbUp.setText("up");

        jrbDown.setText("down");

        javax.swing.GroupLayout jpUpsLayout = new javax.swing.GroupLayout(jpUps);
        jpUps.setLayout(jpUpsLayout);
        jpUpsLayout.setHorizontalGroup(
            jpUpsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpUpsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpUpsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jrbDown)
                    .addComponent(jrbUp))
                .addContainerGap())
        );
        jpUpsLayout.setVerticalGroup(
            jpUpsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpUpsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jrbUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jrbDown)
                .addContainerGap())
        );

        jcbRegex.setEditable(true);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jcbRegex, 0, 483, Short.MAX_VALUE)
                .addGap(8, 8, 8)
                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton)
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton)
                    .addComponent(jcbRegex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jcbMatchWholeWord)
                            .addComponent(jcbIsRegex))
                        .addGap(18, 18, 18)
                        .addComponent(jpUps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 445, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jcbMatchWholeWord)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jcbIsRegex))
                    .addComponent(jpUps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        OKPressed();
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        cancelPressed();

    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Closes the dialog
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        doClose(RET_CANCEL);
    }//GEN-LAST:event_closeDialog

    private void doClose(int retStatus) {
        returnStatus = retStatus;
        setVisible(false);
        dispose();
    }

    public AccessibleContext getAccessibleContext() {
        return jcbIsRegex.getAccessibleContext();
    }

    public boolean isRegexChecked() {
        return jcbIsRegex.isSelected();
    }

    public boolean isDownChecked() {
        return jrbDown.isSelected();
    }

    public Action getAction() {
        return jrbDown.getAction();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.ButtonGroup group;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JCheckBox jcbIsRegex;
    private javax.swing.JCheckBox jcbMatchWholeWord;
    private javax.swing.JComboBox<String> jcbRegex;
    private javax.swing.JPanel jpUps;
    private javax.swing.JRadioButton jrbDown;
    private javax.swing.JRadioButton jrbUp;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables

    private int returnStatus = RET_CANCEL;

    public void setRegex(ArrayList<String> regExs) {
        setJCBElements(jcbRegex, regExs);
    }

    static public void setJCBElements(JComboBox jcb, ArrayList<String> elements) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) jcb.getModel();
        model.removeAllElements();
        if (!elements.isEmpty()) {
            for (String regEx : elements) {
                model.addElement(regEx);
            }
            jcb.setSelectedIndex(0);
            jcb.getEditor().selectAll();
        }
    }

    public void setDown(boolean down) {
        jrbDown.setSelected(down);
        jrbUp.setSelected(!down);
    }

    boolean checkMatch(String toString) {
        if (selectedRegEx != null) {
            if (toString == null) {
                return selectedRegEx.matcher("").find();
            } else {
                return selectedRegEx.matcher(toString).find();
            }

        } else {
            if (toString == null) {
                return false;
            }
            if (isMatchWholeWordSelected()) {
                return editLine.equalsIgnoreCase(toString);
            } else {
                return toString.toLowerCase().contains(editLine.toLowerCase());
            }
        }
    }

    void setShowUps(boolean b) {
        jpUps.setEnabled(b);
        jrbDown.setEnabled(b);
        jrbUp.setEnabled(b);
    }

    private void cancelPressed() {
        doClose(RET_CANCEL);
    }
}
