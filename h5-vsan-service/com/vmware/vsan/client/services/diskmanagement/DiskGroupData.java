package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanScsiDisk;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class DiskGroupData implements IStorageData {
   public String name;
   public String iconId;
   public Boolean isMounted;
   public DiskData[] disks;
   public PmemDiskData[] pmemStorage;
   public ManagedObjectReference ownerHostRef;
   public DiskGroupType diskGroupType;
   public Boolean isLocked;
   public StorageCapacity capacity;
   public List objectUuids;

   public static DiskGroupData fromVsanDiskGroupMapping(ManagedObjectReference hostRef, DiskMapInfoEx mapInfo, Map claimedDisks) {
      DiskMapping mapping = mapInfo.getMapping();
      DiskGroupData result = new DiskGroupData();
      result.ownerHostRef = hostRef;
      result.name = mapping.ssd.vsanDiskInfo.vsanUuid;
      result.diskGroupType = mapInfo.isAllFlash ? DiskGroupType.ALL_FLASH : DiskGroupType.HYBRID;
      result.isMounted = mapInfo.isMounted;
      result.isLocked = mapInfo.encryptionInfo != null && mapInfo.encryptionInfo.encryptionEnabled && Boolean.FALSE.equals(mapInfo.unlockedEncrypted);
      List children = new ArrayList();
      String ssdUuid = mapping.ssd.uuid;
      if (claimedDisks.containsKey(ssdUuid)) {
         DiskData cacheDisk = (DiskData)claimedDisks.get(ssdUuid);
         cacheDisk.diskGroup = result.name;
         cacheDisk.isMappedAsCache = true;
         cacheDisk.claimOption = ClaimOption.ClaimForCache;
         children.add(cacheDisk);
      }

      List capacityDisks = new ArrayList();
      ScsiDisk[] var8 = mapping.getNonSsd();
      int var9 = var8.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         ScsiDisk nonSsd = var8[var10];
         if (claimedDisks.containsKey(nonSsd.uuid)) {
            DiskData capacityDisk = (DiskData)claimedDisks.get(nonSsd.uuid);
            capacityDisk.diskGroup = result.name;
            capacityDisk.isMappedAsCache = false;
            capacityDisk.claimOption = ClaimOption.ClaimForStorage;
            children.add(capacityDisk);
            capacityDisks.add(capacityDisk);
         }
      }

      result.disks = (DiskData[])children.toArray(new DiskData[0]);
      result.objectUuids = DiskManagementUtil.collectObjectUuids(result.disks);
      result.capacity = StorageCapacity.aggregate((IStorageData[])capacityDisks.toArray(new DiskData[0]));
      result.iconId = getDiskGroupIcon(result.isLocked, result.isMounted, result.disks);
      return result;
   }

   public static String getDiskGroupIcon(boolean isGroupLocked, boolean isGroupMounted, DiskData[] disks) {
      if (isGroupLocked) {
         return "disk-group-error-icon";
      } else if (!isGroupMounted) {
         return "disk-group-unmounted-icon";
      } else {
         return areAllDisksHealthy(disks) ? "disk-group-icon" : "disk-group-error-icon";
      }
   }

   private static boolean areAllDisksHealthy(DiskData[] disks) {
      return ArrayUtils.isNotEmpty(disks) && Arrays.stream(disks).allMatch(DiskData::isHealthy);
   }

   public static DiskGroupData fromVsanDirectDisks(ManagedObjectReference hostRef, VsanScsiDisk[] managedScsiDisks, Map disks) {
      DiskGroupData diskGroup = new DiskGroupData();
      diskGroup.ownerHostRef = hostRef;
      diskGroup.diskGroupType = DiskGroupType.VSAN_DIRECT;
      diskGroup.disks = (DiskData[])Arrays.stream(managedScsiDisks).filter((disk) -> {
         return disks.containsKey(disk.uuid);
      }).map((disk) -> {
         return (DiskData)disks.get(disk.uuid);
      }).toArray((x$0) -> {
         return new DiskData[x$0];
      });
      diskGroup.objectUuids = DiskManagementUtil.collectObjectUuids(diskGroup.disks);
      diskGroup.capacity = StorageCapacity.aggregate(diskGroup.disks);
      return diskGroup;
   }

   public static DiskGroupData fromPmemStorage(ManagedObjectReference hostRef, ManagedObjectReference[] managedPMemStorage, Map pmemStorage) {
      DiskGroupData diskGroup = new DiskGroupData();
      diskGroup.ownerHostRef = hostRef;
      diskGroup.diskGroupType = DiskGroupType.PMEM;
      diskGroup.disks = new DiskData[0];
      Stream var10001 = Arrays.stream(managedPMemStorage);
      pmemStorage.getClass();
      var10001 = var10001.filter(pmemStorage::containsKey);
      pmemStorage.getClass();
      diskGroup.pmemStorage = (PmemDiskData[])var10001.map(pmemStorage::get).toArray((x$0) -> {
         return new PmemDiskData[x$0];
      });
      diskGroup.objectUuids = DiskManagementUtil.collectObjectUuids(diskGroup.pmemStorage);
      diskGroup.capacity = StorageCapacity.aggregate(diskGroup.pmemStorage);
      return diskGroup;
   }

   public StorageCapacity getCapacity() {
      return this.capacity;
   }

   public List getObjectUuids() {
      return this.objectUuids;
   }
}
