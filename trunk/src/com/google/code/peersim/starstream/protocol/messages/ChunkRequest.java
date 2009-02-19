/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;

/**
 * This message is used to express the interest in receiving a chunk that has
 * been advertised by means of a {@link ChunkAdvertisement} message.
 * 
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkRequest extends ChunkAdvertisement {

  /**
   * Constructor. When creating a new instance, the specified source is also used to
   * initialize the message originator.
   *
   * @param src The sender
   * @param dst The destination
   * @param chunkId The identifier of the chunk we are interested in
   */
  ChunkRequest(StarStreamNode src, StarStreamNode dst, PastryId chunkId) {
    super(src, dst, chunkId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_REQ;
  }

  /**
   * If a node that receives a {@link ChunkRequest} is not able to provide the
   * inquiring node with the requested chunk, this method must be used to create
   * a {@link ChunkMissing} message.
   *
   * @return The {@link ChunkMissing} message
   */
  public ChunkMissing replyWithChunkMissing() {
    return new ChunkMissing(getDestination(), getSource(), getChunkId());
  }
}