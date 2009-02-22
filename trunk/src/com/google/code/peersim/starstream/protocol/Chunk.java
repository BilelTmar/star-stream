/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.pastry.protocol.PastryResource;
import java.util.UUID;

/**
 * This class is used to represent chunks that must be disseminated to every node
 * during the streaming event. Being a {@link PastryResource}, it is uniquely
 * identified by a {@link PastryId} instance.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class Chunk<T> extends PastryResource<T> {

  private final UUID sessionId;
  private final int sequenceId;

  /**
   * Constructor.
   * @param chunk The actual content
   * @param sid The streaming-session identifier
   * @param seq The sequence number
   */
  Chunk(T chunk, UUID sid, int seq) {
    super(chunk);
    sessionId = sid;
    sequenceId = seq;
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