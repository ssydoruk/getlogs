package com.myutils.getlogs;

public class OSFile {
    private String fileName;
    private long size;

    public OSFile(String fileName, long size) {
        this.fileName = fileName;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }
}
