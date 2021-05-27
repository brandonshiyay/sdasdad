package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VSANWitnessHostInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class WitnessHostsDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(WitnessHostsDataRetriever.class);
   private Future witnessHostsFuture;

   public WitnessHostsDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
   }

   public void start() {
      this.witnessHostsFuture = this.measure.newFuture("VsanVcStretchedClusterSystem.getWitnessHosts");

      try {
         VsanConnection connection = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
         Throwable var2 = null;

         try {
            VsanVcStretchedClusterSystem stretchedClusterSystem = connection.getVcStretchedClusterSystem();
            stretchedClusterSystem.getWitnessHosts(this.clusterRef, this.witnessHostsFuture);
         } catch (Throwable var12) {
            var2 = var12;
            throw var12;
         } finally {
            if (connection != null) {
               if (var2 != null) {
                  try {
                     connection.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  connection.close();
               }
            }

         }

      } catch (Exception var14) {
         throw new VsanUiLocalizableException("vsan.guardRail.witnessHost.error");
      }
   }

   public VSANWitnessHostInfo[] prepareResult() {
      try {
         VSANWitnessHostInfo[] witnessHostInfos = (VSANWitnessHostInfo[])this.witnessHostsFuture.get();
         if (witnessHostInfos == null) {
            return null;
         } else {
            VSANWitnessHostInfo[] var2 = witnessHostInfos;
            int var3 = witnessHostInfos.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               VSANWitnessHostInfo witnessHostInfo = var2[var4];
               VmodlHelper.assignServerGuid(witnessHostInfo.host, this.clusterRef.getServerGuid());
            }

            return witnessHostInfos;
         }
      } catch (Exception var6) {
         logger.error("Failed to get witness hosts." + var6);
         return null;
      }
   }
}
