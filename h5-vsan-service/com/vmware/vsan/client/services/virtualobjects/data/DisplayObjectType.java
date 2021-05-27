package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;

@TsModel
public enum DisplayObjectType {
   VM,
   FOLDER,
   DISK,
   CNS_VOLUME,
   EXTENSION_APP,
   VM_SWAP,
   VM_MEMORY,
   ISCSI_TARGET,
   ISCSI_LUN,
   FILE_SHARE,
   FILE_SHARE_OBJECT,
   FILE_VOLUME,
   FILE_VOLUME_OBJECT,
   REPLICATION,
   FCD_DISK,
   OTHER;

   public static DisplayObjectType fromVmodlType(VsanObjectType vmodlType) {
      switch(vmodlType) {
      case improvedVirtualDisk:
         return FCD_DISK;
      case attachedCnsVolBlock:
      case detachedCnsVolBlock:
      case extension:
         return CNS_VOLUME;
      case vdisk:
      case hbrDisk:
         return DISK;
      case vmswap:
         return VM_SWAP;
      case vmem:
         return VM_MEMORY;
      case hbrPersist:
      case namespace:
         return FOLDER;
      case iscsiTarget:
         return ISCSI_TARGET;
      case iscsiLun:
         return ISCSI_LUN;
      case fileShare:
         return FILE_SHARE;
      case cnsVolFile:
         return FILE_VOLUME;
      default:
         return OTHER;
      }
   }
}
