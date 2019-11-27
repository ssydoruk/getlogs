/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import java.awt.Component;
import java.awt.Point;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Stepan
 */
class JTablePopupMenu extends JPopupMenu {

    private final JTablePopup tab;
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    JTablePopupMenu(JTablePopup aThis) {
        tab = aThis;
        addPopupMenuListener(new PopupListener());

    }

    private class PopupListener implements PopupMenuListener {

        public PopupListener() {
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//            SwingUtilities.invokeLater(new Runnable() {
//                @Override
//                public void run() {
            Point popupPoint = SwingUtilities.convertPoint((Component) e.getSource(), new Point(0, 0), tab);
            logger.debug("p: " + popupPoint + " row: " + tab.rowAtPoint(popupPoint) + " col: " + tab.columnAtPoint(popupPoint));
            int row = tab.rowAtPoint(popupPoint);
            int col = tab.columnAtPoint(popupPoint);
            tab.setPopupRow(row);
            tab.setPopupCol(col);
            if (!tab.isRowSelected(row)) {
                logger.debug("1isRowSelected: ");
                tab.changeSelection(row, col, false, false);
            } else {
                logger.debug("isRowSelected1: ");

            }
            tab.callingPopup();
//                }
//            });

        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {

        }
    }

}
