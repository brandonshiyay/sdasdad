package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import com.vmware.vsan.client.services.common.data.VmData;
import com.vmware.vsphere.client.vsan.base.data.VsanObject;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import java.util.Map;

@TsModel
public class VsanVmObject extends VsanObject {
   public String description;
   public ManagedObjectReference vmRef;
   public String vmName;
   public String vmPrimaryIconId;
   public Object namespaceCapabilityMetadata;

   public VsanVmObject() {
   }

   public VsanVmObject(VsanObjectIdentity objectIdentity) {
      this.objectType = VsanObjectType.parse(objectIdentity.type);
      this.description = objectIdentity.description;
      this.vmRef = objectIdentity.vm;
      this.vsanObjectUuid = objectIdentity.uuid;
      if (this.objectType.equals(VsanObjectType.other)) {
         this.name = this.description;
      }

   }

   public VsanVmObject(VsanObjectType objectType, String objectUuid, ManagedObjectReference vmRef) {
      this.objectType = objectType;
      this.vmRef = vmRef;
      this.vsanObjectUuid = objectUuid;
   }

   public VsanVmObject(VsanObjectIdentity objectData, VmData vmData) {
      this(objectData);
      this.vmName = vmData.name;
      this.vmPrimaryIconId = vmData.primaryIconId;
      this.namespaceCapabilityMetadata = vmData.namespaceCapabilityMetadata;
      if (this.objectType.equals(VsanObjectType.vdisk)) {
         if (vmData.uuidToVirtualDiskMap.containsKey(this.vsanObjectUuid)) {
            VirtualDisk disk = (VirtualDisk)vmData.uuidToVirtualDiskMap.get(this.vsanObjectUuid);
            this.name = disk.deviceInfo.label;
         } else if (vmData.uuidToDiskSnapshotMap.containsKey(this.vsanObjectUuid)) {
            this.name = vmData.getSnapshotName(this.vsanObjectUuid);
            this.objectType = VsanObjectType.vdiskSnapshot;
         }
      }

   }

   public void updateHealthData(Map vsanHealthData) {
      if (vsanHealthData.containsKey(this.vsanObjectUuid)) {
         VsanObjectHealthData vsanHealthInfo = (VsanObjectHealthData)vsanHealthData.get(this.vsanObjectUuid);
         super.updateHealthData(vsanHealthData);
      }
   }
}
