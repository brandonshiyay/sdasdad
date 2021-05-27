package com.vmware.vsan.client.services.common.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice.FileBackingInfo;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk.FlatVer2BackingInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.PropertyValue;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class VmData extends BasicVmData {
   public Map uuidToVirtualDiskMap;
   public Map uuidToDiskSnapshotMap;
   public Map backingInfoToDiskMap;
   public String vmPathUuid;
   public Object namespaceCapabilityMetadata;

   public VmData(ManagedObjectReference vmRef) {
      super(vmRef);
   }

   public void updateVmData(PropertyValue propValue) {
      if (propValue != null && propValue.value != null) {
         String var2 = propValue.propertyName;
         byte var3 = -1;
         switch(var2.hashCode()) {
         case -1099694814:
            if (var2.equals("namespaceCapabilityMetadata")) {
               var3 = 4;
            }
            break;
         case -826278890:
            if (var2.equals("primaryIconId")) {
               var3 = 1;
            }
            break;
         case -637434256:
            if (var2.equals("config.hardware.device")) {
               var3 = 2;
            }
            break;
         case 3373707:
            if (var2.equals("name")) {
               var3 = 0;
            }
            break;
         case 814403083:
            if (var2.equals("summary.config.vmPathName")) {
               var3 = 3;
            }
         }

         switch(var3) {
         case 0:
            this.name = (String)propValue.value;
            break;
         case 1:
            this.primaryIconId = (String)propValue.value;
            break;
         case 2:
            this.setVirtualDiskMaps((VirtualDevice[])((VirtualDevice[])propValue.value));
            break;
         case 3:
            this.vmPathUuid = this.getVmHomeVsanUuid((String)propValue.value);
            break;
         case 4:
            this.namespaceCapabilityMetadata = propValue.value;
         }

      }
   }

   private String getVmHomeVsanUuid(String vmFilePath) {
      if (vmFilePath == null) {
         return null;
      } else {
         int startIndex = vmFilePath.indexOf(93);
         int endIndex = vmFilePath.indexOf(47);
         return startIndex >= 0 && endIndex > startIndex ? vmFilePath.substring(startIndex + 1, endIndex).trim() : null;
      }
   }

   public void setVirtualDiskMaps(VirtualDevice[] virtualDevices) {
      if (virtualDevices != null && virtualDevices.length != 0) {
         Map uuidToVirtualDiskMap = new HashMap();
         Map uuidToDiskSnapshotMap = new HashMap();
         Map backingInfoToDiskMap = new HashMap();
         VirtualDevice[] var5 = virtualDevices;
         int var6 = virtualDevices.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            VirtualDevice device = var5[var7];
            if (device instanceof VirtualDisk) {
               VirtualDisk disk = (VirtualDisk)device;
               if (disk.backing != null && disk.backing instanceof FileBackingInfo) {
                  FileBackingInfo fileBackingInfo = (FileBackingInfo)disk.backing;
                  if (StringUtils.isEmpty(fileBackingInfo.backingObjectId)) {
                     continue;
                  }

                  uuidToVirtualDiskMap.put(fileBackingInfo.backingObjectId, disk);
               }

               Object parentBackingInfoObject = getParentVirtualDiskBacking(disk);
               if (parentBackingInfoObject instanceof FlatVer2BackingInfo) {
                  for(FlatVer2BackingInfo parentBackingInfo = (FlatVer2BackingInfo)parentBackingInfoObject; parentBackingInfo != null; parentBackingInfo = parentBackingInfo.parent) {
                     uuidToDiskSnapshotMap.put(parentBackingInfo.backingObjectId, parentBackingInfo);
                     backingInfoToDiskMap.put(parentBackingInfo, disk);
                  }
               }
            }
         }

         this.uuidToDiskSnapshotMap = uuidToDiskSnapshotMap;
         this.backingInfoToDiskMap = backingInfoToDiskMap;
         this.uuidToVirtualDiskMap = uuidToVirtualDiskMap;
      }
   }

   private static Object getParentVirtualDiskBacking(VirtualDisk disk) {
      return disk.backing != null && disk.backing instanceof FlatVer2BackingInfo ? ((FlatVer2BackingInfo)disk.backing).parent : null;
   }

   public String getSnapshotName(String uuid) {
      FlatVer2BackingInfo parentBackingInfo = (FlatVer2BackingInfo)this.uuidToDiskSnapshotMap.get(uuid);
      VirtualDisk disk = (VirtualDisk)this.backingInfoToDiskMap.get(parentBackingInfo);
      return disk == null ? "" : String.format("%s - %s", disk.deviceInfo.label, getVirtualDiskFileName(parentBackingInfo.fileName));
   }

   private static String getVirtualDiskFileName(String filePath) {
      if (!StringUtils.isEmpty(filePath)) {
         String[] splittedPath = filePath.split("/");
         if (!ArrayUtils.isEmpty(splittedPath)) {
            return StringUtils.trim(splittedPath[splittedPath.length - 1]);
         }
      }

      return "";
   }
}
