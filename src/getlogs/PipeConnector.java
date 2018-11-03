/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import java.io.*;
 
/**
 * Copy the output stream from one process in a pipeline to the input stream of the next process
 *<p>
 * This class is necessary to implement pipelines because ProcessBuilder only allows redirection 
 * to Files, not to InputStreams or OutputStreams, and Process returns its I/O streams as
 * InputStreams and OutputStreams, which cannot be connected, rather than PipedInputStreams and 
 * PipedOutputStreams.
 *
 *<p>
 * Start with:<br>
 *   Thread thread = new Thread (new PipeManager (input, output), "thread name");<br>
 *   thread.start();
 *
 * @author      Brian B. McGuinness
 * @version     1.0  2015-01-06
 */
public class PipeConnector implements Runnable {
  private InputStream  _process1Output = null;
  private OutputStream _process2Input  = null;
 
  /**
   * Initialize the PipeConnector
   *
   * @param  process1Output  The output stream from the first process (read as an InputStream)
   * @param  process2Input   The input stream to the second process (written as an OutputStream)
   */
  public PipeConnector (InputStream process1Output, OutputStream process2Input) {
    _process1Output = process1Output;
    _process2Input  = process2Input;
  }
 
  /**
   * Perform the copy operation in a separate thread
   */
  public void run () {
    int value;
 
    while (true) {
      try {
        value = _process1Output.read();
        if (value == -1) break;  // end of input stream
        _process2Input.write (value);
        _process2Input.flush();
      }
      catch (IOException error) {
//          GetLogs.logger.error("-1-", error);
        break;
      }
//           GetLogs.logger.info("-done-");
   }
 
    try {
      _process1Output.close();
    }
    catch (IOException error) {}
    try {
      _process2Input.close();
    }
    catch (IOException error) {}
  }
}