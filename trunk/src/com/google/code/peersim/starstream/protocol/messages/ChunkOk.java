package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import com.google.code.peersim.starstream.protocol.messages.StarStreamMessage.Type;
import java.util.UUID;

/**
 * This message is used to report the successful reception of a chunk, in response
 * to a {@link ChunkMessage}.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkOk extends ChunkAdvertisement {

  /**
   * Constructor. When creating a new instance, the specified source is also used to
   * initialize the message originator.
   *
   * @param src The sender
   * @param dst The destination
   * @param sessionId The session ID
   * @param chunkId The chunk ID
   */
  ChunkOk(StarStreamNode src, StarStreamNode dst, UUID sessionId, PastryId chunkId) {
    super(src, dst, sessionId, chunkId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_OK;
  }
}