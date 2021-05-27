package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;

public class IscsiHomeObjectDataRetriever extends AbstractAsyncDataRetriever {
   public IscsiHomeObjectDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
   }

   public void start() {
      this.future = this.measure.newFuture("vsanIscsiSystem.getHomeObject");

      try {
         VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
         Throwable var2 = null;

         try {
            VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
            vsanIscsiSystem.getHomeObject(this.clusterRef, this.future);
         } catch (Throwable var13) {
            var2 = var13;
            throw var13;
         } finally {
            if (conn != null) {
               if (var2 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var12) {
                     var2.addSuppressed(var12);
                  }
               } else {
                  conn.close();
               }
            }

         }
      } catch (Exception var15) {
         this.future.setException(var15);
      }

   }
}
