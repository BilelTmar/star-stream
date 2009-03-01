/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.starstream.protocol.StarStreamNode;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;

/**
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamScheduler implements Control {
  
  public static final String START_TIME = "start";
  private long start;

  public StarStreamScheduler(String prefix) {
    start = Configuration.getLong(prefix+"."+START_TIME);
  }

  @Override
  public boolean execute() {
    boolean stop = false;
    if(CommonState.getTime()>=start) {
      int size = Network.size();
      for(int i=0; i<size; i++) {
        StarStreamNode node = (StarStreamNode) Network.get(i);
      }
    }
    return stop;
  }

}