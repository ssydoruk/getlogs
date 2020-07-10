/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

/**
 *
 * @author stepan_sydoruk
 */
class JTableFileList extends JTableFileListBase<JTableFileEntry> {

    public JTableFileList() {
        super(new <JTableFileEntry>JTableFileModel());
    }

}
