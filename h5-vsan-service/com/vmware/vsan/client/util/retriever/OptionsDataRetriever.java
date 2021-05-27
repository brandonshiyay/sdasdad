package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.option.OptionManager;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;

public class OptionsDataRetriever extends AbstractAsyncDataRetriever {
   private VcClient vcClient;
   private String queryView;

   public OptionsDataRetriever(ManagedObjectReference clusterRef, String queryView, Measure measure, VcClient vcClient) {
      super(clusterRef, measure);
      this.vcClient = vcClient;
      this.queryView = queryView;
   }

   public void start() {
      this.future = this.measure.newFuture("OptionManager.queryView");

      try {
         VcConnection vcConnection = this.vcClient.getConnection(this.clusterRef.getServerGuid());
         Throwable var2 = null;

         try {
            OptionManager optionManager = vcConnection.getOptionManager();
            optionManager.queryView(this.queryView, this.future);
         } catch (Throwable var12) {
            var2 = var12;
            throw var12;
         } finally {
            if (vcConnection != null) {
               if (var2 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      } catch (Exception var14) {
         this.future.setException(var14);
      }

   }
}
