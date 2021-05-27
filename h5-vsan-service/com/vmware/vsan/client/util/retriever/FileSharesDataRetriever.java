package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareQueryResult;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServiceSystem;
import com.vmware.vsan.client.services.fileservice.model.FileSharesPaginationSpec;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

class FileSharesDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(FileSharesDataRetriever.class);
   private List allFileShares = new ArrayList();
   private List futureQueries;
   private FileSharesPaginationSpec paginationSpec;

   public FileSharesDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient, FileSharesPaginationSpec spec) {
      super(clusterRef, measure, vsanClient);
      this.paginationSpec = spec;
   }

   public void start() {
      if (this.paginationSpec != null) {
         this.queryFileSharesWithPagination(new FileSharesPaginationSpec[]{this.paginationSpec});
      } else {
         int totalPages = this.getTotalPageCount();
         if (totalPages > 1) {
            this.queryFileSharesWithPagination((FileSharesPaginationSpec[])IntStream.rangeClosed(2, totalPages).mapToObj(this::getPaginationSpec).toArray((x$0) -> {
               return new FileSharesPaginationSpec[x$0];
            }));
         }

      }
   }

   private FileSharesPaginationSpec getPaginationSpec(int pageIndex) {
      FileSharesPaginationSpec spec = FileSharesPaginationSpec.getDefaultPaginationSpec();
      spec.pageIndex = pageIndex;
      spec.queryProperties.includeBasic = true;
      spec.queryProperties.includeVsanObjectUuids = true;
      return spec;
   }

   private int getTotalPageCount() {
      int result = 0;

      try {
         FileShareQueryResult firstPage = this.getPagedResult(this.getPaginationSpec(1));
         if (firstPage == null) {
            return result;
         }

         if (ArrayUtils.isNotEmpty(firstPage.fileShares)) {
            this.allFileShares.addAll(Arrays.asList(firstPage.fileShares));
         }

         int total = firstPage.totalShareCount == null ? 0 : firstPage.totalShareCount.intValue();
         result = total / 32;
         if (total % 32 != 0) {
            ++result;
         }
      } catch (Exception var4) {
         logger.error("Cannot load file shares for cluster " + this.clusterRef, var4);
      }

      return result;
   }

   private FileShareQueryResult getPagedResult(FileSharesPaginationSpec spec) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var3 = null;

      FileShareQueryResult var5;
      try {
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
         var5 = fileServiceSystem.queryFileShares(spec.toVmodl(), this.clusterRef);
      } catch (Throwable var14) {
         var3 = var14;
         throw var14;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var13) {
                  var3.addSuppressed(var13);
               }
            } else {
               conn.close();
            }
         }

      }

      return var5;
   }

   private void queryFileSharesWithPagination(FileSharesPaginationSpec[] specs) {
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var3 = null;

      try {
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
         this.futureQueries = new ArrayList();
         FileSharesPaginationSpec[] var5 = specs;
         int var6 = specs.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            FileSharesPaginationSpec spec = var5[var7];
            Future queryResultFuture = this.measure.newFuture("fileServiceSystem.queryFileShares");
            fileServiceSystem.queryFileShares(spec.toVmodl(), this.clusterRef, queryResultFuture);
            this.futureQueries.add(queryResultFuture);
         }
      } catch (Throwable var17) {
         var3 = var17;
         throw var17;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var16) {
                  var3.addSuppressed(var16);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   protected List prepareResult() throws ExecutionException, InterruptedException {
      if (CollectionUtils.isEmpty(this.futureQueries)) {
         return this.allFileShares;
      } else {
         Iterator var1 = this.futureQueries.iterator();

         while(var1.hasNext()) {
            Future future = (Future)var1.next();
            FileShareQueryResult fileShareQueryResult = (FileShareQueryResult)future.get();
            if (fileShareQueryResult != null && ArrayUtils.isNotEmpty(fileShareQueryResult.fileShares)) {
               this.allFileShares.addAll(Arrays.asList(fileShareQueryResult.fileShares));
            }
         }

         return this.allFileShares;
      }
   }
}
