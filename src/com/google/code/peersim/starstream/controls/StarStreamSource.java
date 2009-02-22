/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.protocol.Chunk;
import com.google.code.peersim.starstream.protocol.ChunkUtils;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import com.google.code.peersim.starstream.protocol.StarStreamProtocol;
import com.google.code.peersim.starstream.protocol.messages.ChunkKo;
import com.google.code.peersim.starstream.protocol.messages.ChunkMessage;
import com.google.code.peersim.starstream.protocol.messages.ChunkOk;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
import peersim.util.FileNameGenerator;

/**
 * This control class represents the source of the *-Stream network.
 * It is in charge of sending out to randomly choosen *-Stream nodes streaming
 * chunks. The rate at which the streaming takes place is configurable in terms
 * of:
 * <ol>
 * <li>how many distinct chunks must be generated and spread per simulated-time unit</li>
 * <li>how many distinct nodes should be (at most) selected to receive the same chunk</li>
 * </ol>
 * The total amount of produced chunks is also configurable.<br>
 * The source starts streaming at a configurable point in time and stops when all the
 * chunks have been sent out.<br>
 * If a sent chunk is not acknowledged within the configured
 * timeout by all the nodes it was sent to, it is sent again to a number of nodes equal
 * to the number of nodes that did not sent their ack.<br>
 * If nack is received from node <i>n</i> for chunk <i>c</i>, that chunk is sent again
 * to that node.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamSource implements Control {

  private static final UUID SESSION_ID = UUID.randomUUID();
  public static final String CHUNKS_PER_TIME_UNIT = "chunksPerTimeUnit";
  private static int chunksPerTimeUnit;
  public static final String NODES_PER_CHUNK = "nodesPerChunk";
  private static int nodesPerChunk;
  public static final String CHUNKS = "chunks";
  private static int chunks;
  public static final String START_TIME = "start";
  private static long start;
  public static final String CHUNK_ACK_TIMEOUT = "ackTimeout";
  private static int ackTimeout;
  /**
   * The log file to log to.
   */
  public static final String LOG_FILE = "log";
  private static String logFile;
  /**
   * Whether to log or not.
   */
  public static final String DO_LOG = "doLog";
  private static boolean doLog;
  /**
   * The stream to the log file.
   */
  private static PrintStream stream;
  /**
   * Whether this control class is active or not.
   */
  private static boolean enabled = true;
  private static int createdChunksCounter = 0;
  /**
   * Fake source-node for sending messages to other nodes.
   */
  private static final StarStreamNode SOURCE_ADDR = null;
  /**
   * Memory of all the already sent chunks.
   */
  private static Map<PastryId,SentChunkDescriptor> sentChunks =  new HashMap<PastryId,SentChunkDescriptor>();

  

  /**
   * Used by {@link StarStreamProtocol}s to signal that a chunk could have not
   * been received.
   *
   * @param ko The KO message
   */
  public static void chunkKo(ChunkKo ko) {
    SentChunkDescriptor scd = sentChunks.get(ko.getChunkId());
    if(scd!=null) {
      scd.receivedNacks++;
    } else {
      // we have received a NACK for a chunk that looks like has not been sent
      // by the source or, at least, has not been saved in the sentChunks data
      // structure: this is a really bad thing!
      throw new IllegalStateException("BAD BAD THING: received a "+ko+" message for a chunk the source does not remember anything about!");
    }
  }

  /**
   * Used by {@link StarStreamProtocol}s to signal that a chunk has
   * been received and processed.
   * 
   * @param ko The OK message
   */
  public static void chunkOk(ChunkOk ok) {
    SentChunkDescriptor scd = sentChunks.get(ok.getChunkId());
    if(scd!=null) {
      scd.receivedNacks++;
    } else {
      // we have received an ACK for a chunk that looks like has not been sent
      // by the source or, at least, has not been saved in the sentChunks data
      // structure: this is a really bad thing!
      throw new IllegalStateException("BAD BAD THING: received a "+ok+" message for a chunk the source does not remember anything about!");
    }
  }

  /**
   * Enables the component.
   */
  public static void enable() {
    enabled = true;
  }

  /**
   * Disables the component.
   */
  public static void disable() {
    enabled = false;
  }

  /**
   * Constructor.
   *
   * @param prefix PeerSim prefix
   */
  public StarStreamSource(String prefix) throws FileNotFoundException {
    super();
    chunksPerTimeUnit = Configuration.getInt(prefix+"."+CHUNKS_PER_TIME_UNIT);
    nodesPerChunk = Configuration.getInt(prefix+"."+NODES_PER_CHUNK);
    chunks = Configuration.getInt(prefix+"."+CHUNKS);
    start = Configuration.getLong(prefix+"."+START_TIME);
    ackTimeout = Configuration.getInt(prefix+"."+CHUNK_ACK_TIMEOUT);
    doLog = Configuration.getBoolean(prefix+"."+DO_LOG);
    if(doLog) {
      logFile = new FileNameGenerator(Configuration.getString(prefix + "."+LOG_FILE), ".log").nextCounterName();
      stream = new PrintStream(new FileOutputStream(logFile));
    }
  }

  /**
   * This method, is the {@link StarStreamSource} is enabled and the current simulated-time
   * is greater or equal to {@link StarStreamSource#start}, does what follows:
   * <ol>
   * <li>produces {@link StarStreamSource#chunks} chunks</li>
   * <li>send each one of the chunks above to at most {@link StarStreamSource#nodesPerChunk} nodes</li>
   * </ol>
   *
   * {@inheritDoc}
   */
  @Override
  public boolean execute() {
    boolean stop = false;
    if(enabled && CommonState.getTime()>=start && createdChunksCounter<chunks) {
      Set<Chunk<?>> batch = produceChunks(SESSION_ID, chunksPerTimeUnit);
      spreadChunks(batch,nodesPerChunk);
    }
    return stop;
  }

  private void log(String msg) {
    stream.println(CommonState.getTime()+") "+msg);
  }

  /**
   * Produces at most {@code n} chunks for the given session identifier.
   *
   * @param sessionId The session id
   * @param n How many chunks must be produced
   * @return The chunks
   */
  private Set<Chunk<?>> produceChunks(UUID sessionId, int n) {
    Set<Chunk<?>> batch = new HashSet<Chunk<?>>();
    for(int i=0; i<n; i++) {
      if(createdChunksCounter<chunks) {
        Chunk<String> chunk = ChunkUtils.<String>createChunk(String.valueOf(createdChunksCounter),sessionId,createdChunksCounter++);
        batch.add(chunk);
      }
    }
    return batch;
  }

  /**
   * Selects <i>at most</i> {@code n} nodes and broadcasts each {@link Chunk}
   * stored in {@code batch} to each selected node.
   * 
   * @param batch The chunks
   * @param n How many nodes must receive each chunk
   */
  private void spreadChunks(Set<Chunk<?>> batch, int n) {
    for(Chunk<?> chunk : batch) {
      Set<StarStreamNode> nodes = selectNodes(n);
      broadcast(chunk, nodes);
    }
  }

  /**
   * Randomly selects up to {@code n} nodes from the network. There is no
   * guarantee that they are all active.
   *
   * @param n How many nodes should be selected (at most)
   * @return The selected nodes
   */
  private Set<StarStreamNode> selectNodes(int n) {
    Set<StarStreamNode> nodes = new HashSet<StarStreamNode>();
    for(int i=0; i<n; i++) {
      int tries = 0;
      StarStreamNode node = null;
      do {
        node = randomJoinedNode();
        // TODO: make the next % configurable?
      } while(!node.isJoined() && tries<(10*Network.size()/100));
      nodes.add(node);
    }
    return nodes;
  }

  /**
   * Broadcasts the given {@link Chunk} to each node in {@code nodes}.
   *
   * @param chunk The chunk
   * @param nodes The nodes
   */
  private void broadcast(Chunk<?> chunk, Set<StarStreamNode> nodes) {
    for(StarStreamNode node : nodes) {
      ChunkMessage msg = new ChunkMessage(SOURCE_ADDR, node, chunk);
      send(msg, node);
    }
    SentChunkDescriptor scd = new SentChunkDescriptor(chunk, nodes.size(), CommonState.getTime());
    sentChunks.put(scd.chunk.getResourceId(), scd);
  }

  /**
   * Sends the given message to the specified node, using the unreliable transport
   * associated with the *-Stream protocol.
   * 
   * @param msg The message
   * @param node The node
   */
  private void send(ChunkMessage msg, StarStreamNode node) {
    EDSimulator.add(0, msg, node, node.getStarStreamPid());
    log("[SND] "+msg);
  }

  /**
   * Randomly selects a {@link StarStreamNode} in the network.
   * @return A node
   */
  private StarStreamNode randomJoinedNode() {
    return (StarStreamNode) Network.get( CommonState.r.nextInt(Network.size()) );
  }

  private static class SentChunkDescriptor {
    private final Chunk<?> chunk;
    private final int nodes;
    private final long timestamp;
    private int receivedAcks = 0;
    private int receivedNacks = 0;

    private SentChunkDescriptor(Chunk<?> chunk, int nodes, long timestamp) {
      this.chunk = chunk;
      this.nodes = nodes;
      this.timestamp = timestamp;
    }
  }
}