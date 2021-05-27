package com.vmware.vsan.client.sessionmanager.vlsi.client.sso;

import com.vmware.vim.binding.sso.admin.ServiceInstance;
import com.vmware.vsan.client.sessionmanager.vlsi.client.AbstractConnectionFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;

public class SsoAdminFactory extends AbstractConnectionFactory {
   protected SsoAdminConnection buildConnection(VlsiSettings settings) {
      return new SsoAdminConnection();
   }

   protected void onConnect(VlsiSettings settings, SsoAdminConnection connection) {
      super.onConnect(settings, connection);
      ServiceInstance si = (ServiceInstance)connection.createStub(ServiceInstance.class, "SsoAdminServiceInstance");
      connection.content = si.retrieveServiceContent();
   }
}
