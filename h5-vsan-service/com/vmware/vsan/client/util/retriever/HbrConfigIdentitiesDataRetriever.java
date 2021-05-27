package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjIdentityQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;

public class HbrConfigIdentitiesDataRetriever extends AbstractAsyncDataRetriever {
   public HbrConfigIdentitiesDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
   }

   public void start() {
      this.future = this.measure.newFuture("VsanObjectSystem.QueryObjectIdentities.HbrConfig");
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var2 = null;

      try {
         VsanObjectSystem objectSystem = conn.getVsanObjectSystem();
         objectSystem.queryObjectIdentities(this.clusterRef, (String[])null, new String[]{VsanObjectType.hbrCfg.name()}, false, true, false, (VsanObjIdentityQuerySpec)null, (Boolean)null, this.future);
      } catch (Throwable var11) {
         var2 = var11;
         throw var11;
      } finally {
         if (conn != null) {
            if (var2 != null) {
               try {
                  conn.close();
               } catch (Throwable var10) {
                  var2.addSuppressed(var10);
               }
            } else {
               conn.close();
            }
         }

      }

   }
}
