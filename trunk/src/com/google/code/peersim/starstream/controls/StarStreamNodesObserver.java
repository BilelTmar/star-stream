/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import com.google.code.peersim.starstream.protocol.StarStreamPlayer;
import com.google.code.peersim.starstream.protocol.StarStreamStore;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.FileNameGenerator;
import peersim.util.IncrementalStats;

/**
 * Observer class in charge of printing to file the state of each {@link StarStreamNode}
 * found in the {@link Network} at simulation completion.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamNodesObserver implements Control {

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
  public StarStreamNodesObserver(String prefix) throws FileNotFoundException {
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
    System.err.print("Dumping *-Stream stats to file " + logFile + "... ");
    // total chunks
    log("Total chunks: "+StarStreamSource.getTotalChunks());
    // nodes x chunk
    log("Nodes x chunk: "+StarStreamSource.getNodesPerChunk());
    // total nodes
    int dim = Network.size();
    log("Total nodes: "+dim);
    // active nodes
    int activeNodes = 0;
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      if(node.isUp())
        activeNodes++;
    }
    log("Active nodes: "+activeNodes);
    // playback started
    int nodesThatStartedPlayack = 0;
    List<PastryId> nodesThatDidNotStartedPlayback = new LinkedList<PastryId>();
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      if(node.hasStartedPlayback())
        nodesThatStartedPlayack++;
      else
        nodesThatDidNotStartedPlayback.add(node.getPastryId());
    }
    log("Started playbacks: "+nodesThatStartedPlayack);
    log("Not started playbacks node-ids: "+nodesThatDidNotStartedPlayback);
    // start-streaming time window
    long lastPlaybackStart = 0;
    long firstPlaybackStart = Long.MAX_VALUE;
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      long time = node.getWhenPlaybackStarted();
      if(time > lastPlaybackStart)
        lastPlaybackStart = time;
      if(time < firstPlaybackStart)
        firstPlaybackStart = time;
    }
    log("Playbacks time-window: "+(lastPlaybackStart-firstPlaybackStart));
    // missing chunks distribution
    Map<Integer,Integer> missingChunksDistribution = new HashMap<Integer, Integer>();
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      int missingChunks = node.countMissingChunks();
      Integer nodesCount = missingChunksDistribution.get(missingChunks);
      if(nodesCount==null) {
        missingChunksDistribution.put(missingChunks, 1);
      } else {
        missingChunksDistribution.put(missingChunks, ++nodesCount);
      }
    }
    log("Missing chunks distribution [missed-chunks/nodes]: "+missingChunksDistribution);
    missingChunksDistribution.clear();
    // stats of perceived chunk delivery times
    IncrementalStats stats = new IncrementalStats();
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      stats.add(node.getPerceivedAvgChunkDeliveryTime());
    }
    log("Perceived avg chunk delivery-time: "+stats.getAverage());
    log("Min of perceived avg chunk delivery-time: "+stats.getMin());
    log("Max of perceived avg chunk delivery-time: "+stats.getMax());
    log("Variance of perceived avg chunk delivery-time: "+stats.getVar());
    log("StD of perceived avg chunk delivery-time: "+stats.getStD());
    // avg sent messages per node
    stats.reset();
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      stats.add(node.getSentMessages());
    }
    stats.reset();
    log("Avg messages sent per node: "+stats.getAverage());
    log("Min messages sent per node: "+stats.getMin());
    log("Max messages sent per node: "+stats.getMax());
    log("Variance of messages sent per node: "+stats.getVar());
    log("StD of messages sent per node: "+stats.getStD());
    // players statistics
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      List<Integer> missed = node.getUnplayedChunks();
      stats.add(node.getPercentageOfUnplayedChunks());
      for(int id : missed) {
        Integer nodesCount = missingChunksDistribution.get(id);
        if(nodesCount==null) {
          missingChunksDistribution.put(id, 1);
        } else {
          missingChunksDistribution.put(id, ++nodesCount);
        }
      }
    }
    log("Avg % of not played chunks: "+stats.getAverage());
    log("Min % of not played chunks: "+stats.getMin());
    log("Max % of not played chunks: "+stats.getMax());
    log("Not played chunks [chunk-id/nodes]: "+missingChunksDistribution);
    missingChunksDistribution.clear();
    log("");
    // players detail
    for (int i = 0; i < dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      log(node.getPlayer().toString());
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