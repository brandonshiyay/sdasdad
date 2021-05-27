package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerformanceManager;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;

class SupportedEntityTypesDataRetriever extends AbstractAsyncDataRetriever {
   public SupportedEntityTypesDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
   }

   public void start() {
      this.future = this.measure.newFuture("VsanPerformanceManager.getSupportedEntityTypes");

      try {
         VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
         Throwable var2 = null;

         try {
            VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();
            perfMgr.getSupportedEntityTypes(this.future);
         } catch (Throwable var12) {
            var2 = var12;
            throw var12;
         } finally {
            if (conn != null) {
               if (var2 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  conn.close();
               }
            }

         }
      } catch (Exception var14) {
         this.future.setException(var14);
      }

   }
}
