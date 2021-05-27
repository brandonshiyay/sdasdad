package com.vmware.vsan.client.sessionmanager.health;

import com.vmware.vim.binding.lookup.ServiceInstance;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;

public class LookupSvcHealthMonitor implements IHealthMonitor {
   public void onCreated(LookupSvcConnection resource, Object settings) {
   }

   public void onDisposed(LookupSvcConnection resource, Object settings) {
   }

   public void check(LookupSvcConnection resource, Object settings) throws Exception {
      ((ServiceInstance)resource.createStub(ServiceInstance.class, "ServiceInstance")).retrieveServiceContent();
   }

   public void onError(LookupSvcConnection resource, Object settings, Throwable t) {
   }
}
