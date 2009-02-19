/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.pastry.protocol.PastryResource;

/**
 * This class is used to represent chunks that must be disseminated to every node
 * during the streaming event. Being a {@link PastryResource}, it is uniquely
 * identified by a {@link PastryId} instance.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class Chunk<T> extends PastryResource<T> {
  
  /**
   * Constructor.
   * @param chunk The actual content
   */
  public Chunk(T chunk) {
    super(chunk);
  }
}