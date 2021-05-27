package com.vmware.vsan.client.sessionmanager.health;

import com.vmware.vsan.client.sessionmanager.vlsi.client.vchealth.VcHealthConnection;

public class VcHealthHealthMonitor implements IHealthMonitor {
   public void onCreated(VcHealthConnection resource, Object settings) {
   }

   public void onDisposed(VcHealthConnection resource, Object settings) {
   }

   public void check(VcHealthConnection resource, Object settings) throws Exception {
   }

   public void onError(VcHealthConnection resource, Object settings, Throwable t) {
   }
}
