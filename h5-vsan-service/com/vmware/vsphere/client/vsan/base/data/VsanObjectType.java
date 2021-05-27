package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

@TsModel
public enum VsanObjectType {
   vmswap,
   vdisk,
   namespace,
   vmem,
   statsdb,
   iscsiTarget,
   iscsiLun,
   iscsiHome,
   fileSystemOverhead,
   dedupOverhead,
   spaceUnderDedupConsideration,
   checksumOverhead,
   improvedVirtualDisk,
   transientSpace,
   physicalTransientSpace,
   minSpaceRequiredForVsanOp,
   hostRebuildCapacity,
   fileShare,
   fileServiceRoot,
   attachedCnsVolBlock,
   detachedCnsVolBlock,
   cnsVolFile,
   extension,
   hbrDisk,
   hbrPersist,
   hbrCfg,
   haMetadataObject,
   other,
   vdiskSnapshot;

   public static VsanObjectType parse(String value) {
      return (VsanObjectType)EnumUtils.fromString(VsanObjectType.class, value, other);
   }
}
