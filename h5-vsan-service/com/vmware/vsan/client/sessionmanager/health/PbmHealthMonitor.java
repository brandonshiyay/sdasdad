package com.vmware.vsan.client.sessionmanager.health;

import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmConnection;

public class PbmHealthMonitor implements IHealthMonitor {
   public void onCreated(PbmConnection resource, Object settings) {
   }

   public void onDisposed(PbmConnection resource, Object settings) {
   }

   public void check(PbmConnection resource, Object settings) throws Exception {
      resource.getProfileManager().fetchResourceType();
   }

   public void onError(PbmConnection resource, Object settings, Throwable t) {
   }
}
