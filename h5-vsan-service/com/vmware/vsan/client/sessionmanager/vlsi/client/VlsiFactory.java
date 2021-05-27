package com.vmware.vsan.client.sessionmanager.vlsi.client;

public class VlsiFactory extends AbstractConnectionFactory {
   protected VlsiConnection buildConnection(VlsiSettings id) {
      return new VlsiConnection();
   }
}
