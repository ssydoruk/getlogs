package com.myutils.getlogs;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public final class DummyFrame extends JFrame {

    DummyFrame(String title, List<? extends Image> iconImages) {
        super(title);
        setUndecorated(true);
        setVisible(true);
        setLocationRelativeTo(null);
        setIconImages(iconImages);
    }
}
