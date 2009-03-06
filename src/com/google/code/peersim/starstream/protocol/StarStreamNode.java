/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.pastry.protocol.PastryNode;
import com.google.code.peersim.pastry.protocol.PastryProtocol;
import com.google.code.peersim.starstream.controls.ChunkUtils;
import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;
import com.google.code.peersim.starstream.controls.StarStreamSource;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.transport.Transport;

/**
 * This class represents a node that leverages both the *-Stream and the Pastry
 * protocols to take part to a P2P streaming event.<br>
 * Extending {@link PastryNode} this class can take advantage of all the features
 * and control classes available for {@link PastryNode}.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamNode extends PastryNode implements StarStreamProtocolListenerIfc {

  /**
   * Configuration parameter key that ties a *-Stream node to its *-Stream protocol.
   */
  public static final String STAR_STREAM = "starstream";
  /**
   * The protocol identifier assigned to the *-Stream protocol by the PeerSim runtime.
   */
  private int STAR_STREAM_PID;
  /**
   * Separator char used for PeersSim-related configuration properties.
   */
  private static final String SEPARATOR = ".";
  
  /**
   * Flag that tells if enough chunks have been collected for the playback to start.
   */
  private boolean canStartPlayback;

  private StarStreamBuffer buffer;
  private int MIN_CONTIGUOUS_CHUNKS_IN_BUFFER;
  private long START_STREAMING_TIME;
  private int START_STREAMING_TIMEOUT;

  /**
   * Default PeerSim-required constructor.
   *
   * @param prefix The prefix for accessing configuration properties
   * @throws java.io.FileNotFoundException Raised whenever it is not possible creating the log file
   */
  public StarStreamNode(String prefix) throws FileNotFoundException {
    super(prefix);
    STAR_STREAM_PID = Configuration.getPid(prefix+SEPARATOR+STAR_STREAM);
    // at this point the protocl stack for the current node must have been already
    // initialized by the PeerSim runtime, so this is the right time for takeing
    // references to Pastry and *-Stream and tight the two instance enabling the
    // latter to receive notifications from the former
    MIN_CONTIGUOUS_CHUNKS_IN_BUFFER = Configuration.getInt(prefix+SEPARATOR+"minContiguousChunksInBuffer");
    START_STREAMING_TIME = Configuration.getLong(prefix+SEPARATOR+"streamingStart");
    START_STREAMING_TIMEOUT = Configuration.getInt(prefix+SEPARATOR+"streamingStartTimeout");
    init();
  }

  /**
   * Tells the *-Stream protocol to check for expired messages that were waiting
   * for acks/nacks and behave consequently.
   */
  public void checkForStarStreamTimeouts() {
    if(isUp())
      getStarStreamProtocol().checkForTimeouts();
  }

  public void checkForStartStreamingTimeout() {
    if(isUp() && !canStartPlayback) {
      if(CommonState.getTime()-START_STREAMING_TIME > START_STREAMING_TIMEOUT) {
        // driiin!!! timeout expired
        // start proactive search (pull) for those chunks required to fill in
        // the buffer
//        forceBufferFillIn();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object clone() {
    StarStreamNode clone = (StarStreamNode) super.clone();
    clone.init();
    return clone;
  }

  /**
   * Returns a reference to the configure <i>unreliable</i> transport for the
   * *-Stream protocol.
   *
   * @return The unreliable transport
   */
  public Transport getStarStreamTransport() {
    return (Transport) getProtocol(FastConfig.getTransport(STAR_STREAM_PID));
  }

  /**
   * The PeerSim-assigned *-Stream protocol identifier.
   *
   * @return The PeerSim-assigned *-Stream protocol identifier.
   */
  public int getStarStreamPid() {
    return STAR_STREAM_PID;
  }

  /**
   * Returns a reference to the *-Store owned by this node.
   *
   * @return The *-Store
   */
  public StarStreamStore getStore() {
    return getStarStreamProtocol().getStore();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void notifyNewChunk(Chunk<?> chunk) {
    log("[*-STREAM] node "+this.getPastryId()+" has stored resource "+chunk);
    bufferChunk(chunk);
  }

  /**
   * Tells the node to start processing potentially delayed messages.
   */
  public void processDelayedMessages() {
    if(isUp())
      getStarStreamProtocol().processDelayedMessages();
  }

  /**
   * Tells the associated {@link StarStreamProtocol} instance that there has been
   * a new simulated-time tick and that both the outbound and inbound bandwiths
   * can be reset to their original levels.
   */
  public void resetUsedBandwidth() {
    getStarStreamProtocol().resetUsedBandwidth();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return super.toString()+"\n"+getStore();
  }

  /**
   * Returns a reference to this node's assigned {@link StarStreamProtocol} instance.
   * @return The {@link StarStreamProtocol} instance
   */
  protected StarStreamProtocol getStarStreamProtocol() {
    return (StarStreamProtocol) getProtocol(STAR_STREAM_PID);
  }

  private void bufferChunk(Chunk<?> chunk) {
    // add the chunk to the buffer
    buffer.add(chunk);
    // if the playback has not been started yet...
    if(!canStartPlayback) {
      // ... check if it is possible to
      checkIfPlaybackIsAllowed();
    }
  }

  private void checkIfPlaybackIsAllowed() {
    if(buffer.countContiguousChunks()==MIN_CONTIGUOUS_CHUNKS_IN_BUFFER) {
      startPalyBack();
    }
  }

  private List<PastryId> collectMissingChunkIds() {
    List<Integer> seqIds = buffer.getMissingSequenceIds();
    if(seqIds.size()==0) {
      // we are really unlucky, the buffer is completely empty, but luckly we know
      // we can search for chunks with seqIDs from 0 to whatever we think is appropriate
      for(int i=0; i<MIN_CONTIGUOUS_CHUNKS_IN_BUFFER; i++) {
        seqIds.add(i);
      }
    }
    return ChunkUtils.getChunkIdsForSequenceIds(StarStreamSource.getStarStreamSessionId(), seqIds);
  }

  /**
   * This methods tries to fill in the first
   * {@link StarStreamNode#MIN_CONTIGUOUS_CHUNKS_IN_BUFFER} buffer positions with
   * as many contiguous chunks, to enable to upper application layer start streaming
   * the content.<br>
   * This is accomplished in two steps:
   * <ol>
   * <li>discover which chunks are missing at the moment</li>
   * <li>ask the *-Stream protocol instance to start looking for those chunks</li>
   * </ol>
   */
  private void forceBufferFillIn() {
    List<PastryId> missingChunkIds = collectMissingChunkIds();
    for(PastryId id : missingChunkIds) {
      getStarStreamProtocol().searchForChunk(StarStreamSource.getStarStreamSessionId(), id);
    }
  }

  /**
   * This method has to be invoked both at construction and cloning-time to let
   * the {@link StarStreamProtocol} instance register over the {@link PastryProtocol}
   * instance for Pastry-related events.
   */
  private void init() {
    getStarStreamProtocol().setOwner(this);
    getStarStreamProtocol().registerStarStreamListener(this);
    PastryProtocol pastry = getPastryProtocol();
    getStarStreamProtocol().registerPastryListeners(pastry);
    canStartPlayback = false;
    buffer = new StarStreamBuffer();
  }

  private void startPalyBack() {
    canStartPlayback=true;
    // TODO start application
  }

  /**
   * @author frusso
   * @version 0.1
   * @since 0.1
   */
  static class StarStreamBuffer implements Iterable<Chunk<?>> {

    private Set<Chunk<?>> buffer;

    private StarStreamBuffer() {
      buffer = new TreeSet<Chunk<?>>();
    }

    private void add(Chunk<?> chunk) {
      buffer.add(chunk);
    }

    public int countContiguousChunks() {
      boolean firstRound = true;
      int firstSeqId = -1;
      int counter = 1;

      for(Chunk<?> c : buffer) {
        if(firstRound) {
          firstRound = false;
          firstSeqId = c.getSequenceId();
        } else {
          int seqId = c.getSequenceId();
          if(seqId == firstSeqId+1) {
            counter++;
          } else {
            break;
          }
        }
      }

      return counter;
    }

    @Override
    public Iterator<Chunk<?>> iterator() {
      return buffer.iterator();
    }

    private List<Integer> getMissingSequenceIds() {
      List<Integer> ids = new ArrayList<Integer>();
      boolean firstRound = true;
      int head = -1;

      for(Chunk<?> c : buffer) {
        if(firstRound) {
          firstRound = false;
          head = c.getSequenceId();
        } else {
          int seqId = c.getSequenceId();
          for(int i=head+1; i<seqId; i++) {
            ids.add(i);
          }
        }
      }

      return ids;
    }
  }
}