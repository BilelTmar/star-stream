/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.starstream.protocol.Chunk.ChunkId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;

/**
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkRequest extends ChunkAdvertisement {

  /**
   * 
   * @param src
   * @param dst
   * @param chunkId
   */
  ChunkRequest(StarStreamNode src, StarStreamNode dst, ChunkId chunkId) {
    super(src, dst, chunkId);
  }

  /**
   *
   * @return
   */
  public ChunkMissing createChunMissing() {
    return new ChunkMissing(getDestination(), getSource(), getChunkId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_REQ;
  }
}