package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjIdentityQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentityAndHealth;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import java.util.concurrent.ExecutionException;

public class ExtensionIdentitiesDataRetriever extends AbstractAsyncDataRetriever {
   private static final String[] EXTENSION_IDENTITIES_TYPES;
   private boolean isSupported;

   public ExtensionIdentitiesDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
   }

   public void start() {
      this.isSupported = VsanCapabilityUtils.isPersistenceServiceSupportedOnVc(this.clusterRef);
      if (this.isSupported) {
         this.future = this.measure.newFuture("VsanObjectSystem.QueryObjectIdentities.Extentions");
         VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
         Throwable var2 = null;

         try {
            VsanObjectSystem objectSystem = conn.getVsanObjectSystem();
            objectSystem.queryObjectIdentities(this.clusterRef, (String[])null, EXTENSION_IDENTITIES_TYPES, false, true, false, (VsanObjIdentityQuerySpec)null, (Boolean)null, this.future);
         } catch (Throwable var11) {
            var2 = var11;
            throw var11;
         } finally {
            if (conn != null) {
               if (var2 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var10) {
                     var2.addSuppressed(var10);
                  }
               } else {
                  conn.close();
               }
            }

         }

      }
   }

   protected VsanObjectIdentityAndHealth prepareResult() throws ExecutionException, InterruptedException {
      return this.isSupported ? (VsanObjectIdentityAndHealth)super.prepareResult() : new VsanObjectIdentityAndHealth();
   }

   static {
      EXTENSION_IDENTITIES_TYPES = new String[]{VsanObjectType.extension.name()};
   }
}
