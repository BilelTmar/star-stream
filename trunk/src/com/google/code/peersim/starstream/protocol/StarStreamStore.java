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
   */
  void addChunk(Chunk<?> chunk) {
    Map<PastryId, Chunk<?>> chunks = store.get(chunk.getSessionId());
    if(chunks==null) {
      chunks = new HashMap<PastryId, Chunk<?>>();
      store.put(chunk.getSessionId(), chunks);
    }
    // store the resource iff it is not there yet
    if(!chunks.containsKey(chunk.getResourceId())) {
      chunks.put(chunk.getResourceId(), chunk);
    }
  }
}