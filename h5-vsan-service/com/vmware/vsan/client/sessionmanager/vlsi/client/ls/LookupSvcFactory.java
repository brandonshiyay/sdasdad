package com.vmware.vsan.client.sessionmanager.vlsi.client.ls;

import com.vmware.vim.binding.lookup.ServiceInstance;
import com.vmware.vsan.client.sessionmanager.vlsi.client.AbstractConnectionFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;

public class LookupSvcFactory extends AbstractConnectionFactory {
   protected LookupSvcConnection buildConnection(VlsiSettings id) {
      return new LookupSvcConnection();
   }

   public void onConnect(VlsiSettings id, LookupSvcConnection connection) {
      super.onConnect(id, connection);
      ServiceInstance si = (ServiceInstance)connection.createStub(ServiceInstance.class, "ServiceInstance");
      connection.content = si.retrieveServiceContent();
   }
}
