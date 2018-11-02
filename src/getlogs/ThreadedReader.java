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

    public ThreadedReader(InputStream in) {
        this.stream = new BufferedReader(new InputStreamReader(in));
    }

    @Override
    public void run() {
        if (stream != null) {
            System.out.println(
                    "Here is the standard output of the command:\n");
            String s;
            try {
                while ((s = stream.readLine()) != null) {
                    GetLogs.doLog(s);
                }
            } catch (IOException ex) {
                Logger.getLogger(ThreadedReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
