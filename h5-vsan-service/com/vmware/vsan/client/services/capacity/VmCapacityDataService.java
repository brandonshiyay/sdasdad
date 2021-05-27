package com.vmware.vsan.client.services.capacity;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanEntitySpaceUsage;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceReportSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceReportingEntityType;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capacity.model.VmCapacityData;
import com.vmware.vsan.client.services.csd.CsdVmService;
import com.vmware.vsan.client.services.csd.model.VmCsdConfig;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VmCapacityDataService {
   private static final Log logger = LogFactory.getLog(VmCapacityDataService.class);
   @Autowired
   VsanClient vsanClient;
   @Autowired
   CsdVmService csdVmService;

   @TsService
   public VmCapacityData getVmSpaceUsage(ManagedObjectReference vmRef) {
      VmCsdConfig vmCsdConfig = this.csdVmService.getVmCsdConfig(vmRef);
      if (vmCsdConfig.hasStorageClusterReadAccess && !vmCsdConfig.storageClusters.isEmpty()) {
         if (vmCsdConfig.storageClusters.size() > 1) {
            throw new VsanUiLocalizableException("vsan.csd.error.multipleStorageClusters");
         } else {
            ManagedObjectReference clusterRef = ((InventoryNode)vmCsdConfig.storageClusters.iterator().next()).moRef;
            VsanSpaceQuerySpec vmSpaceQuerySpec = new VsanSpaceQuerySpec(VsanSpaceReportingEntityType.VM.toString(), new String[]{vmRef.getValue()});

            try {
               VsanConnection vsanConnection = this.vsanClient.getConnection(clusterRef.getServerGuid());
               Throwable var9 = null;

               VmCapacityData var43;
               try {
                  VsanSpaceReportSystem capacitySystem = vsanConnection.getVsanSpaceReportSystem();
                  Measure measure = new Measure("Query VM space capacity");
                  Throwable var12 = null;

                  long totalVmCapacity;
                  VsanEntitySpaceUsage[] vmSpaceUsages;
                  try {
                     Future vmSpaceUsageFuture = measure.newFuture("VsanSpaceReportSystem.queryEntitySpaceUsage");
                     capacitySystem.queryEntitySpaceUsage(clusterRef, vmSpaceQuerySpec, vmSpaceUsageFuture);
                     totalVmCapacity = this.getVmTotalDiskSize(vmRef);
                     vmSpaceUsages = (VsanEntitySpaceUsage[])vmSpaceUsageFuture.get();
                  } catch (Throwable var37) {
                     var12 = var37;
                     throw var37;
                  } finally {
                     if (measure != null) {
                        if (var12 != null) {
                           try {
                              measure.close();
                           } catch (Throwable var36) {
                              var12.addSuppressed(var36);
                           }
                        } else {
                           measure.close();
                        }
                     }

                  }

                  if (ArrayUtils.isEmpty(vmSpaceUsages) || ArrayUtils.isEmpty(vmSpaceUsages[0].spaceUsageByObjectType)) {
                     logger.error("Unable to fetch VM capacity data. No result returned by queryEntitySpaceUsage");
                     throw new VsanUiLocalizableException("vsan.common.generic.error");
                  }

                  VmCapacityData vmCapacityData = (new VsanCapacityBreakdownCalculator(vmSpaceUsages[0].spaceUsageByObjectType)).calculateVmCapacityData();
                  vmCapacityData.totalVmCapacity = totalVmCapacity;
                  var43 = vmCapacityData;
               } catch (Throwable var39) {
                  var9 = var39;
                  throw var39;
               } finally {
                  if (vsanConnection != null) {
                     if (var9 != null) {
                        try {
                           vsanConnection.close();
                        } catch (Throwable var35) {
                           var9.addSuppressed(var35);
                        }
                     } else {
                        vsanConnection.close();
                     }
                  }

               }

               return var43;
            } catch (Exception var41) {
               logger.error("Unable to fetch VM capacity data.", var41);
               throw new VsanUiLocalizableException("vsan.common.generic.error");
            }
         }
      } else {
         throw new VsanUiLocalizableException("vsan.csd.error.noStorageClusterAccess");
      }
   }

   private long getVmTotalDiskSize(ManagedObjectReference vmRef) {
      try {
         long result = 0L;
         VirtualDevice[] devices = (VirtualDevice[])QueryUtil.getProperty(vmRef, "config.hardware.device");
         VirtualDevice[] var5 = devices;
         int var6 = devices.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            VirtualDevice device = var5[var7];
            if (device instanceof VirtualDisk) {
               VirtualDisk disk = (VirtualDisk)device;
               result += disk.capacityInBytes;
            }
         }

         return result;
      } catch (Exception var10) {
         logger.error("Unable to get VM's disks to determine total disks capacity");
         throw new VsanUiLocalizableException(var10);
      }
   }
}
