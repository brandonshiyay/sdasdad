package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ClusterRuntimeInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class AssignedSharedWitnessClustersDataRetriever extends AbstractAsyncDataRetriever {
   private Map hostFutures = new HashMap();
   private final VsanClient vsanClient;
   private final List hosts;

   public AssignedSharedWitnessClustersDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient, List hosts) {
      super(clusterRef, measure);
      this.vsanClient = vsanClient;
      this.hosts = hosts;
   }

   public void start() {
      ManagedObjectReference hostRef;
      Future hostFuture;
      for(Iterator var1 = this.hosts.iterator(); var1.hasNext(); this.hostFutures.put(hostRef, hostFuture)) {
         hostRef = (ManagedObjectReference)var1.next();
         hostFuture = this.measure.newFuture("VsanStretchedClusterSystem.queryWitnessHostClusterInfo[" + hostRef + "]");
         if (VsanCapabilityUtils.isSharedWitnessSupported(hostRef) && VsanCapabilityUtils.isSharedWitnessSupportedOnVc(hostRef)) {
            VsanConnection vsanConnection = this.vsanClient.getConnection(hostRef.getServerGuid());
            Throwable var5 = null;

            try {
               VsanVcStretchedClusterSystem vsanStretchedClusterSystem = vsanConnection.getVcStretchedClusterSystem();
               vsanStretchedClusterSystem.queryWitnessHostClusterInfo(hostRef, (Boolean)null, hostFuture);
            } catch (Throwable var14) {
               var5 = var14;
               throw var14;
            } finally {
               if (vsanConnection != null) {
                  if (var5 != null) {
                     try {
                        vsanConnection.close();
                     } catch (Throwable var13) {
                        var5.addSuppressed(var13);
                     }
                  } else {
                     vsanConnection.close();
                  }
               }

            }
         } else {
            hostFuture = null;
         }
      }

   }

   public Map prepareResult() throws ExecutionException, InterruptedException {
      Map hostToAssignedClustersInfo = new HashMap();
      Iterator var2 = this.hostFutures.keySet().iterator();

      while(var2.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();
         Future hostFuture = (Future)this.hostFutures.get(hostRef);
         if (hostFuture == null) {
            hostToAssignedClustersInfo.put(hostRef, (Object)null);
         } else {
            ClusterRuntimeInfo[] assignedClustersInfo = (ClusterRuntimeInfo[])hostFuture.get();
            hostToAssignedClustersInfo.put(hostRef, assignedClustersInfo == null ? Collections.emptyList() : Arrays.asList(assignedClustersInfo));
         }
      }

      return hostToAssignedClustersInfo;
   }
}
