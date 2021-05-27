package com.vmware.vsphere.client.vsan.upgrade;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.VsanUpgradeSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanUpgradeSystemEx;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDiskFormatConversionSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanUpgradeMutationProvider {
   public static final String TASK_TYPE = "Task";
   private static final Log _logger = LogFactory.getLog(VsanUpgradeMutationProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanUpgradeMutationProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public ManagedObjectReference performUpgrade(ManagedObjectReference clusterRef, VsanUpgradeSpec spec) throws Exception {
      boolean isUpgradeSystem2Supported = VsanCapabilityUtils.isUpgradeSystem2SupportedOnVc(clusterRef);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      ManagedObjectReference var10;
      try {
         VsanUpgradeSystem upgradeSystem = isUpgradeSystem2Supported ? conn.getVsanUpgradeSystem() : conn.getVsanLegacyUpgradeSystem();
         VsanProfiler.Point p = _profiler.point("upgradeSystem.performUpgrade");
         Throwable var8 = null;

         try {
            ManagedObjectReference taskRef = upgradeSystem.performUpgrade(clusterRef, spec.performObjectUpgrade, spec.downgradeFormat, spec.allowReducedRedundancy, (ManagedObjectReference[])null);
            var10 = buildTaskMor(taskRef.getValue(), clusterRef.getServerGuid());
         } catch (Throwable var33) {
            var8 = var33;
            throw var33;
         } finally {
            if (p != null) {
               if (var8 != null) {
                  try {
                     p.close();
                  } catch (Throwable var32) {
                     var8.addSuppressed(var32);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var35) {
         var5 = var35;
         throw var35;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var31) {
                  var5.addSuppressed(var31);
               }
            } else {
               conn.close();
            }
         }

      }

      return var10;
   }

   @TsService
   public ManagedObjectReference performUpgradePreflightAsyncCheck(ManagedObjectReference clusterRef) throws VsanUiLocalizableException {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      ManagedObjectReference var8;
      try {
         VsanUpgradeSystemEx upgradeSystemEx = conn.getVsanUpgradeSystemEx();

         try {
            VsanProfiler.Point p = _profiler.point("upgradeSystemEx.performUpgradePreflightAsyncCheck");
            Throwable var6 = null;

            try {
               ManagedObjectReference taskRef = upgradeSystemEx.performUpgradePreflightAsyncCheck(clusterRef, (Boolean)null, (VsanDiskFormatConversionSpec)null);
               taskRef.setServerGuid(clusterRef.getServerGuid());
               var8 = taskRef;
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
         } catch (Exception var35) {
            _logger.error("Unable to perform upgrade preflight async check: ", var35);
            throw new VsanUiLocalizableException("vsan.manage.diskManagement.upgradeComponent.precheckUpgradeError");
         }
      } catch (Throwable var36) {
         var3 = var36;
         throw var36;
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
   }

   private static ManagedObjectReference buildTaskMor(String taskId, String vcGuid) {
      ManagedObjectReference task = new ManagedObjectReference("Task", taskId, vcGuid);
      return task;
   }
}
