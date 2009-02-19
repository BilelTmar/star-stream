/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

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
import peersim.config.FastConfig;
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
public class StarStreamProtocol implements EDProtocol {

  public static final String MSG_TIMEOUT = "timeOut";
  public static int msgTimeout;
  public static final String STAR_STORE_SIZE = "starStoreSize";
  public static int starStoreSize;
  public static final String REL_TRANSPORT = "reliableTransport";
  public static int reliableTransportPid;
  public static final String UNREL_TRANSPORT = "unreliableTransport";
  public static int unreliableTransportPid;
  public static final String PASTRY_TRANSPORT = "pastryTransport";
  public static int pastryTransportPid;
  public static final String LOG_FILE = "log";
  public static String logFile;
  public static final String DO_LOG = "doLog";
  public static boolean doLog;

  private static final String SEPARATOR = ".";

  private String prefix;

  private StarStreamNode thisNode;
  private PrintStream stream;

  /**
   *
   * @param prefix
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
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("Cloning failed. See nested exceptions, please.", e);
    }
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
   * 
   */
  private void registerPastryListeners() {
  }
}