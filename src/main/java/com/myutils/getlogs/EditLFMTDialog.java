package com.myutils.getlogs;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class EditLFMTDialog extends StandardDialog {

    private EnterPanel lfmtBaseDir;

    EnterPanel lfmt;
    EnterPanel lfmtInstance;

    public EditLFMTDialog() {
        super();
        setTitle("Edit LFMT host");
    }

    public EnterPanel getLfmt() {
        return lfmt;
    }

    public EnterPanel getLfmtInstance() {
        return lfmtInstance;
    }

    public EnterPanel getBaseDir() {
        return lfmtBaseDir;
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public JComponent createContentPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        lfmt = new EnterPanel("LFMT host");
        content.add(lfmt.getEnterPanel());
        lfmtInstance = new EnterPanel("LFMT instance");
        content.add(lfmtInstance.getEnterPanel());
        lfmtBaseDir = new EnterPanel("Log base dir");
        content.add(lfmtBaseDir.getEnterPanel());
        return content;
    }

    @Override
    public ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = new ButtonPanel();
        JButton cancelButton = new JButton();
        buttonPanel.addButton(cancelButton);

        cancelButton.setAction(new AbstractAction() {
            @Override
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

    public boolean doShow() {

        setModal(true);

        pack();

//            ScreenInfo.CenterWindow(this);
        setLocationRelativeTo(getParent());
        setVisible(true);
        return getDialogResult() == StandardDialog.RESULT_AFFIRMED;
    }

    class EnterPanel {

        private final JTextField tbValue;
        private final JPanel enterPanel;

        EnterPanel(String title) {
            enterPanel = new JPanel();
            enterPanel.setLayout(new BoxLayout(enterPanel, BoxLayout.LINE_AXIS));
            enterPanel.add(new JLabel(title));
            tbValue = new JTextField();
            enterPanel.add(tbValue);
        }

        public JPanel getEnterPanel() {
            return enterPanel;
        }

        public String getText() {
            return tbValue.getText();
        }

        public void setText(String txt) {
            tbValue.setText(txt);
        }
    }
}
