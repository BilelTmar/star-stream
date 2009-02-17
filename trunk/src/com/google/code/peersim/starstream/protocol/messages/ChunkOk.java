package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.starstream.protocol.Chunk.ChunkId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import com.google.code.peersim.starstream.protocol.messages.StarStreamMessage.Type;

/**
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkOk extends ChunkAdvertisement {

  /**
   * 
   * @param src
   * @param dst
   * @param chunkId
   */
  ChunkOk(StarStreamNode src, StarStreamNode dst, ChunkId chunkId) {
    super(src, dst, chunkId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_OK;
  }
}
