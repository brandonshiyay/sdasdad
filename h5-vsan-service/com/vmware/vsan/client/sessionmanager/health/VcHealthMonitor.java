package com.vmware.vsan.client.sessionmanager.health;

import com.vmware.vim.binding.vim.ServiceInstance;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;

public class VcHealthMonitor implements IHealthMonitor {
   public static final String SERVICE_INSTANCE = "ServiceInstance";

   public void onCreated(VcConnection resource, Object settings) {
   }

   public void onDisposed(VcConnection resource, Object settings) {
   }

   public void check(VcConnection resource, Object settings) {
      ServiceInstance si = (ServiceInstance)resource.createStub(ServiceInstance.class, "ServiceInstance");
      si.getServerClock();
   }

   public void onError(VcConnection resource, Object settings, Throwable t) {
   }
}
