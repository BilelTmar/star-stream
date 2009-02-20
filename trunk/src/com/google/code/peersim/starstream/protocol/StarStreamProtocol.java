/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

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
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
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
   * Constructor. Sets up only those configuration parameters that can be set
   * by means of the PeerSim configuration file.
   *
   * @param prefix The configuration prefix
   */
  public StarStreamProtocol(String prefix) throws FileNotFoundException {
    this.prefix = prefix;
    msgTimeout = Configuration.getInt(prefix+SEPARATOR+MSG_TIMEOUT);
    starStoreSize = Configuration.getInt(prefix+SEPARATOR+STAR_STORE_SIZE);
    reliableTransportPid = Configuration.getPid(prefix+SEPARATOR+REL_TRANSPORT);
    unreliableTransportPid = Configuration.getPid(prefix+SEPARATOR+UNREL_TRANSPORT);
    pastryTransportPid = Configuration.getPid(prefix+SEPARATOR+PASTRY_TRANSPORT);
    doLog = Configuration.getBoolean(prefix+SEPARATOR+DO_LOG);
    if(doLog) {
      stream = new PrintStream(new FileOutputStream(new FileNameGenerator(Configuration.getString(prefix + ".log"), ".log").nextCounterName()));
    }
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
   *
   * @param chunkMessage
   */
  private void handleChunk(ChunkMessage chunkMessage) {
    throw new UnsupportedOperationException("Not yet implemented");
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
   * Logs the given message appending a new-line to the input parameter.
   * @param msg The log message
   */
  private void log(String msg) {
    stream.print(CommonState.getTime()+") ["+pastryProtocol.getPastryId()+"] "+msg+"\n");
  }
}