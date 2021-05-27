package com.vmware.vsan.client.services.vum;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVumSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VumLoginService {
   private static final VsanProfiler _profiler = new VsanProfiler(VumLoginService.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public boolean loginToVum(ManagedObjectReference clusterRef, String username, String password) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      Throwable var9;
      try {
         VsanVumSystem vumSystem = conn.getVsanVumSystem();
         VsanProfiler.Point p = _profiler.point("VsanVumSystem.fetchIsoDepotCookie");
         Throwable var8 = null;

         try {
            vumSystem.fetchIsoDepotCookie(username, password);
            var9 = true;
         } catch (Throwable var32) {
            var9 = var32;
            var8 = var32;
            throw var32;
         } finally {
            if (p != null) {
               if (var8 != null) {
                  try {
                     p.close();
                  } catch (Throwable var31) {
                     var8.addSuppressed(var31);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var34) {
         var5 = var34;
         throw var34;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var30) {
                  var5.addSuppressed(var30);
               }
            } else {
               conn.close();
            }
         }

      }

      return (boolean)var9;
   }
}
