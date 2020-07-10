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
class JTablePasteFileList extends JTableFileListBase<JTableFileEntryGeneral> {

    public JTablePasteFileList() {
        super(new <JTableFileEntryGeneral>JTablePasteFileModel());
    }

}
