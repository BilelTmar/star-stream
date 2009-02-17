/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryNode;
import java.io.FileNotFoundException;

/**
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamNode extends PastryNode {

  public StarStreamNode(String prefix) throws FileNotFoundException {
    super(prefix);
  }
}