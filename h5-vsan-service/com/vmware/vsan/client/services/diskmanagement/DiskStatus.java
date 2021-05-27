package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.proxygen.ts.TsModel;
import java.util.HashMap;
import java.util.Map;

@TsModel
public class DiskStatus {
   public DiskStatusType type;
   public boolean isDiskHealthy;
   public Map additionalStatuses;

   public DiskStatus() {
   }

   public DiskStatus(DiskStatusType type, boolean isDiskHealthy) {
      this.type = type;
      this.isDiskHealthy = isDiskHealthy;
   }

   public void addStatus(DiskStatusKey key, Object value) {
      if (this.additionalStatuses == null) {
         this.additionalStatuses = new HashMap();
      }

      this.additionalStatuses.put(key, value);
   }

   public static DiskStatus createVsanDiskStatus(boolean isDiskHealthy, Integer healthFlag, Integer formatVersion) {
      DiskStatus diskStatus = new DiskStatus(DiskStatusType.VSAN, isDiskHealthy);
      diskStatus.addStatus(DiskStatusKey.VSAN_HEALTH_FLAG, healthFlag);
      diskStatus.addStatus(DiskStatusKey.VSAN_DISK_FORMAT_VERSION, formatVersion);
      return diskStatus;
   }

   public static DiskStatus createScsiVsanDirectDiskStatus(boolean isDiskHealthy, String operationalState, boolean isMounted) {
      DiskStatus diskStatus = new DiskStatus(DiskStatusType.VSAN_DIRECT_SCSI_LUN, isDiskHealthy);
      diskStatus.addStatus(DiskStatusKey.SCSI_LUN_OPERATIONAL_STATE, operationalState);
      diskStatus.addStatus(DiskStatusKey.IS_MOUNTED, isMounted);
      return diskStatus;
   }

   public static DiskStatus createScsiNotClaimedDiskStatus(boolean isDiskHealthy, String operationalState) {
      DiskStatus diskStatus = new DiskStatus(DiskStatusType.NOT_CLAIMED_SCSI_LUN, isDiskHealthy);
      diskStatus.addStatus(DiskStatusKey.SCSI_LUN_OPERATIONAL_STATE, operationalState);
      return diskStatus;
   }

   public static DiskStatus createPmemStorageStatus(boolean isStorageHealthy, boolean isMounted) {
      DiskStatus storageStatus = new DiskStatus(DiskStatusType.PMEM, isStorageHealthy);
      storageStatus.addStatus(DiskStatusKey.IS_MOUNTED, isMounted);
      return storageStatus;
   }
}
