/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.starstream.protocol.*;
import com.google.code.peersim.starstream.protocol.Chunk.*;

/**
 * This message is used to disseminate a new chunk into the network.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkMessage extends StarStreamMessage {

  /**
   * The chunk that has to be disseminated.
   */
  private Chunk chunk;

  /**
   * Constructor. When creating a new instance, the specified source is also used to
   * initialize the message originator.
   *
   * @param src The sender
   * @param dst The destination
   * @param chunk The chunk
   */
  public ChunkMessage(StarStreamNode src, StarStreamNode dst, Chunk chunk) {
    super(src, dst);
    if(chunk==null) throw new IllegalArgumentException("The chunk cannot be 'null'");
    this.chunk = chunk;
  }

  /**
   * Returns a reference to the chunk.
   * @return The chunk
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
   * Creates a {@link ChunkKo} message required to signal the sending node
   * that the chunk was either currupted or the message did not arrived at all
   * i.e. due to timeout expiration.
   *
   * @return The {@link ChunkKo} message
   */
  public ChunkKo replyKo() {
    return new ChunkKo(getDestination(), getSource(), chunk.getResourceId());
  }

  /**
   * Creates a {@link ChunkOk} message required to signal the sending node
   * that the chunk has been properly received.
   *
   * @return The {@link ChunkOk} message
   */
  public ChunkOk replyOk() {
    return new ChunkOk(getDestination(), getSource(), chunk.getResourceId());
  }

  /**
   * Creates a {@link ChunkAdvertisement} message that must be used by nodes
   * that receive new chunks to advertise their presence to their neighbors.
   *
   * @return The {@link ChunkAdvertisement} message
   */
  public ChunkAdvertisement replyWithChunkAdv() {
    return new ChunkAdvertisement(getDestination(), getSource(), chunk.getResourceId());
  }
}