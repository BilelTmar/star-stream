package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;

/**
 * This message is used to advertise the availability of a new chunk at the
 * (at least) advertising node. This message simply carries the chunk unique
 * identifier. Interested nodes have to reply this message with the appropriate
 * {@link ChunkRequest} to obtain the advertised chunk.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkAdvertisement extends StarStreamMessage {

  /**
   * The advertised chunk's unique identifier.
   */
  private final PastryId chunkId;

  /**
   * Constructor. When creating a new instance, the specified source is also used to
   * initialize the message originator.
   *
   * @param src The sender
   * @param dst The destination
   * @param chunkId The advertised chunk unique identifier
   */
  ChunkAdvertisement(StarStreamNode src, StarStreamNode dst, PastryId chunkId) {
    super(src, dst);
    if(chunkId==null) throw new IllegalArgumentException("The chunk identifier cannot be 'null'");
    this.chunkId = chunkId;
  }

  /**
   * Returns a reference to the chunk identifier.
   *
   * @return The advertised chunk unique identifier
   */
  public PastryId getChunkId() {
    return chunkId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_ADV;
  }

  /**
   * Creates a {@link ChunkRequest} message used to show interest in obtaining
   * the chunk from the advertising node.
   *
   * @return The {@link ChunkRequest} message
   */
  public ChunkRequest replyWithChunkReq() {
    return new ChunkRequest(getDestination(), getSource(), chunkId);
  }
}