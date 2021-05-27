package com.vmware.vsan.client.sessionmanager.vlsi.client.pbm;

import com.vmware.vim.binding.pbm.ServiceInstance;
import com.vmware.vsan.client.sessionmanager.vlsi.client.AbstractConnectionFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;

public class PbmFactory extends AbstractConnectionFactory {
   protected PbmConnection buildConnection(VlsiSettings settings) {
      return new PbmConnection();
   }

   public void onConnect(VlsiSettings id, PbmConnection connection) {
      super.onConnect(id, connection);
      ServiceInstance vcSi = (ServiceInstance)connection.createStub(ServiceInstance.class, "ServiceInstance");
      connection.content = vcSi.getContent();
   }
}
