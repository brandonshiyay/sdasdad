package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.pbm.profile.ProfileManager;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmConnection;
import com.vmware.vsan.client.util.Measure;

public class CapabilityObjectSchemaDataRetriever extends AbstractAsyncDataRetriever {
   private PbmClient pbmClient;

   public CapabilityObjectSchemaDataRetriever(ManagedObjectReference clusterRef, Measure measure, PbmClient pbmClient) {
      super(clusterRef, measure);
      this.pbmClient = pbmClient;
   }

   public void start() {
      try {
         PbmConnection pbmConn = this.pbmClient.getConnection(this.clusterRef.getServerGuid());
         Throwable var2 = null;

         try {
            this.future = this.measure.newFuture("ProfileManager.FetchCapabilitySchema");
            ProfileManager profileManager = pbmConn.getProfileManager();
            profileManager.fetchCapabilitySchema((String)null, (String[])null, this.future);
         } catch (Throwable var12) {
            var2 = var12;
            throw var12;
         } finally {
            if (pbmConn != null) {
               if (var2 != null) {
                  try {
                     pbmConn.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  pbmConn.close();
               }
            }

         }
      } catch (Exception var14) {
         this.future.setException(var14);
      }

   }
}
