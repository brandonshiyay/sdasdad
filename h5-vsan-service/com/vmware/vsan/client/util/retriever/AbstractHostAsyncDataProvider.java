package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.util.Measure;
import java.util.concurrent.ExecutionException;

public abstract class AbstractHostAsyncDataProvider implements DataRetriever {
   protected ManagedObjectReference hostRef;
   protected Measure measure;
   protected Future future;
   protected Object result;
   protected VsanClient vsanClient;

   public AbstractHostAsyncDataProvider(ManagedObjectReference hostRef, Measure measure, VsanClient vsanClient) {
      this.hostRef = hostRef;
      this.measure = measure;
      this.vsanClient = vsanClient;
   }

   public AbstractHostAsyncDataProvider(ManagedObjectReference hostRef, Measure measure) {
      this.hostRef = hostRef;
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
