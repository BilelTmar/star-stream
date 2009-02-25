/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Instances of this class are used by {@link StarStreamProtocol} instances to
 * store chunks exchanged with other nodes that must be made available to any
 * higher layer, for playback i.e.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamStore {

  /**
   * Internal representation of the store.
   */
  private Map<UUID, Map<PastryId,Chunk<?>>> store;

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    //sb.append("JvmStoreId: "+super.toString()+"\n");
    for(Map.Entry<UUID,Map<PastryId,Chunk<?>>> entry : store.entrySet()) {
      UUID sid = entry.getKey();
      sb.append("SessionId: "+sid+"\n");
      Map<PastryId,Chunk<?>> chunks = entry.getValue();
      sb.append("Size: "+chunks.size()+"\n");
      int i = 0;
      for(Map.Entry<PastryId,Chunk<?>> chunk : chunks.entrySet()) {
        sb.append((i++)+") "+chunk.getValue()+"\n");
      }
    }
    return sb.toString();
  }

  /**
   * Constructor.
   */
  StarStreamStore() {
    store = new HashMap<UUID, Map<PastryId, Chunk<?>>>();
  }

  /**
   * Stores the given chunk iff it not already in the store.
   * @param chunk The chunk to be added
   * @return Whether the chunk has been added or not
   */
  boolean addChunk(Chunk<?> chunk) {
    boolean added = false;
    Map<PastryId, Chunk<?>> chunks = store.get(chunk.getSessionId());
    if(chunks==null) {
      chunks = new HashMap<PastryId, Chunk<?>>();
      store.put(chunk.getSessionId(), chunks);
    }
    // store the resource iff it is not there yet
    if(!chunks.containsKey(chunk.getResourceId())) {
      chunks.put(chunk.getResourceId(), chunk);
      added = true;
    }
    return added;
  }

  /**
   * Returns the chunk stored with the specified identifiers, or {@code null}
   * if it is not available.
   *
   * @param sessionId The *-Stream session id
   * @param chunkId The *-Stream chunk id
   * @return The requested chunk or {@code null} if it is not available
   */
  Chunk<?> getChunk(UUID sessionId, PastryId chunkId) {
    Chunk<?> chunk = null;
    Map<PastryId, Chunk<?>> chunks = store.get(sessionId);
    if(chunks!=null) {
      chunk = chunks.get(chunkId);
    }
    return chunk;
  }

  /**
   * Tells whether the chunk uniquely identified by the provided *-Stream session
   * identifier and *-Stream chunk identifier (a {@link PastryId} actually) is
   * already in the store or not.
   *
   * @param sessionId The *-Stream session id
   * @param chunkId The *-Stream chunk id
   * @return Whether the chunk is stored or not
   */
  boolean isStored(UUID sessionId, PastryId chunkId) {
    return getChunk(sessionId, chunkId)!=null;
  }
}