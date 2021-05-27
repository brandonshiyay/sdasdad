package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjIdentityQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentityAndHealth;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanObjectHealthVersion;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

class ObjectIdentitiesDataRetriever extends AbstractAsyncDataRetriever {
   private Set uuids;
   private boolean includeHealth;

   public ObjectIdentitiesDataRetriever(ManagedObjectReference clusterRef, Measure measure, Set uuids, boolean queryHealth, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
      this.uuids = uuids;
      this.includeHealth = queryHealth;
   }

   public void start() {
      this.future = this.measure.newFuture("VsanObjectSystem.QueryObjectIdentities");
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var2 = null;

      try {
         VsanObjectSystem objectSystem = conn.getVsanObjectSystem();
         String[] uuidsParam = CollectionUtils.isEmpty(this.uuids) ? null : (String[])this.uuids.toArray(new String[this.uuids.size()]);
         objectSystem.queryObjectIdentities(this.clusterRef, uuidsParam, (String[])null, this.includeHealth, true, false, this.getHealthQuerySpec(), (Boolean)null, this.future);
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

   private VsanObjIdentityQuerySpec getHealthQuerySpec() {
      if (this.includeHealth && VsanCapabilityUtils.isObjectsHealthV2SupportedOnVc(this.clusterRef)) {
         VsanObjIdentityQuerySpec spec = new VsanObjIdentityQuerySpec();
         spec.setObjectHealthVersion(VsanObjectHealthVersion.v2.name());
         return spec;
      } else {
         return null;
      }
   }

   public VsanObjectIdentityAndHealth prepareResult() throws ExecutionException, InterruptedException {
      VsanObjectIdentityAndHealth result = (VsanObjectIdentityAndHealth)super.prepareResult();
      if (!ArrayUtils.isEmpty(result.identities)) {
         VsanObjectIdentity[] var2 = result.identities;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanObjectIdentity id = var2[var4];
            if (id.vm != null) {
               id.vm.setServerGuid(this.clusterRef.getServerGuid());
            }
         }
      }

      return result;
   }
}
