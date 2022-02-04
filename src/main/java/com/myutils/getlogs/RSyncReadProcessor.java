package com.myutils.getlogs;

import Utils.FileUtils;
import Utils.UnixProcess.IProcessOutputRead;
import com.myutils.logbrowser.indexer.Main;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class RSyncReadProcessor {
    private final boolean zipDest;
    private  Path logsDir;
    private  Main indexer;
    private String previousLine;
    private HashSet<String> filesToGet = new HashSet<>();

    private Thread zipperThread;
    BlockingQueue<String> filesToProcess = new LinkedBlockingDeque<>();
    AtomicBoolean threadExit = new AtomicBoolean(false);

    static Logger logger = LogManager.getLogger(RSyncReadProcessor.class);

    private IProcessOutputRead stdoutReader = new IProcessOutputRead() {
        @Override
        public void lineRead(String s) {
            if(StringUtils.isNotBlank(previousLine)){ // rsync prints to stdout name of file being transferred
                String fileName = FilenameUtils.getName(previousLine);
                if(StringUtils.isNotBlank(fileName) && filesToGet.contains(fileName)){
                    filesToProcess.add(fileName);
                }
            }
            previousLine = s;
        }
    };

    public RSyncReadProcessor(Path logsDir, Main indexer, ArrayList<OSFile> fileNames, boolean zipDest) {
        this.logsDir = logsDir;
        this.indexer = indexer;
        this.zipDest=zipDest;

        for (OSFile f: fileNames){
            filesToGet.add(FilenameUtils.getName( f.getFileName()));
        }

        zipperThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String fileInfo;
                while ((fileInfo = filesToProcess.poll()) != null || !threadExit.get()) {
                    if(fileInfo!=null) {
                        Path downloadedFile = Paths.get(logsDir.toString(), fileInfo);
                        ArrayList<Path > zipNames = new ArrayList<>();
                        if(Files.isReadable(downloadedFile)){
                            if(zipDest) {
                                FileUtils.zipFile(downloadedFile, destFile -> zipNames.add(destFile), s -> logger.info(s));
                                if (indexer != null) {
                                    for (Path f : zipNames) {
                                        indexer.processAddedFile(f);
                                    }
                                }
                            }
                            else {
                                indexer.processAddedFile(downloadedFile);
                            }
                        }
                        else {
                            logger.error("Cannot read file "+downloadedFile);
                        }
                    }
                    else {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                logger.info("done thread rsyncreader");
            }
        });
        zipperThread.start();
    }

    public IProcessOutputRead getOutputStringAction() {
        return stdoutReader;
    }

    public void finish() {
        threadExit.set(true);
        try {
            zipperThread.join(7200000);
        } catch (InterruptedException e) {
            logger.error("interrupted zipperThread");
        }
    }

    ;
}
