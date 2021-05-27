package com.vmware.vsan.client.sessionmanager.vlsi.client.vchealth;

import com.vmware.vsan.client.sessionmanager.vlsi.client.AbstractConnectionFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;

public class VcHealthFactory extends AbstractConnectionFactory {
   protected VcHealthConnection buildConnection(VlsiSettings id) {
      return new VcHealthConnection();
   }
}
