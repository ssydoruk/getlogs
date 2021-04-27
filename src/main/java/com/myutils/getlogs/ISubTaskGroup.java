package com.myutils.getlogs;

import java.io.IOException;

public interface ISubTaskGroup {

    ThreadGroup task() throws InterruptedException, IOException;
}
