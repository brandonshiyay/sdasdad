package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.host.ScsiLun.State;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanDirectStorage;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanScsiDisk;
import com.vmware.vsan.client.services.diskmanagement.pmem.PmemStorage;
import com.vmware.vsan.client.util.CMMDSHealthFlags;
import com.vmware.vsphere.client.vsan.health.DatastoreHealthStatus;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskStatusUtil {
   private static final Logger logger = LoggerFactory.getLogger(DiskStatusUtil.class);

   public static DiskStatus getVsanDiskStatus(Integer healthFlag, Integer formatVersion) {
      boolean isDiskHealthy = CMMDSHealthFlags.OK.isEqualTo(healthFlag) || CMMDSHealthFlags.UNKNOWN.isEqualTo(healthFlag);
      return DiskStatus.createVsanDiskStatus(isDiskHealthy, healthFlag, formatVersion);
   }

   public static DiskStatus getVsanDiskStatus(boolean isOperationalHealthy, boolean isMetadataHealthy, boolean isInCmmds, boolean isInVsi, Integer healthFlag, Integer formatVersion) {
      boolean isDiskHealthy = isOperationalHealthy && isMetadataHealthy && isInCmmds && isInVsi;
      return DiskStatus.createVsanDiskStatus(isDiskHealthy, healthFlag, formatVersion);
   }

   public static Map getVsanDirectDiskStatuses(VsanDirectStorage[] vsanDirectDisks) {
      return (Map)(!DiskManagementUtil.hasClaimedDisks(vsanDirectDisks) ? new HashMap() : (Map)Arrays.stream(vsanDirectDisks).flatMap((storage) -> {
         return Arrays.stream(storage.scsiDisks);
      }).collect(HashMap::new, (result, disk) -> {
         DiskStatus var10000 = (DiskStatus)result.put(disk.getUuid(), getVsanDirectDiskStatus(disk));
      }, HashMap::putAll));
   }

   public static DiskStatus getVsanDirectDiskStatus(VsanScsiDisk disk) {
      if (!isOperationalStateValid(disk.operationalState)) {
         logger.warn("Unable to parse the operational state for vSAN Direct disk: " + disk.uuid);
         return null;
      } else {
         String operationalState = disk.operationalState[0];
         boolean isDiskHealthy = isScsiDiskHealthy(operationalState);
         boolean isMounted = disk.mountInfo != null && disk.mountInfo.mounted;
         return DiskStatus.createScsiVsanDirectDiskStatus(isDiskHealthy, operationalState, isMounted);
      }
   }

   public static DiskStatus getNotClaimedDiskStatus(ScsiDisk disk, Exception error) {
      if (!isOperationalStateValid(disk.operationalState)) {
         logger.warn("Unable to parse the operational state for non claimed disk: " + disk.uuid);
         return null;
      } else {
         String operationalState = disk.operationalState[0];
         boolean isDiskHealthy = isScsiDiskHealthy(operationalState) && error == null;
         return DiskStatus.createScsiNotClaimedDiskStatus(isDiskHealthy, operationalState);
      }
   }

   public static DiskStatus getPmemStorageStatus(PmemStorage storage) {
      boolean isStorageHealthy = storage.overallStatus == DatastoreHealthStatus.GREEN && storage.isAccessible;
      return DiskStatus.createPmemStorageStatus(isStorageHealthy, storage.isMounted);
   }

   private static boolean isOperationalStateValid(String[] diskOperationalState) {
      return !ArrayUtils.isEmpty(diskOperationalState) && EnumUtils.isValidEnum(State.class, diskOperationalState[0]);
   }

   private static boolean isScsiDiskHealthy(String operationalState) {
      return State.valueOf(operationalState).equals(State.ok);
   }
}
