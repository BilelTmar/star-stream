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
public class ChunkMissing extends ChunkAdvertisement {

  /**
   * 
   * @param src
   * @param dst
   * @param chunkId
   */
  ChunkMissing(StarStreamNode src, StarStreamNode dst, ChunkId chunkId) {
    super(src, dst, chunkId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_MISSING;
  }
}