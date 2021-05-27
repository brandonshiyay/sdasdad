package com.vmware.vsan.client.services.networkdiagnostics;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDiagnosticsSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDiagnosticsThreshold;
import com.vmware.vim.vsan.binding.vim.cluster.VsanNetworkDiagnostics;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.networkdiagnostics.model.NetworkDiagnostic;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.perf.model.PerfGraphThreshold;
import com.vmware.vsphere.client.vsan.perf.model.PerfGraphThresholdDirection;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NetworkDiagnosticsService {
   private static final Logger logger = LoggerFactory.getLogger(NetworkDiagnosticsService.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public List getNetworkDiagnostics(ManagedObjectReference hostRef) {
      ArrayList result = new ArrayList();

      VsanNetworkDiagnostics[] vsanNetworkDiagnostics;
      try {
         VsanConnection connection = this.vsanClient.getConnection(hostRef.getServerGuid());
         Throwable var5 = null;

         try {
            ManagedObjectReference parentCluster = BaseUtils.getCluster(hostRef);
            VsanDiagnosticsSystem diagnosticsSystem = connection.getDiagnosticsSystem();
            vsanNetworkDiagnostics = diagnosticsSystem.queryNetworkDiagnostics(parentCluster, hostRef);
         } catch (Throwable var16) {
            var5 = var16;
            throw var16;
         } finally {
            if (connection != null) {
               if (var5 != null) {
                  try {
                     connection.close();
                  } catch (Throwable var15) {
                     var5.addSuppressed(var15);
                  }
               } else {
                  connection.close();
               }
            }

         }
      } catch (Exception var18) {
         logger.error("Failed to retrieve network diagnostics: ", var18);
         throw new VsanUiLocalizableException("vsan.common.generic.error");
      }

      if (vsanNetworkDiagnostics == null) {
         return result;
      } else {
         VsanNetworkDiagnostics[] var19 = vsanNetworkDiagnostics;
         int var20 = vsanNetworkDiagnostics.length;

         for(int var21 = 0; var21 < var20; ++var21) {
            VsanNetworkDiagnostics vsanNetworkDiagnostic = var19[var21];
            result.add(NetworkDiagnostic.create(vsanNetworkDiagnostic));
         }

         return result;
      }
   }

   @TsService
   public List getPerfGraphThresholds(ManagedObjectReference hostRef, String entityType) {
      List result = new ArrayList();
      if (!VsanCapabilityUtils.isNetworkDiagnosticsSupportedOnVc(hostRef)) {
         return result;
      } else {
         VsanDiagnosticsThreshold[] thresholds;
         try {
            VsanConnection connection = this.vsanClient.getConnection(hostRef.getServerGuid());
            Throwable var6 = null;

            try {
               ManagedObjectReference parentCluster = BaseUtils.getCluster(hostRef);
               VsanDiagnosticsSystem diagnosticsSystem = connection.getDiagnosticsSystem();
               thresholds = diagnosticsSystem.getThresholds(parentCluster, entityType, (String)null);
            } catch (Throwable var17) {
               var6 = var17;
               throw var17;
            } finally {
               if (connection != null) {
                  if (var6 != null) {
                     try {
                        connection.close();
                     } catch (Throwable var16) {
                        var6.addSuppressed(var16);
                     }
                  } else {
                     connection.close();
                  }
               }

            }
         } catch (Exception var19) {
            logger.error("Failed to retrieve perf graph thresholds: ", var19);
            throw new VsanUiLocalizableException("vsan.common.generic.error");
         }

         VsanDiagnosticsThreshold[] var20 = thresholds;
         int var21 = thresholds.length;

         for(int var22 = 0; var22 < var21; ++var22) {
            VsanDiagnosticsThreshold vsanDiagnosticsThreshold = var20[var22];
            PerfGraphThreshold perfGraphThreshold = PerfGraphThreshold.create(vsanDiagnosticsThreshold);
            perfGraphThreshold.direction = PerfGraphThresholdDirection.upper;
            result.add(perfGraphThreshold);
         }

         return result;
      }
   }

   @TsService
   public void setPerfGraphThreshold(ManagedObjectReference hostRef, PerfGraphThreshold perfGraphThreshold) {
      try {
         VsanConnection connection = this.vsanClient.getConnection(hostRef.getServerGuid());
         Throwable var4 = null;

         try {
            ManagedObjectReference parentCluster = BaseUtils.getCluster(hostRef);
            VsanDiagnosticsSystem diagnosticsSystem = connection.getDiagnosticsSystem();
            VsanDiagnosticsThreshold threshold = PerfGraphThreshold.toVsanDiagnosticsThreshold(perfGraphThreshold);
            diagnosticsSystem.setThresholds(parentCluster, new VsanDiagnosticsThreshold[]{threshold});
         } catch (Throwable var16) {
            var4 = var16;
            throw var16;
         } finally {
            if (connection != null) {
               if (var4 != null) {
                  try {
                     connection.close();
                  } catch (Throwable var15) {
                     var4.addSuppressed(var15);
                  }
               } else {
                  connection.close();
               }
            }

         }

      } catch (Exception var18) {
         logger.error("Failed to update vsan diagnostics threshold: ", var18);
         throw new VsanUiLocalizableException("vsan.common.generic.error");
      }
   }
}
