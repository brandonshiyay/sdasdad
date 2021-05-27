package com.vmware.vsan.client.services.health;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PurgeInaccessibleVmSwapObjectsProvider {
   private static final Log logger = LogFactory.getLog(PurgeInaccessibleVmSwapObjectsProvider.class);
   private static final VsanProfiler profiler = new VsanProfiler(PurgeInaccessibleVmSwapObjectsProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public String[] getInaccessibleVmSwapObjects(ManagedObjectReference clusterRef) {
      this.validateIfApiIsSupported(clusterRef);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      String[] var8;
      try {
         VsanObjectSystem vsanObjectSystem = conn.getVsanObjectSystem();
         VsanProfiler.Point p = profiler.point("vsanObjectSystem.queryInaccessibleVmSwapObjects");
         Throwable var6 = null;

         try {
            String[] inaccessibleSwapObjects = vsanObjectSystem.queryInaccessibleVmSwapObjects(clusterRef);
            var8 = inaccessibleSwapObjects != null ? inaccessibleSwapObjects : new String[0];
         } catch (Throwable var31) {
            var6 = var31;
            throw var31;
         } finally {
            if (p != null) {
               if (var6 != null) {
                  try {
                     p.close();
                  } catch (Throwable var30) {
                     var6.addSuppressed(var30);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var33) {
         var3 = var33;
         throw var33;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var29) {
                  var3.addSuppressed(var29);
               }
            } else {
               conn.close();
            }
         }

      }

      return var8;
   }

   @TsService
   public ManagedObjectReference purgeInaccessibleVmSwapObjects(ManagedObjectReference clusterRef, String[] objUuids) throws Exception {
      this.validateIfApiIsSupported(clusterRef);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanObjectSystem vsanObjectSystem = conn.getVsanObjectSystem();
         VsanProfiler.Point p = profiler.point("vsanObjectSystem.DeleteObjects");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = vsanObjectSystem.deleteObjects(clusterRef, objUuids, true);
            var9 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
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

   private void validateIfApiIsSupported(ManagedObjectReference clusterRef) {
      if (!VsanCapabilityUtils.isPurgeInaccessibleVmSwapObjectsSupported(clusterRef)) {
         throw new VsanUiLocalizableException("vsan.common.error.notSupported");
      }
   }
}
