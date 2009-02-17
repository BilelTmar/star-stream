/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.starstream.protocol.*;
import com.google.code.peersim.starstream.protocol.Chunk.*;

/**
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkMessage<T> extends StarStreamMessage {

  /**
   * 
   */
  private Chunk chunk;

  /**
   *
   * @param orig
   * @param dst
   * @param chunk
   */
  public ChunkMessage(StarStreamNode src, StarStreamNode dst, Chunk chunk) {
    super(src, dst);
    this.chunk = chunk;
  }

  /**
   *
   * @return
   */
  public ChunkAdvertisement createChunkAdv() {
    return new ChunkAdvertisement(getDestination(), getSource(), chunk.getId());
  }

  /**
   *
   * @return
   */
  public Chunk getChunk() {
    return chunk;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK;
  }

  /**
   *
   * @return
   */
  public ChunkOk replyOk() {
    return new ChunkOk(getDestination(), getSource(), chunk.getId());
  }

  /**
   *
   * @return
   */
  public ChunkKo replyKo() {
    return new ChunkKo(getDestination(), getSource(), chunk.getId());
  }
}