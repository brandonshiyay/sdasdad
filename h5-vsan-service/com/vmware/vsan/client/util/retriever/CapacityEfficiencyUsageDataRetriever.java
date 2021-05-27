package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcDiskManagementSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;

public class CapacityEfficiencyUsageDataRetriever extends AbstractAsyncDataRetriever {
   public CapacityEfficiencyUsageDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
   }

   public void start() {
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var2 = null;

      try {
         this.future = this.measure.newFuture("VsanVcDiskManagementSystem.queryClusterDataEfficiencyCapacityState");
         VsanVcDiskManagementSystem diskManagementSystem = conn.getVsanDiskManagementSystem();
         diskManagementSystem.queryClusterDataEfficiencyCapacityState(this.clusterRef, this.future);
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
