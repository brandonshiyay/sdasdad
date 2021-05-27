package com.vmware.vsan.client.services.diskplacement;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.vm.ConfigInfo;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice.FileBackingInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsService;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VmDiskPlacementProvider {
   @Autowired
   private VirtualObjectsService virtualObjectsService;

   @TsService
   public List getVmVirtualObjects(ManagedObjectReference storageCluster, ManagedObjectReference vmRef, Set vmObjectUuids) {
      Validate.notNull(vmRef);
      Validate.notNull(storageCluster);

      try {
         return this.virtualObjectsService.listVmVirtualObjects(storageCluster, vmRef, vmObjectUuids);
      } catch (Exception var5) {
         throw new VsanUiLocalizableException(var5);
      }
   }

   @TsService
   public Set getVmObjectUuids(ManagedObjectReference vmRef) {
      try {
         Measure rootMeasure = new Measure("VmDiskPlacementProvider.getVmObjectUuids");
         Throwable var3 = null;

         try {
            Measure dsMeasure = rootMeasure.start("DataService[" + vmRef + "] - VM props");
            Throwable var6 = null;

            DataServiceResponse vmProps;
            try {
               vmProps = QueryUtil.getProperties(vmRef, new String[]{"config.hardware.device", "config.vmStorageObjectId", "config.swapStorageObjectId"});
            } catch (Throwable var33) {
               var6 = var33;
               throw var33;
            } finally {
               if (dsMeasure != null) {
                  if (var6 != null) {
                     try {
                        dsMeasure.close();
                     } catch (Throwable var32) {
                        var6.addSuppressed(var32);
                     }
                  } else {
                     dsMeasure.close();
                  }
               }

            }

            Set vmObjectUuids = new HashSet();
            String vmHomeObjectUuid = (String)vmProps.getProperty(vmRef, "config.vmStorageObjectId");
            vmObjectUuids.add(vmHomeObjectUuid);
            String vmSwapObjectUuid = (String)vmProps.getProperty(vmRef, "config.swapStorageObjectId");
            if (StringUtils.isNotEmpty(vmSwapObjectUuid)) {
               vmObjectUuids.add(vmSwapObjectUuid);
            }

            VirtualDevice[] vmDevices = (VirtualDevice[])vmProps.getProperty(vmRef, "config.hardware.device");
            vmObjectUuids.addAll(this.getVmDiskObjectUuids(vmDevices));
            Collection configSnapshots = this.virtualObjectsService.listVmSnapshots(vmRef, rootMeasure);
            Iterator var10 = configSnapshots.iterator();

            while(var10.hasNext()) {
               ConfigInfo configSnapshot = (ConfigInfo)var10.next();
               vmObjectUuids.addAll(this.getVmDiskObjectUuids(configSnapshot.hardware.device));
            }

            HashSet var40 = vmObjectUuids;
            return var40;
         } catch (Throwable var35) {
            var3 = var35;
            throw var35;
         } finally {
            if (rootMeasure != null) {
               if (var3 != null) {
                  try {
                     rootMeasure.close();
                  } catch (Throwable var31) {
                     var3.addSuppressed(var31);
                  }
               } else {
                  rootMeasure.close();
               }
            }

         }
      } catch (Exception var37) {
         throw new VsanUiLocalizableException(var37);
      }
   }

   private Set getVmDiskObjectUuids(VirtualDevice[] vmDevices) {
      Set result = new HashSet();
      VirtualDevice[] var3 = vmDevices;
      int var4 = vmDevices.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         VirtualDevice device = var3[var5];
         if (device instanceof VirtualDisk) {
            VirtualDisk disk = (VirtualDisk)device;
            if (disk.backing != null && disk.backing instanceof FileBackingInfo) {
               FileBackingInfo fileBackingInfo = (FileBackingInfo)disk.backing;
               if (!StringUtils.isEmpty(fileBackingInfo.backingObjectId)) {
                  result.add(fileBackingInfo.backingObjectId);
               }
            }
         }
      }

      return result;
   }
}
