/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryNode;
import com.google.code.peersim.pastry.protocol.PastryProtocol;
import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;
import java.io.FileNotFoundException;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.transport.Transport;

/**
 * This class represents a node that leverages both the *-Stream and the Pastry
 * protocols to take part to a P2P streaming event.<br>
 * Extending {@link PastryNode} this class can take advantage of all the features
 * and control classes available for {@link PastryNode}.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamNode extends PastryNode implements StarStreamProtocolListenerIfc {

  /**
   * Configuration parameter key that ties a *-Stream node to its *-Stream protocol.
   */
  public static final String STAR_STREAM = "starstream";
  /**
   * The protocol identifier assigned to the *-Stream protocol by the PeerSim runtime.
   */
  private static int STAR_STREAM_PID;
  /**
   * Separator char used for PeersSim-related configuration properties.
   */
  private static final String SEPARATOR = ".";

  /**
   * Default PeerSim-required constructor.
   *
   * @param prefix The prefix for accessing configuration properties
   * @throws java.io.FileNotFoundException Raised whenever it is not possible creating the log file
   */
  public StarStreamNode(String prefix) throws FileNotFoundException {
    super(prefix);
    STAR_STREAM_PID = Configuration.getPid(prefix+SEPARATOR+STAR_STREAM);
    // at this point the protocl stack for the current node must have been already
    // initialized by the PeerSim runtime, so this is the right time for takeing
    // references to Pastry and *-Stream and tight the two instance enabling the
    // latter to receive notifications from the former
    init();
  }

  /**
   * Tells the *-Stream protocol to check for expired messages that were waiting
   * for acks/nacks and behave consequently.
   */
  public void checkForStarStreamTimeouts() {
    getStarStreamProtocol().checkForTimeouts();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object clone() {
    StarStreamNode clone = (StarStreamNode) super.clone();
    clone.init();
    return clone;
  }

  /**
   * Returns a reference to the configure <i>unreliable</i> transport for the
   * *-Stream protocol.
   *
   * @return The unreliable transport
   */
  public Transport getStarStreamTransport() {
    return (Transport) getProtocol(FastConfig.getTransport(STAR_STREAM_PID));
  }

  /**
   * The PeerSim-assigned *-Stream protocol identifier.
   *
   * @return The PeerSim-assigned *-Stream protocol identifier.
   */
  public int getStarStreamPid() {
    return STAR_STREAM_PID;
  }

  /**
   * Returns a reference to the *-Store owned by this node.
   *
   * @return The *-Store
   */
  public StarStreamStore getStore() {
    return getStarStreamProtocol().getStore();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return super.toString()+"\n"+getStore();
  }

  /**
   * Returns a reference to this node's assigned {@link StarStreamProtocol} instance.
   * @return The {@link StarStreamProtocol} instance
   */
  protected StarStreamProtocol getStarStreamProtocol() {
    return (StarStreamProtocol) getProtocol(STAR_STREAM_PID);
  }

  /**
   * This method has to be invoked both at construction and cloning-time to let
   * the {@link StarStreamProtocol} instance register over the {@link PastryProtocol}
   * instance for Pastry-related events.
   */
  private void init() {
    getStarStreamProtocol().setOwner(this);
    getStarStreamProtocol().registerStarStreamListener(this);
    PastryProtocol pastry = getPastryProtocol();
    getStarStreamProtocol().registerPastryListeners(pastry);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void notifyNewChunk(Chunk<?> chunk) {
    log("[*-STREAM] node "+this.getPastryId()+" has stored resource "+chunk);
  }
}