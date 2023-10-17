package com.myutils.getlogs;

import java.io.IOException;
import java.util.ArrayList;

@FunctionalInterface public interface IThreadingSubTask {

    ArrayList<ISubTask> task() throws InterruptedException, IOException;
}
