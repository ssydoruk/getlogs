/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import static getlogs.GetLogs.logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 *
 * @author stepansydoruk
 */
public class ExtProcess {

    private ProcessBuilder pb;
    String cmd;

    Process proc = null;
    private boolean saveStdErr = false;
    private boolean saveStdOut = false;

    ExtProcess(ArrayList<String> tarParams) throws IOException {
        cmd = tarParams.get(0);
        pb = getProcessBuilder(tarParams);
        GetLogs.logger.debug("Working directory :" + pb.directory());

        proc = pb.start();

    }

    ExtProcess(ArrayList<String> tarParams, ExtProcess procSSH) throws IOException {
        this(tarParams);

        PipeConnector pc = new PipeConnector(procSSH.getInputStream(), proc.getOutputStream());
        pc.run();

    }

    ThreadedReader inputReader = null;
    ThreadedReader errReader = null;

    void readOutputs() {
        inputReader = new ThreadedReader((proc.getInputStream()), cmd, "in", saveStdOut);
        inputReader.run();

        errReader = new ThreadedReader((proc.getErrorStream()), cmd, "err", saveStdErr);
        errReader.run();

    }

    public ArrayList<String> getOutBuf() {
        if (inputReader != null) {
            return inputReader.getOutBuf();
        } else {
            return null;
        }
    }

    public ArrayList<String> getErrBuf() {
        if (errReader != null) {
            return errReader.getOutBuf();
        } else {
            return null;
        }
    }

    void start() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static ProcessBuilder getProcessBuilder(ArrayList<String> sshParameters) throws IOException {
        if (logger.isDebugEnabled()) {
            StringBuilder l = new StringBuilder();
            for (String sshParameter : sshParameters) {
                if (l.length() > 0) {
                    l.append(" ");
                }
                l.append(sshParameter);
            }
            logger.debug("Executing: [" + l + "]");
        }

        return new ProcessBuilder(sshParameters);

    }

    private InputStream getInputStream() {
        return proc.getInputStream();
    }

    int waitFor() throws InterruptedException {
        return proc.waitFor();
    }

    void readOutputs(boolean saveStdOut, boolean saveStdErr) {
        this.saveStdOut = saveStdOut;
        this.saveStdErr = saveStdErr;
        readOutputs();
    }

    ArrayList<String> getSTDOut() {
        if( inputReader!=null)
            return inputReader.getOutBuf();
        else
            return null;
    }

}
