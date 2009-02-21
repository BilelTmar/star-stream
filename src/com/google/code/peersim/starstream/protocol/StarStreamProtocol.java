/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.pastry.protocol.PastryJoinLsnrIfc.JoinedInfo;
import com.google.code.peersim.pastry.protocol.PastryProtocol;
import com.google.code.peersim.pastry.protocol.PastryProtocolListenerIfc;
import com.google.code.peersim.pastry.protocol.PastryResourceAssignLsnrIfc.ResourceAssignedInfo;
import com.google.code.peersim.pastry.protocol.PastryResourceDiscoveryLsnrIfc.ResourceDiscoveredInfo;
import com.google.code.peersim.starstream.protocol.messages.ChunkAdvertisement;
import com.google.code.peersim.starstream.protocol.messages.ChunkKo;
import com.google.code.peersim.starstream.protocol.messages.ChunkMessage;
import com.google.code.peersim.starstream.protocol.messages.ChunkMissing;
import com.google.code.peersim.starstream.protocol.messages.ChunkOk;
import com.google.code.peersim.starstream.protocol.messages.ChunkRequest;
import com.google.code.peersim.starstream.protocol.messages.StarStreamMessage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import peersim.util.FileNameGenerator;

/**
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamProtocol implements EDProtocol, PastryProtocolListenerIfc {

  /**
   * Configurable timeout for *-Stream messages.
   */
  public static final String MSG_TIMEOUT = "timeOut";
  /**
   * Timeout value for *-Stream messages.
   */
  private static int msgTimeout;
  /**
   * Configurable size for the *-Stream Store.
   */
  public static final String STAR_STORE_SIZE = "starStoreSize";
  /**
   * Size for the *-Stream Store.
   */
  private static int starStoreSize;
  /**
   * Whether messages should be corruptable or not.
   */
  public static final String CURRUPTED_MESSAGES = "curruptedMessages";
  /**
   * Whether messages should be corruptable or not.
   */
  private static boolean curruptedMessages;
  /**
   * Whether messages should be corruptable or not. Legal values are in [0..1].
   */
  public static final String CURRUPTED_MESSAGES_PROB = "curruptedMessagesProbability";
  /**
   * Whether messages should be corruptable or not.
   */
  private static float curruptedMessagesProbability;
  /**
   * How many simulation-time units a chunk can persist in the *-Stream Store before
   * it is removed to make room for other chunks.
   */
  public static final String CHUNK_EXPIRATION = "chunkExpiration";
  /**
   * How many simulation-time units a chunk can persist in the *-Stream Store before
   * it is removed to make room for other chunks.
   */
  private static int chunkExpiration;
  /**
   * Reliable transport protocol for *-Stream.
   */
  public static final String REL_TRANSPORT = "reliableTransport";
  /**
   * The protocol id assigned by the PeerSim runtime to the reliable transport instance.
   */
  private static int reliableTransportPid;
  /**
   * Unreliable transport protocol for *-Stream.
   */
  public static final String UNREL_TRANSPORT = "unreliableTransport";
  /**
   * The protocol id assigned by the PeerSim runtime to the unreliable transport instance.
   */
  private static int unreliableTransportPid;
  /**
   * Pastry protocol for *-Stream.
   */
  public static final String PASTRY_TRANSPORT = "pastryTransport";
  /**
   * The protocol id assigned by the PeerSim runtime to the Pastry protocol instance.
   */
  private static int pastryTransportPid;
  /**
   * Configurable file name for logging purposes.
   */
  public static final String LOG_FILE = "log";
  /**
   * Name of the configured log file (if any).
   */
  private static String logFile;
  /**
   * Property for configuring whether the protocol should log its activity or not.
   */
  public static final String DO_LOG = "doLog";
  /**
   * Whether the protocol should log its activity or not.
   */
  private static boolean doLog;
  /**
   * PeerSim property separator char.
   */
  private static final String SEPARATOR = ".";
  /**
   * Configuration prefix.
   */
  private static String prefix;

  /**
   * This reference to the node associated with the current protocol instance
   * is set-up whenevere a new event has to be processed and set back to {@code null}
   * upon event-handling completion.
   */
  private StarStreamNode thisNode;
  /**
   * The stream to log to.
   */
  private PrintStream stream;
  /**
   * The reference to the underlying {@link PastryProtocol} instance.
   */
  private PastryProtocol pastryProtocol;
  /**
   * This is the *-Stream local-store for storing chunks.
   */
  private StarStreamStore store;

  
  private static int outDeg;
  private static int inDeg;

  /**
   * Constructor. Sets up only those configuration parameters that can be set
   * by means of the PeerSim configuration file.
   *
   * @param prefix The configuration prefix
   */
  public StarStreamProtocol(String prefix) throws FileNotFoundException {
    StarStreamProtocol.prefix = prefix;
    msgTimeout = Configuration.getInt(prefix+SEPARATOR+MSG_TIMEOUT);
    starStoreSize = Configuration.getInt(prefix+SEPARATOR+STAR_STORE_SIZE);
    reliableTransportPid = Configuration.getPid(prefix+SEPARATOR+REL_TRANSPORT);
    unreliableTransportPid = Configuration.getPid(prefix+SEPARATOR+UNREL_TRANSPORT);
    pastryTransportPid = Configuration.getPid(prefix+SEPARATOR+PASTRY_TRANSPORT);
    doLog = Configuration.getBoolean(prefix+SEPARATOR+DO_LOG);
    if(doLog) {
      stream = new PrintStream(new FileOutputStream(new FileNameGenerator(Configuration.getString(prefix + ".log"), ".log").nextCounterName()));
    }
    curruptedMessages = Configuration.getBoolean(prefix+SEPARATOR+CURRUPTED_MESSAGES);
    if(curruptedMessages) {
      curruptedMessagesProbability = (float)Configuration.getDouble(prefix+SEPARATOR+CURRUPTED_MESSAGES_PROB);
    }
    chunkExpiration = Configuration.getInt(prefix+SEPARATOR+CHUNK_EXPIRATION);
    outDeg = Configuration.getInt(prefix+SEPARATOR+"outDeg");
    inDeg = Configuration.getInt(prefix+SEPARATOR+"inDeg");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object clone() {
    try {
      Object clone = super.clone();
      ((StarStreamProtocol)clone).thisNode = null;
      ((StarStreamProtocol)clone).pastryProtocol = null;
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("Cloning failed. See nested exceptions, please.", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joined(JoinedInfo info) {
    log("Received pastry-event "+info);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resourceAssigned(ResourceAssignedInfo info) {
    log("Received pastry-event "+info);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resourceReceived(ResourceReceivedInfo info) {
    log("Received pastry-event "+info);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resourceRouted(ResourceRoutedInfo info) {
    log("Received pastry-event "+info);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resourceDiscovered(ResourceDiscoveredInfo info) {
    log("Received pastry-event "+info);
  }

  /**
   * 
   * @param localNode
   * @param thisProtocolId
   * @param event
   */
  @Override
  public void processEvent(Node localNode, int thisProtocolId, Object event) {
    thisNode = (StarStreamNode) localNode;
    // event-handling logic begins
    if(event instanceof StarStreamMessage) {
      // this is a known event, let's process it
      StarStreamMessage msg = (StarStreamMessage)event;
      switch(msg.getType()) {
        case CHUNK : {
          handleChunk((ChunkMessage)msg);
          break;
        }
        case CHUNK_OK : {
          handleChunkOk((ChunkOk)msg);
          break;
        }
        case CHUNK_KO : {
          handleChunkKo((ChunkKo)msg);
          break;
        }
        case CHUNK_ADV : {
          handleChunkAdvertisement((ChunkAdvertisement)msg);
          break;
        }
        case CHUNK_REQ : {
          handleChunkRequest((ChunkRequest)msg);
          break;
        }
        case CHUNK_MISSING : {
          handleChunkMissing((ChunkMissing)msg);
          break;
        }
        default: {
          throw new IllegalStateException("A message of type "+msg.getType()+" has been received, but I do not know how to handle it.");
        }
      }
    } else {
      // an unknown event has been received
      throw new IllegalStateException("An event of type "+event.getClass()+" has been received, but I do not know how to handle it.");
    }
    // event-handling logic ends
    thisNode = null;
  }

  /**
   * This method has to be invoked only once by the {@link StarStreamNode} instance that owns this
   * {@link StarStreamProtocol} instance to let the latter register itself as a listener for
   * Pastry protocol-events.
   *
   * @param pastry The {@link PastryProtocol} to register on for event notifications
   */
  void registerPastryListeners(PastryProtocol pastry) {
    pastryProtocol = pastry;
    pastryProtocol.registerListener(this);
  }

  /**
   * Advertises the existence of a new chunk to a selection of neighbors.
   * The number of neighbors is based on how many ougoing connections this
   * node is able to create.
   *
   * @param msg The message containing the chunk that has to be advertised
   */
  private void advertiseChunk(ChunkMessage msg) {
    Set<StarStreamNode> neighbors = selectOutNeighbors();
    List<ChunkAdvertisement> advs = msg.createChunkAdvs(neighbors);
    broadcastOverReliableTransport(advs);
  }

  /**
   * Tells how many output connections can be established before
   * exhausting the output bandwidth.
   *
   * @return The number of connections
   */
  private int availableOutDeg() {
    return outDeg;
  }

  /**
   * Sends each message stored in the provided input over the configured reliable
   * transport.
   * 
   * @param msgs The messages
   */
  private void broadcastOverReliableTransport(List<? extends StarStreamMessage> msgs) {
    for(StarStreamMessage msg : msgs) {
      sendOverReliableTransport(msg);
    }
  }

  /**
   * Tells whether the message can be consumed or not. This is state basing on
   * the {@link StarStreamProtocol#curruptedMessagesProbability} configured value.
   *
   * @param chunkMsg
   * @return Whether the message can be consumed or not
   */
  private boolean checkMessageIntegrity(ChunkMessage chunkMsg) {
    boolean res;
    if(StarStreamProtocol.curruptedMessages) {
      res = CommonState.r.nextFloat() < StarStreamProtocol.curruptedMessagesProbability;
    } else {
      res = true;
    }
    return res;
  }

  /**
   * When a new chunk is received, the receiving protocol instance has to do two
   * things "concurrently":
   * <ol>
   * <li>immediately reply the sending node with either a {@link ChunkOk} or
   * {@link ChunkKo} message (the integrity of the message can be verified by means
   * of the {@link StarStreamProtocol#checkMessageIntegrity(ChunkMessage)};<br><br>
   * <b>NOTE:</b> in case of corrupted message, we must abort the processing<br><br>
   * ask itself if it already owns the chunk; should it not be the case, store the 
   * chunk locally. In either case, it must now adverties the received chunk to
   * a set of <i>outDeg</i> neighbors. <i>outDeg</i> means the number of outgoing
   * connections the node can currently establish. How many outgoing connections
   * can be used at the moment is defined by the 
   * {@link StarStreamProtocol#availableOutDeg(com.google.code.peersim.starstream.protocol.StarStreamProtocol.NetworkOperation)}
   * method</li>
   * <li>iff the sender is the <i>source-node</i>, route the chunk over the Pastry network</li>
   * </ol>
   *
   * @param chunkMessage The message
   */
  private void handleChunk(ChunkMessage chunkMessage) {
    log(thisNode+" received "+chunkMessage);
    if(checkMessageIntegrity(chunkMessage)) {
      // send OK and proceede
      handleChunk_SendOK(chunkMessage);
      storeIfNotStored(chunkMessage.getChunk());
      // advertise the new chunk
      advertiseChunk(chunkMessage);
      // route the resource over the Pastry network
      thisNode.publishResource(chunkMessage.getChunk());
    } else {
      // send KO and abort
      handleChunk_SendKO(chunkMessage);
    }
  }

  /**
   *
   * @param chunkAdvertisement
   */
  private void handleChunkAdvertisement(ChunkAdvertisement chunkAdvertisement) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   *
   * @param chunkKo
   */
  private void handleChunkKo(ChunkKo chunkKo) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   *
   * @param chunkMissing
   */
  private void handleChunkMissing(ChunkMissing chunkMissing) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   *
   * @param chunkOk
   */
  private void handleChunkOk(ChunkOk chunkOk) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * 
   * @param chunkRequest
   */
  private void handleChunkRequest(ChunkRequest chunkRequest) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Reply to the node that just sent the input {@link ChunkMessage} with a
   * {@link ChunkKo} message stating that the chunk has not been properly received.
   * This message is sent over a reliable transport.
   *
   * @param chunkMessage The message to reply to
   */
  private void handleChunk_SendKO(ChunkMessage chunkMessage) {
    ChunkKo ko = chunkMessage.replyKo();
    sendOverReliableTransport(ko);
  }

  /**
   * Reply to the node that just sent the input {@link ChunkMessage} with a
   * {@link ChunkOk} message stating that the chunk has been properly received.
   * This message is sent over a reliable transport.
   *
   * @param chunkMessage The message to reply to
   */
  private void handleChunk_SendOK(ChunkMessage chunkMessage) {
    ChunkOk ok = chunkMessage.replyOk();
    sendOverReliableTransport(ok);
  }

  /**
   * Logs the given message appending a new-line to the input parameter.
   * @param msg The log message
   */
  private void log(String msg) {
    stream.print(CommonState.getTime()+") ["+pastryProtocol.getPastryId()+"] "+msg+"\n");
  }

  /**
   * Randomly selects neighbors according to Pastry's <i>neighbor</i> definition.
   * @return Zero or more Pastry neighbors
   */
  private Set<StarStreamNode> selectOutNeighbors() {
    return thisNode.getPastryProtocol().getNeighbors(availableOutDeg());
  }

  /**
   * Send the input {@link StarStreamMessage} over the configured reliable transport.
   *
   * @param msg The message
   */
  private void sendOverReliableTransport(StarStreamMessage msg) {
    Transport t = (Transport) thisNode.getProtocol(FastConfig.getTransport(StarStreamProtocol.reliableTransportPid));
    t.send(msg.getSource(), msg.getDestination(), msg, StarStreamProtocol.reliableTransportPid);
    log("[SND] "+msg);
  }

  /**
   * Adds the given chunk to the local *-Stream Store iff that chunk is not yet
   * in the store.
   * 
   * @param chunk The chunk
   */
  private void storeIfNotStored(Chunk<?> chunk) {
    if(store==null)
      store = new StarStreamStore();
    store.addChunk(chunk);
  }
}