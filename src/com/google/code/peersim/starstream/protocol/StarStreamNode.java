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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.transport.Transport;
import peersim.util.IncrementalStats;

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
  private boolean playbackStarted;
  private int MIN_CONTIGUOUS_CHUNKS_IN_BUFFER;
  private long START_STREAMING_TIME;
  private int START_STREAMING_TIMEOUT;
//  private int WAIT_BETWEEN_FORCES;
//  private long lastForce;
//  private boolean aggressive;
  private long whenPlaybackStarted;
  private IncrementalStats perceivedChunkDeliveryTimes;
  private int advance;
  private int chunkPlaybackLength;
  private Set<Integer> issuedChunkRequests;
  private Set<Integer> deliveredChunks;
  private List<Integer> pendingChunkRequests;

  /**
   * Default PeerSim-required constructor.
   *
   * @param prefix The prefix for accessing configuration properties
   * @throws java.io.FileNotFoundException Raised whenever it is not possible creating the log file
   */
  public StarStreamNode(String prefix) throws FileNotFoundException {
    super(prefix);
    STAR_STREAM_PID = Configuration.getPid(prefix + SEPARATOR + STAR_STREAM);
    // at this point the protocl stack for the current node must have been already
    // initialized by the PeerSim runtime, so this is the right time for takeing
    // references to Pastry and *-Stream and tight the two instance enabling the
    // latter to receive notifications from the former
    MIN_CONTIGUOUS_CHUNKS_IN_BUFFER = Configuration.getInt(prefix + SEPARATOR + "minContiguousChunksInBuffer");
    START_STREAMING_TIME = Configuration.getLong(prefix + SEPARATOR + "startStreaming");
    START_STREAMING_TIMEOUT = (int) Math.ceil(Configuration.getDouble(prefix + SEPARATOR + "startStreamingTimeout"));
//    WAIT_BETWEEN_FORCES = Configuration.getInt(prefix+SEPARATOR+"waitBetweenForces");
//    aggressive = Configuration.getBoolean(prefix+SEPARATOR+"aggressive");
    advance = Configuration.getInt(prefix + SEPARATOR + "advance");
    chunkPlaybackLength = Configuration.getInt(prefix + SEPARATOR + "chunkPlaybackLength");
    init();
  }

  /**
   * Tells the *-Stream protocol to check for expired messages that were waiting
   * for acks/nacks and behave consequently.
   */
  public void checkForStarStreamTimeouts() {
    if (isJoined()) {
      getStarStreamProtocol().checkForTimeouts();
    }
  }

  public void checkForStartStreamingTimeout() {
    if (isJoined()) {
//      if(!playbackStarted) {
//        if(CommonState.getTime() > START_STREAMING_TIME + START_STREAMING_TIMEOUT) {
//          // driiin!!! timeout expired
//          // start proactive search (pull) for those chunks required to fill in
//          // the buffer
//          forceBufferFillIn();
//        }
//      } else {
//        scheduleNextChunkRequest();
//      }
      if (CommonState.getTime() > START_STREAMING_TIME + START_STREAMING_TIMEOUT) {
        processPendingChunkRequests();
        scheduleNextChunkRequest();
      }
    }
  }

  public int countMissingChunks() {
    return issuedChunkRequests.size();
  }

  public double getPerceivedAvgChunkDeliveryTime() {
    return perceivedChunkDeliveryTimes.getAverage();
  }

  public long getSentMessages() {
    return getStarStreamProtocol().getSentMessages();
  }

  private void processPendingChunkRequests() {
    UUID sessionId = StarStreamSource.getStarStreamSessionId();
    Integer[] pcrs = pendingChunkRequests.toArray(new Integer[pendingChunkRequests.size()]);
    pendingChunkRequests.clear();
    for (int pcr : pcrs) {
      foo(sessionId, pcr);
    }
  }

  private void scheduleNextChunkRequest() {
    long currentTime = CommonState.getTime();
    double avgObservedDeliveryTime = perceivedChunkDeliveryTimes.getAverage();
    double num = currentTime + advance + avgObservedDeliveryTime - whenPlaybackStarted;
    int nextChunkSeqId = Double.valueOf(Math.floor(num / chunkPlaybackLength)).intValue();
    if (StarStreamSource.isSeqIdLegal(nextChunkSeqId) && !hasBeenDelivered(nextChunkSeqId)) {
      // once the next seq-id has been computed we have to:
      // 1. store somewhere that the i-th chunk has been scheduled for search right now
      // 2. start searching for that chunk iff this is the very first time we scheduled it
      // NOTE: pro-actively searching for a chunk entails two steps:
      // 1. ask some neighbors if anyone of them actually has the chunk
      // 2. either in the event of nacks or in case of req-timeout expiration, issue
      //    a Pastry lookup for that chunk: the Pastry lookup already has its own
      //    configurable retries
      // Thus, according to the two observations above, it is not necessary implementing
      // at this level any kind of resubmission logic for chunk requests
      if (addToIssuedChunkRequests(nextChunkSeqId)) {
        UUID sessionId = StarStreamSource.getStarStreamSessionId();
        foo(sessionId, nextChunkSeqId);
      }
    }
  }

  private void foo(UUID sessionId, int nextChunkSeqId) {
    PastryId chunkId = ChunkUtils.getChunkIdForSequenceId(sessionId, nextChunkSeqId);
    if (chunkId != null) {
      log("[*-STREAM] node " + this.getPastryId() + " starts looking for chunk(" + nextChunkSeqId + ") " + chunkId);
      getStarStreamProtocol().searchForChunk(sessionId, chunkId);
    } else {
      pendingChunkRequests.add(nextChunkSeqId);
      log("[*-STREAM] WARN node " + this.getPastryId() + " No one knows anything about chunk " + nextChunkSeqId);
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
   * The PeerSim-assigned *-Stream protocol identifier.
   *
   * @return The PeerSim-assigned *-Stream protocol identifier.
   */
  public int getStarStreamPid() {
    return STAR_STREAM_PID;
  }

  public boolean hasStartedPlayback() {
    return playbackStarted;
  }

  /**
   * Returns a reference to the configure <i>unreliable</i> transport for the
   * *-Stream protocol.
   *
   * @return The unreliable transport
   */
  Transport getStarStreamTransport() {
    return (Transport) getProtocol(FastConfig.getTransport(STAR_STREAM_PID));
  }

  /**
   * Returns a reference to the *-Store owned by this node.
   *
   * @return The *-Store
   */
  StarStreamStore getStore() {
    return getStarStreamProtocol().getStore();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void notifyNewChunk(Chunk<?> chunk) {
    log("[*-STREAM] node " + this.getPastryId() + " has stored resource " + chunk);
    updateLocalStats(chunk);
    removeFromIssuedChunkRequests(chunk.getSequenceId());
    addToDeliveredChunks(chunk.getSequenceId());
    if (!playbackStarted) {
      checkIfPlaybackIsAllowed();
    }
  }

  /**
   * Tells the node to start processing potentially delayed messages.
   */
  public void processDelayedMessages() {
    if (isJoined()) {
      getStarStreamProtocol().processDelayedMessages();
    }
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
    return super.toString() + "\n" + getStore();
  }

  public long getWhenPlaybackStarted() {
    return whenPlaybackStarted;
  }

  /**
   * Returns a reference to this node's assigned {@link StarStreamProtocol} instance.
   * @return The {@link StarStreamProtocol} instance
   */
  protected StarStreamProtocol getStarStreamProtocol() {
    return (StarStreamProtocol) getProtocol(STAR_STREAM_PID);
  }

  private void addToDeliveredChunks(int sequenceId) {
    deliveredChunks.add(sequenceId);
  }

  private boolean addToIssuedChunkRequests(int nextChunkSeqId) {
    return issuedChunkRequests.add(nextChunkSeqId);
  }

  private void checkIfPlaybackIsAllowed() {
    int contiguousChunks = getStore().countContiguousChunksFromStart(StarStreamSource.getStarStreamSessionId());
    if (contiguousChunks >= MIN_CONTIGUOUS_CHUNKS_IN_BUFFER) {
      startPalyBack();
    }
  }

//  private List<PastryId> collectMissingChunkIds() {
//    List<Integer> seqIds = getStore().getMissingSequenceIds(StarStreamSource.getStarStreamSessionId());
//    if(seqIds.size()==0) {
//      if(aggressive) {
//        // we are really unlucky, the buffer is completely empty, but luckly we know
//        // we can search for chunks with seqIDs from 0 to whatever we think is appropriate
//        for(int i=0; i<MIN_CONTIGUOUS_CHUNKS_IN_BUFFER; i++) {
//          seqIds.add(i);
//        }
//      } else {
//        seqIds.add(ChunkUtils.getMinSeqNumber());
//      }
//    }
//    return ChunkUtils.getChunkIdsForSequenceIds(StarStreamSource.getStarStreamSessionId(), seqIds);
//  }
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
//  private void forceBufferFillIn() {
//    if(CommonState.getTime() > lastForce + WAIT_BETWEEN_FORCES) {
//      lastForce = CommonState.getTime();
//      List<PastryId> missingChunkIds = collectMissingChunkIds();
//      for(PastryId id : missingChunkIds) {
//        getStarStreamProtocol().searchForChunk(StarStreamSource.getStarStreamSessionId(), id);
//      }
//    }
//  }
  private boolean hasBeenDelivered(int seqId) {
    return deliveredChunks.contains(seqId);
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
    playbackStarted = false;
//    this.lastForce = 0;
    this.playbackStarted = false;
    this.whenPlaybackStarted = -1;
    this.perceivedChunkDeliveryTimes = new IncrementalStats();
    this.issuedChunkRequests = new LinkedHashSet<Integer>();
    this.deliveredChunks = new LinkedHashSet<Integer>();
    this.pendingChunkRequests = new LinkedList<Integer>();
  }

  private void removeFromIssuedChunkRequests(int sequenceId) {
    issuedChunkRequests.remove(sequenceId);
  }

  private void startPalyBack() {
    playbackStarted = true;
    whenPlaybackStarted = CommonState.getTime();
    log("[*-STREAM] node " + this.getPastryId() + " has started playback");
  }

  private void updateLocalStats(Chunk<?> chunk) {
    long deliveryTime = CommonState.getTime() - chunk.getTimeStamp();
    perceivedChunkDeliveryTimes.add(deliveryTime);
  }
}