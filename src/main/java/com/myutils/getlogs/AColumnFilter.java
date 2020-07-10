/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.getlogs;

import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author ssydoruk
 */
abstract class AColumnFilter {

    static final String RECORD_EMPTY = "<(Empty)>";

    public AColumnFilter() {
    }

    abstract boolean showValue(Object val);

    public static class StringsFilter extends AColumnFilter {

        private HashSet<String> values;

        public StringsFilter() {
        }

        public StringsFilter(HashSet<String> selValues) {
            this.values = selValues;
        }

        @Override
        boolean showValue(Object val) {
            if (val == null || val.toString().isEmpty()) {
                if (values.contains(RECORD_EMPTY)) {
                    return true;
                }
            } else {
                return values.contains(val.toString());

            }
            return false;
        }
    }

    public static class PositionalFilter extends AColumnFilter {

        private int row;
        private boolean isUp;

        public PositionalFilter() {
        }

        PositionalFilter(int popupRow, boolean up) {
            this.row = popupRow;
            this.isUp = up;
        }

        @Override
        boolean showValue(Object val) {
            return true;
        }
    }

    static public abstract class NumberFilter extends AColumnFilter {

        private String name;
        private boolean needsSecond;
        private double firstNum;
        private double secondNum;
        private boolean showNonNumbers = false;

        public boolean isShowNonNumbers() {
            return showNonNumbers;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isNeedsSecond() {
            return needsSecond;
        }

        public void setNeedsSecond(boolean needsSecond) {
            this.needsSecond = needsSecond;
        }

        public double getFirstNum() {
            return firstNum;
        }

        public void setFirstNum(String firstNum) {
            if (firstNum != null && !firstNum.isEmpty()) {
                this.firstNum = Double.parseDouble(firstNum);
            } else {
                this.firstNum = 0;
            }
        }

        public double getSecondNum() {
            return secondNum;
        }

        public void setSecondNum(String secondNum) {
            if (secondNum != null && !secondNum.isEmpty()) {
                this.secondNum = Double.parseDouble(secondNum);
            } else {
                this.secondNum = 0;
            }
        }

        @Override
        public String toString() {
            return name;
        }

        public NumberFilter(String name, boolean needsSecond) {
            this.name = name;
            this.needsSecond = needsSecond;
        }

        public void setShowNonNumbers(boolean selected) {
            this.showNonNumbers = selected;
        }
    }

    public static class FilterComboboxModel extends ArrayList<AColumnFilter> implements ComboBoxModel {

        private Object selectedItem;

        public FilterComboboxModel() {
            super();
        }

        @Override
        public void setSelectedItem(Object anItem) {
            this.selectedItem = anItem;
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }

        @Override
        public int getSize() {
            return super.size();
        }

        @Override
        public Object getElementAt(int index) {
            return super.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
        }

        @Override
        public void removeListDataListener(ListDataListener l) {

        }

    }
}
