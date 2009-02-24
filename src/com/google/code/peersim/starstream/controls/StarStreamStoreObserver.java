/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.starstream.protocol.StarStreamNode;
import com.google.code.peersim.starstream.protocol.StarStreamStore;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.FileNameGenerator;

/**
 * Observer class in charge of printing to file the state of each {@link StarStreamNode}
 * found in the {@link Network} at simulation completion.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamStoreObserver implements Control {

  /**
   * Whether to log or not.
   */
  public static final String DO_LOG = "doLog";
  /**
   * The file name to log to.
   */
  public static final String LOG_FILE = "log";
  private static final String SEPARATOR = ".";
  /**
   * Whether to log or not.
   */
  private boolean doLog;
  /**
   * The file name to log to.
   */
  private String logFile;
  /**
   * The stream to log to.
   */
  private PrintStream stream;

  /**
   * Constructor.
   *
   * @param prefix The prefix
   * @throws java.io.FileNotFoundException Throw iff the log file cannot be created
   */
  public StarStreamStoreObserver(String prefix) throws FileNotFoundException {
    super();
    doLog = Configuration.getBoolean(prefix + SEPARATOR + DO_LOG);
    if (doLog) {
      logFile = new FileNameGenerator(Configuration.getString(prefix + SEPARATOR + LOG_FILE), ".log").nextCounterName();
      stream = new PrintStream(new FileOutputStream(logFile));
    }
  }

  /**
   * Once the very last simulation cycle begins, this method collects information
   * related to each {@link StarStreamNode}'s {@link StarStreamStore} instance and
   * print it all to the configured log file.
   *
   * @return {@link Boolean#TRUE}
   */
  @Override
  public boolean execute() {
    boolean stop = false;
    if (doLog && (CommonState.getTime() == CommonState.getEndTime()-1)) {
      dump();
    }
    return stop;
  }

  /**
   * Dumps down to the log file.
   */
  private void dump() {
    System.err.print("Dumping *-Stream stores to file " + logFile + "... ");
    int dim = Network.size();
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      log(node.toString());
    }
    System.err.print("done!\n\n");
  }

  /**
   * Logging method.
   *
   * @param msg
   */
  private void log(String msg) {
    stream.println(msg);
  }
}