package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.host.VsanSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HostConfigDataRetriever extends AbstractHostAsyncDataProvider {
   private static final Log logger = LogFactory.getLog(IsWitnessHostDataRetriever.class);
   private VcClient vcClient;

   public HostConfigDataRetriever(ManagedObjectReference hostRef, Measure measure, VcClient vcClient) {
      super(hostRef, measure);
      this.vcClient = vcClient;
   }

   public void start() {
      this.future = this.measure.newFuture("VsanSystem.getConfig");

      try {
         VcConnection conn = this.vcClient.getConnection(this.hostRef.getServerGuid());
         Throwable var2 = null;

         try {
            VsanSystem vsanSystem = conn.getHostVsanSystem(this.hostRef);
            vsanSystem.getConfig(this.future);
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
         this.future.set((Object)null);
         logger.error("Cannot retrieve host's configuration from VsanSystem: " + this.hostRef.toString(), var14);
      }

   }
}
