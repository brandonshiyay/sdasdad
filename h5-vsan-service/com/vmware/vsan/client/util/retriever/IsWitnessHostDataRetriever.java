package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IsWitnessHostDataRetriever extends AbstractHostAsyncDataProvider {
   private static final Log logger = LogFactory.getLog(IsWitnessHostDataRetriever.class);

   public IsWitnessHostDataRetriever(ManagedObjectReference hostRef, Measure measure, VsanClient vsanClient) {
      super(hostRef, measure, vsanClient);
   }

   public void start() {
      this.future = this.measure.newFuture("VsanVcStretchedClusterSystem.isWitnessHost");

      try {
         VsanConnection conn = this.vsanClient.getConnection(this.hostRef.getServerGuid());
         Throwable var2 = null;

         try {
            VsanVcStretchedClusterSystem vsanStretchedClusterSystem = conn.getVcStretchedClusterSystem();
            vsanStretchedClusterSystem.isWitnessHost(this.hostRef, this.future);
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
      } catch (Exception var14) {
         this.future.set(false);
         logger.error("Unable to determine if the host is a witness: " + this.hostRef.toString(), var14);
      }

   }
}
