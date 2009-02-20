/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryNode;
import com.google.code.peersim.pastry.protocol.PastryProtocol;
import java.io.FileNotFoundException;
import peersim.config.Configuration;

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
public class StarStreamNode extends PastryNode {

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
    bindStarStreamToPastry();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object clone() {
    StarStreamNode clone = (StarStreamNode) super.clone();
    clone.bindStarStreamToPastry();
    return clone;
  }

  /**
   * Returns a reference to this node's assigned {@link StarStreamProtocol} instance.
   * @return The {@link StarStreamProtocol} instance
   */
  protected StarStreamProtocol getStarStreamProtocol() {
    return (StarStreamProtocol) super.getProtocol(STAR_STREAM_PID);
  }

  /**
   * This method has to be invoked both at construction and cloning-time to let
   * the {@link StarStreamProtocol} instance register over the {@link PastryProtocol}
   * instance for Pastry-related events.
   */
  private void bindStarStreamToPastry() {
    PastryProtocol pastry = getPastryProtocol();
    getStarStreamProtocol().registerPastryListeners(pastry);
  }
}