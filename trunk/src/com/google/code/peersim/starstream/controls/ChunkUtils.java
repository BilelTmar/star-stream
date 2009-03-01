package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.pastry.protocol.PastryResource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class is useful to disparate other components that need to deal with chunks.
 * It must be used as a <i>chunks-factory</i> (i.e. by the {@link StarStreamSource}),
 * and as a centralized <i>generated chunk identifiers repository</i> by *-Stream
 * Chunk Schedulers.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkUtils {

  /**
   * Memory of generated chunk-identifiers. It has a two-level depth, the first one
   * being the *-Stream session identifier (an UUID), and the second being a chunk's
   * sequence identifier. The final value that can be accessed is the concrete chunk
   * identifier, that is a {@link PastryId}.
   */
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
  static <T> Chunk<T> createChunk(T data, UUID sid, int seqNumber) {
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
    if (ids != null) {
      res = ids.get(seqNumber + 1);
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
    if (ids == null) {
      ids = new HashMap<Integer, PastryId>();
      chunkIds.put(chunk.getSessionId(), ids);
    }
    ids.put(chunk.getSequenceId(), chunk.getResourceId());
  }

  /**
   * This class is used to represent chunks that must be disseminated to every node
   * during the streaming event. Being a {@link PastryResource}, it is uniquely
   * identified by a {@link PastryId} instance.
   *
   * @author frusso
   * @version 0.1
   * @since 0.1
   */
  public static class Chunk<T> extends PastryResource<T> {

    private final UUID sessionId;
    private final int sequenceId;

    /**
     * Constructor.
     * @param chunk The actual content
     * @param sid The streaming-session identifier
     * @param seq The sequence number
     */
    private Chunk(T chunk, UUID sid, int seq) {
      super(chunk);
      sessionId = sid;
      sequenceId = seq;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
      if(!(other instanceof Chunk))
        return false;
      Chunk<?> otherChunk = (Chunk<?>) other;
      return super.equals(other) && sequenceId==otherChunk.sequenceId && sessionId.equals(otherChunk.sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      int hash = 5;
      hash = 89 * hash + (this.sessionId != null ? this.sessionId.hashCode() : 0);
      hash = 89 * hash + this.sequenceId;
      hash = 89 * hash + super.hashCode();
      return hash;
    }

    /**
     * Returns the sequence number associated with this chunk of data.
     * @return The sequence number
     */
    public int getSequenceId() {
      return sequenceId;
    }

    /**
     * Returns the session id associated with this chunk of data.
     * @return The session identifier
     */
    public UUID getSessionId() {
      return sessionId;
    }
  }
}