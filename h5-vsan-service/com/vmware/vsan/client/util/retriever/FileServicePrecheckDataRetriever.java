package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceDomainConfig;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServicePreflightCheckResult;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServiceSystem;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.concurrent.ExecutionException;

class FileServicePrecheckDataRetriever extends AbstractAsyncDataRetriever {
   private boolean isFileServicesSupported;

   public FileServicePrecheckDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
   }

   public void start() {
      this.isFileServicesSupported = VsanCapabilityUtils.isFileServiceSupported(this.clusterRef);
      if (this.isFileServicesSupported) {
         try {
            VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
            Throwable var2 = null;

            try {
               VsanFileServiceSystem vsanVcFileServiceSystem = conn.getVsanFileServiceSystem();
               this.future = this.measure.newFuture("VsanVcFileServiceSystem.performFileServicePreflightCheck");
               vsanVcFileServiceSystem.performFileServicePreflightCheck(this.clusterRef, (FileServiceDomainConfig)null, (ManagedObjectReference)null, this.future);
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

   protected VsanFileServicePreflightCheckResult prepareResult() throws ExecutionException, InterruptedException {
      return this.isFileServicesSupported ? (VsanFileServicePreflightCheckResult)super.prepareResult() : null;
   }
}
