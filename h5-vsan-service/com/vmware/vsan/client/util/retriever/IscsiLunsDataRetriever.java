package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiLUN;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSystem;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.concurrent.ExecutionException;

class IscsiLunsDataRetriever extends AbstractAsyncDataRetriever {
   private boolean isSupported;

   public IscsiLunsDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
   }

   public void start() {
      this.isSupported = VsanCapabilityUtils.isIscsiTargetsSupportedOnCluster(this.clusterRef);
      if (this.isSupported) {
         this.future = this.measure.newFuture("VsanIscsiTargetSystem.getIscsiLUNs");

         try {
            VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
            Throwable var2 = null;

            try {
               VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
               vsanIscsiSystem.getIscsiLUNs(this.clusterRef, (String[])null, this.future);
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

   protected VsanIscsiLUN[] prepareResult() throws ExecutionException, InterruptedException {
      return this.isSupported ? (VsanIscsiLUN[])super.prepareResult() : new VsanIscsiLUN[0];
   }
}
