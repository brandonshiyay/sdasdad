package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.vm.ProfileSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.QueryVsanManagedStorageSpaceUsageSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDatastoreType;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceReportSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsage;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsageWithDatastoreType;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class SpaceUsageDataRetriever extends AbstractAsyncDataRetriever {
   private final DatastoreType datastoreType;
   private Future vsanSpaceUsageFuture;

   public SpaceUsageDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient, DatastoreType datastoreType) {
      super(clusterRef, measure, vsanClient);
      this.datastoreType = datastoreType;
   }

   public void start() {
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var2 = null;

      try {
         VsanSpaceReportSystem capacitySystem = conn.getVsanSpaceReportSystem();
         if (this.loadVsanDataOnly()) {
            this.vsanSpaceUsageFuture = this.measure.newFuture("VsanSpaceReportSystem.querySpaceUsage");
            capacitySystem.querySpaceUsage(this.clusterRef, (ProfileSpec[])null, (Boolean)null, this.vsanSpaceUsageFuture);
         } else {
            this.future = this.measure.newFuture("VsanSpaceReportSystem.queryVsanManagedStorageSpaceUsage");
            capacitySystem.queryVsanManagedStorageSpaceUsage(this.clusterRef, this.getSpaceUsageSpec(), this.future);
         }
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

   public VsanSpaceUsageWithDatastoreType[] prepareResult() throws ExecutionException, InterruptedException {
      VsanSpaceUsageWithDatastoreType[] datastoresSpaceUsage;
      if (this.loadVsanDataOnly()) {
         VsanSpaceUsageWithDatastoreType vsanDatastoreSpaceUsage = new VsanSpaceUsageWithDatastoreType();
         vsanDatastoreSpaceUsage.datastoreType = VsanDatastoreType.vsan.name();
         vsanDatastoreSpaceUsage.spaceUsage = (VsanSpaceUsage)this.vsanSpaceUsageFuture.get();
         datastoresSpaceUsage = new VsanSpaceUsageWithDatastoreType[]{vsanDatastoreSpaceUsage};
      } else {
         datastoresSpaceUsage = (VsanSpaceUsageWithDatastoreType[])this.future.get();
      }

      return datastoresSpaceUsage;
   }

   private boolean loadVsanDataOnly() {
      return this.datastoreType == DatastoreType.VSAN || !this.queryVsanManagedStorageSpaceUsageIsSupported();
   }

   private boolean queryVsanManagedStorageSpaceUsageIsSupported() {
      return VsanCapabilityUtils.isManagedVmfsSupportedOnVC(this.clusterRef);
   }

   private QueryVsanManagedStorageSpaceUsageSpec getSpaceUsageSpec() {
      String[] supportedTypes = DatastoreType.getSupportedTypes(this.clusterRef);
      if (this.datastoreType == null) {
         return new QueryVsanManagedStorageSpaceUsageSpec(supportedTypes);
      } else if (Arrays.asList(supportedTypes).contains(this.datastoreType.getKey())) {
         return new QueryVsanManagedStorageSpaceUsageSpec(new String[]{this.datastoreType.getKey()});
      } else {
         throw new IllegalArgumentException(String.format("Requested datastore type '%s' is not supported!", this.datastoreType.getKey()));
      }
   }
}
