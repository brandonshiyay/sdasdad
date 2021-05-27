package com.vmware.vsphere.client.vsan.support;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.health.util.VsanHealthUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanSupportMutationProvider {
   private static final Log _logger = LogFactory.getLog(VsanSupportMutationProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanSupportMutationProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public ManagedObjectReference attachVsanSupportBundleToSr(ManagedObjectReference clusterRef, VsanSRAttachSpec spec) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         VsanProfiler.Point p = _profiler.point("healthSystem.attachVsanSupportBundleToSr");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = healthSystem.attachVsanSupportBundleToSr(clusterRef, spec.serviceRequestID);
            var9 = VsanHealthUtil.buildTaskMor(taskRef.getValue(), clusterRef.getServerGuid());
         } catch (Throwable var32) {
            var7 = var32;
            throw var32;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var31) {
                     var7.addSuppressed(var31);
                  }
               } else {
                  p.close();
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
