package com.myutils.getlogs;

import java.nio.file.attribute.FileTime;

public class OSFile {
    private String fileName;
    private long size;
    private FileTime creationTime;

    public OSFile(String fileName, long size) {
        this(fileName, size, null);
    }
    public OSFile(String fileName, long size, FileTime creationTime) {
        this.fileName = fileName;
        this.size = size;
        this.creationTime=creationTime;
    }
    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public FileTime getCreationTime() {
        return creationTime;
    }
}
