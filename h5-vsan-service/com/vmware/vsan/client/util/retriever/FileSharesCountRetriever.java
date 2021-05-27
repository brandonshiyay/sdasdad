package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareQueryResult;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServiceSystem;
import com.vmware.vsan.client.services.fileservice.model.FileSharesPaginationSpec;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.concurrent.ExecutionException;

public class FileSharesCountRetriever extends AbstractAsyncDataRetriever {
   private boolean isFileServicesSupported;
   private Future fileShareFuture;
   private FileSharesPaginationSpec paginationSpec;

   public FileSharesCountRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient, FileSharesPaginationSpec paginationSpec) {
      super(clusterRef, measure, vsanClient);
      this.paginationSpec = paginationSpec;
   }

   public void start() {
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var2 = null;

      try {
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
         this.fileShareFuture = this.measure.newFuture("VsanVcFileServiceSystem.queryFileShares");
         FileSharesPaginationSpec spec = FileSharesPaginationSpec.getDefaultPaginationSpec();
         if (this.paginationSpec != null) {
            spec.filters = this.paginationSpec.filters;
            spec.shareType = this.paginationSpec.shareType;
         }

         fileServiceSystem.queryFileShares(spec.toVmodl(), this.clusterRef, this.fileShareFuture);
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

   }

   protected Integer prepareResult() throws ExecutionException, InterruptedException {
      FileShareQueryResult result = (FileShareQueryResult)this.fileShareFuture.get();
      return result != null && result.getTotalShareCount() != null ? result.totalShareCount.intValue() : 0;
   }
}
