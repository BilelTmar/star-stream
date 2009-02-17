package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.starstream.protocol.Chunk.ChunkId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;

/**
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkAdvertisement extends StarStreamMessage {

  /**
   * 
   */
  private ChunkId chunkId;

  /**
   *
   * @param src
   * @param dst
   * @param chunkId
   */
  ChunkAdvertisement(StarStreamNode src, StarStreamNode dst, ChunkId chunkId) {
    super(src, dst);
    this.chunkId = chunkId;
  }

  /**
   * 
   * @return
   */
  public ChunkRequest createChunkReq() {
    return new ChunkRequest(getDestination(), getSource(), chunkId);
  }

  /**
   *
   * @return
   */
  public ChunkId getChunkId() {
    return chunkId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_ADV;
  }
}
