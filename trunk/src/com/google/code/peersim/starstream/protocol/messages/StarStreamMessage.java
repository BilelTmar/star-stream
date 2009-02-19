/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.starstream.protocol.*;
import java.util.UUID;

/**
 * Base abstract class for all the *-stream messages. Every *-stream message has
 * at least:
 * <ol>
 * <li>an <i>originator</i>, that is the node that sent the message for first</li>
 * <li>a <i>sender</i>, that is the node the message has been actually received from
 * (it is not the <i>originator</i> in case of forwarding)</li>
 * <li>a <i>destination</i>, that is the node the message is addressed to</li>
 * </ol>
 * Every concrete message class must also provide methods for replying to the current
 * message with another message, according to what the protocol prescribes.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public abstract class StarStreamMessage {

  /**
   * Classification of *-stream message types.
   *
   * @author frusso
   * @version 0.1
   * @since 0.1
   */
  public static enum Type {
    /**
     * This kind of message typically travels over unreliable transports and is
     * used for sending chunks to other nodes.
     */
    CHUNK {
      /**
       * {@inheritDoc}
       */
      public String toString() {
        return "Chunk message";
      }
    },
    /**
     * This kind of message typically travels over unreliable transports and is
     * used to reply to a {@link StarStreamMessage.Type#CHUNK} message to inform
     * the other node the chunk has been properly received.
     */
    CHUNK_OK {
      /**
       * {@inheritDoc}
       */
      public String toString() {
        return "Chunk OK message";
      }
    },
    /**
     * This kind of message typically travels over unreliable transports and is
     * used to reply to a {@link StarStreamMessage.Type#CHUNK} message to inform
     * the other node the chunk has not been properly received.
     */
    CHUNK_KO {
      /**
       * {@inheritDoc}
       */
      public String toString() {
        return "Chunk KO message";
      }
    },
    /**
     * This kind of message typically travels over reliable transports and is
     * used to inform other nodes (the <i>centers</i>) that a new chunk
     * has been received. Thus this message follows {@link StarStreamMessage.Type#CHUNK}
     * messages.
     */
    CHUNK_ADV {
      /**
       * {@inheritDoc}
       */
      public String toString() {
        return "Chunk advertisement message";
      }
    },
    /**
     * This kind of message typically travels over reliable transports and is
     * used to reply to a {@link StarStreamMessage.Type#CHUNK_ADV} message. Such
     * a reply means that we are interested in receiving the advertised chunk.
     */
    CHUNK_REQ {
      /**
       * {@inheritDoc}
       */
      public String toString() {
        return "Chunk request message";
      }
    },
    /**
     * This kind of message typically travels over reliable transports and is
     * used to reply to a {@link StarStreamMessage.Type#CHUNK_REQ} message. Such
     * a reply means the requested chunk is not actually available.
     */
    CHUNK_MISSING {
      /**
       * {@inheritDoc}
       */
      public String toString() {
        return "Chunk missing message";
      }
    };

    /**
     * Returns a human-readable description of the message type.
     */
    @Override
    public abstract String toString();
  }

  /**
   * The node that has to receive the message.
   */
  private StarStreamNode destination;
  /**
   * The number of hops the message has travelled.
   */
  private int hops;
  /**
   * Unique message identifier.
   */
  private UUID id;
  /**
   * The node that originally sent the message for first.
   */
  private StarStreamNode originator;
  /**
   * The node the message has been received from.
   */
  private StarStreamNode source;

  /**
   * Internal constructor useful for subclassess only.
   *
   * @param src The sender (used to initialize the <i>originator</i> as well
   * @param dst The destination
   */
  protected StarStreamMessage(StarStreamNode src, StarStreamNode dst) {
    if(src==null) throw new IllegalArgumentException("The source cannot be 'null'");
    if(dst==null) throw new IllegalArgumentException("The destination cannot be 'null'");
    this.source = src;
    this.originator = src;
    this.destination = dst;
    id = UUID.randomUUID();
    hops = 0;
  }

  /**
   * Two {@link StarStreamMessage} instances are considered equivalent iff:
   * <ol>
   * <li>they have the same identifier</li>
   * <li>they have the same type</li>
   * </ol>
   * @param obj The other instance
   * @return {@link Boolean#TRUE} iff the two instances are logically equivalent,
   * {@link Boolean#FALSE} otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if(this==obj)
      return true;
    if(!(obj instanceof StarStreamMessage))
      return false;

    StarStreamMessage that = (StarStreamMessage)obj;
    return this.id.equals(that.id) &&
            this.getType().equals(that.getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int hash = 17;
    hash = 31 * hash + this.id.hashCode();
    hash = 31 * hash + this.getType().hashCode();
    return hash;
  }

  /**
   * The actual message type.
   * @return The actual message type
   */
  public abstract Type getType();

  /**
   * The node that has to receive the message
   * @return The destination
   */
  public StarStreamNode getDestination() {
    return destination;
  }

  /**
   * Returns the number of hops the message has travelled so far.
   * @return The number of hops
   */
  public int getHops() {
    return hops;
  }

  /**
   * The unique immutable identifier associated with the message.
   * @return The message identifier
   */
  public UUID getId() {
    return id;
  }

  /**
   * The node that originally sent the message.
   * @return The originator
   */
  public StarStreamNode getOriginator() {
    return originator;
  }

  /**
   * The node the message must be received from
   * @return The source
   */
  public StarStreamNode getSource() {
    return source;
  }

  /**
   * Increase the current number of hops adding 1.
   */
  protected void increaseHops() {
    hops++;
  }

  /**
   * Sets the destination of the message
   * @param destination The destination to set
   */
  protected void setDestination(StarStreamNode destination) {
    this.destination = destination;
  }

  /**
   * Sets the originator of the message.
   * @param originator The originator to set
   */
  protected void setOriginator(StarStreamNode originator) {
    this.originator = originator;
  }

  /**
   * Sets the source of the message
   * @param source The source to set
   */
  protected void setSource(StarStreamNode source) {
    this.source = source;
  }
}