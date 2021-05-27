package com.vmware.vsan.client.sessionmanager.health;

import com.vmware.vsan.client.sessionmanager.resource.Resource;

public interface IHealthMonitor {
   void onCreated(Resource var1, Object var2);

   void onDisposed(Resource var1, Object var2);

   void check(Resource var1, Object var2) throws Exception;

   void onError(Resource var1, Object var2, Throwable var3);
}
