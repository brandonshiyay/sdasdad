package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.util.Measure;
import java.util.concurrent.ExecutionException;

abstract class AbstractAsyncDataRetriever implements DataRetriever {
   protected ManagedObjectReference clusterRef;
   protected Measure measure;
   protected Future future;
   protected Object result;
   protected VsanClient vsanClient;

   public AbstractAsyncDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      this.clusterRef = clusterRef;
      this.measure = measure;
      this.vsanClient = vsanClient;
   }

   public AbstractAsyncDataRetriever(ManagedObjectReference clusterRef, Measure measure) {
      this.clusterRef = clusterRef;
      this.measure = measure;
   }

   public final Object getResult() throws ExecutionException, InterruptedException {
      if (this.result == null) {
         this.result = this.prepareResult();
      }

      return this.result;
   }

   protected Object prepareResult() throws ExecutionException, InterruptedException {
      return this.future.get();
   }
}
