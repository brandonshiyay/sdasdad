package com.vmware.vsan.client.services.health;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelayoutVsanObjectsProvider {
   private static final Log logger = LogFactory.getLog(RelayoutVsanObjectsProvider.class);
   private static final VsanProfiler profiler = new VsanProfiler(RelayoutVsanObjectsProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public ManagedObjectReference relayoutObjects(ManagedObjectReference clusterRef) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         ManagedObjectReference var8;
         try {
            VsanObjectSystem vsanObjectSystem = conn.getVsanObjectSystem();
            VsanProfiler.Point p = profiler.point("vsanObjectSystem.relayoutObjects");
            Throwable var6 = null;

            try {
               ManagedObjectReference taskRef = vsanObjectSystem.relayoutObjects(clusterRef);
               var8 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            } catch (Throwable var33) {
               var6 = var33;
               throw var33;
            } finally {
               if (p != null) {
                  if (var6 != null) {
                     try {
                        p.close();
                     } catch (Throwable var32) {
                        var6.addSuppressed(var32);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Throwable var35) {
            var3 = var35;
            throw var35;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var31) {
                     var3.addSuppressed(var31);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var8;
      } catch (Exception var37) {
         logger.error("Unable to start relayout vSAN objects task.");
         throw new VsanUiLocalizableException("vsan.common.generic.operation.error", var37);
      }
   }
}
