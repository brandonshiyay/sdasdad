package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapability;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapabilitySystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class HostCapabilitiesDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(HostCapabilitiesDataRetriever.class);
   private Future task = null;
   private final List hosts;

   public HostCapabilitiesDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient, List hosts) {
      super(clusterRef, measure, vsanClient);
      this.hosts = hosts;
   }

   public void start() {
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var2 = null;

      try {
         VsanCapabilitySystem capabilitySystem = conn.getVsanCapabilitySystem();
         this.task = this.measure.newFuture("VsanCapability[]");
         capabilitySystem.getCapabilities((ManagedObjectReference[])this.hosts.toArray(new ManagedObjectReference[this.hosts.size()]), this.task);
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

   public VsanCapability[] prepareResult() {
      try {
         return (VsanCapability[])this.task.get();
      } catch (Exception var2) {
         logger.error("Failed to get host capabilities: ", var2);
         return null;
      }
   }
}
