/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryNode;
import java.io.FileNotFoundException;

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
   * Default PeerSim-required constructor.
   *
   * @param prefix The prefix for accessing configuration properties
   * @throws java.io.FileNotFoundException Raised whenever it is not possible creating the log file
   */
  public StarStreamNode(String prefix) throws FileNotFoundException {
    super(prefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object clone() {
    return super.clone();
  }
}