package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkUtils {

  private static Map<UUID, Map<Integer, PastryId>> chunkIds = new HashMap<UUID, Map<Integer, PastryId>>();

  /**
   * Factory method.
   * 
   * @param <T> The actual payload type
   * @param data The chunk payload
   * @param sid The *-Stream unique session ID
   * @param seqNumber The chunk sequence number
   * @return The new chunk
   */
  public static <T> Chunk<T> createChunk(T data, UUID sid, int seqNumber) {
    Chunk<T> chunk = new Chunk<T>(data, sid, seqNumber);
    storeNewChunkIdentity(chunk);
    return chunk;
  }

  /**
   * Returns the unique identifier of the produced chunk belonging to session
   * {@code sid} and with seuqence number equal to {@code seqNumber+1}, if it
   * exists, {@code null} otherwise.
   *
   * @param sid The *-Stream session identifier
   * @param seqNumber The last seen sequence number
   * @return The very next chunk unique ID, or {@code null}
   */
  public static PastryId nextChunkId(UUID sid, int seqNumber) {
    PastryId res = null;
    Map<Integer, PastryId> ids = chunkIds.get(sid);
    if(ids!=null) {
      res = ids.get(seqNumber+1);
    } else {
      // no entry for the give sid
      // NOP
    }
    return res;
  }

  /**
   * Stores the {@link PastryId} associated with the given chunk in the internal
   * memory.
   *
   * @param chunk The new chunk whose identity has to be memorized
   */
  private static void storeNewChunkIdentity(Chunk<?> chunk) {
    Map<Integer, PastryId> ids = chunkIds.get(chunk.getSessionId());
    if(ids==null) {
      ids = new HashMap<Integer, PastryId>();
      chunkIds.put(chunk.getSessionId(), ids);
    }
    ids.put(chunk.getSequenceId(), chunk.getResourceId());
  }
}