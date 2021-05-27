package com.vmware.vsan.client.services.encryption;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.data.EncryptionRekeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncryptionMutationProvider {
   private static final VsanProfiler _profiler = new VsanProfiler(EncryptionMutationProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public ManagedObjectReference rekeyEncryptedCluster(ManagedObjectReference clusterRef, EncryptionRekeySpec spec) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
         VsanProfiler.Point point = _profiler.point("vsanConfigSystem.rekeyEncryptedCluster");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = vsanConfigSystem.rekeyEncryptedCluster(clusterRef, spec.reEncryptData, spec.allowReducedRedundancy);
            VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            var9 = taskRef;
         } catch (Throwable var32) {
            var7 = var32;
            throw var32;
         } finally {
            if (point != null) {
               if (var7 != null) {
                  try {
                     point.close();
                  } catch (Throwable var31) {
                     var7.addSuppressed(var31);
                  }
               } else {
                  point.close();
               }
            }

         }
      } catch (Throwable var34) {
         var4 = var34;
         throw var34;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var30) {
                  var4.addSuppressed(var30);
               }
            } else {
               conn.close();
            }
         }

      }

      return var9;
   }
}
