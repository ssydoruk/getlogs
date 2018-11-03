/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stepansydoruk
 */
public class ThreadedReader implements Runnable {

    private final BufferedReader stream; // no need to buffer it
    private final String cmd;
    private final String streamName;

    public ThreadedReader(InputStream in, String cmd, String stream) {
        this.stream = new BufferedReader(new InputStreamReader(in));
        this.cmd=cmd;
        this.streamName=stream;
        GetLogs.logger.debug("started reader for cmd: "+cmd+" stream:"+streamName);
    }

    @Override
    public void run() {
        if (stream != null) {
            String s;
            try {
                while ((s = stream.readLine()) != null) {
                    GetLogs.doLog(cmd+"_"+streamName+": "+s);
                }
            } catch (IOException ex) {
//                Logger.getLogger(ThreadedReader.class.getName()).log(Level.SEVERE, null, ex);
            }
            GetLogs.logger.debug(cmd+"_"+streamName+": exited");
        }
    }

}
