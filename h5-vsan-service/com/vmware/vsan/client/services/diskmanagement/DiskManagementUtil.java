package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vim.vsan.host.DiskResult.State;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanDirectStorage;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedDisksInfo;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedPMemInfo;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanScsiDisk;
import com.vmware.vsan.client.services.diskmanagement.claiming.ClaimOptionsUtil;
import com.vmware.vsan.client.services.diskmanagement.claiming.ClaimedDisksSummary;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import com.vmware.vsphere.client.vsan.data.DiskLocalityType;
import com.vmware.vsphere.client.vsan.data.VsanDiskData;
import com.vmware.vsphere.client.vsan.data.VsanPmemDiskData;
import com.vmware.vsphere.client.vsan.data.VsanSemiAutoClaimDisksData;
import com.vmware.vsphere.client.vsan.util.VsanAllFlashClaimOptionRecommender;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskManagementUtil {
   private static final Logger logger = LoggerFactory.getLogger(DiskManagementUtil.class);

   public static String getDiskName(ScsiDisk disk) {
      return disk.displayName != null ? disk.displayName : disk.canonicalName;
   }

   public static DiskLocalityType getDiskLocality(ScsiDisk disk) {
      if (disk.localDisk == null) {
         return DiskLocalityType.Unknown;
      } else {
         return disk.localDisk ? DiskLocalityType.Local : DiskLocalityType.Remote;
      }
   }

   public static Integer normalizeDiskFormatVersion(Integer formatVersion) {
      return formatVersion != null && formatVersion == 0 ? 1 : formatVersion;
   }

   public static boolean isDiskEligible(DiskResult result) {
      if (!EnumUtils.isValidEnum(State.class, result.state)) {
         logger.warn("Unable to parse the disk result state: " + result.state);
         return false;
      } else {
         return Enum.valueOf(State.class, result.state) == State.eligible;
      }
   }

   public static boolean isPmemStorageInUse(ManagedObjectReference dsRef, VsanManagedDisksInfo managedDisks) {
      return managedDisks != null && isPmemStorageInUse(dsRef, managedDisks.vSANPMemInfo);
   }

   public static boolean isPmemStorageInUse(ManagedObjectReference dsRef, VsanManagedPMemInfo vSANPMemInfo) {
      return hasClaimedDisks(vSANPMemInfo) && Arrays.asList(vSANPMemInfo.localPMemDatastores).contains(dsRef);
   }

   public static Map getVsanDirectDisksCapacity(VsanDirectStorage[] vsanDirectDisks) {
      return (Map)(!hasClaimedDisks(vsanDirectDisks) ? new HashMap() : (Map)Arrays.stream(vsanDirectDisks).flatMap((storage) -> {
         return Arrays.stream(storage.scsiDisks);
      }).collect(HashMap::new, (result, disk) -> {
         StorageCapacity var10000 = (StorageCapacity)result.put(disk.getUuid(), getVsanDirectDiskCapacity(disk));
      }, HashMap::putAll));
   }

   public static StorageCapacity getVsanDirectDiskCapacity(VsanScsiDisk disk) {
      long totalCapacity = BaseUtils.lbaToBytes(disk.capacity);
      long usedCapacity = disk.usedCapacity != null ? disk.usedCapacity : 0L;
      return new StorageCapacity(totalCapacity, usedCapacity, (Long)null);
   }

   public static boolean hasClaimedDisks(VsanDirectStorage[] vsanDirectStorage) {
      return ArrayUtils.isEmpty(vsanDirectStorage) ? false : Arrays.stream(vsanDirectStorage).anyMatch((storage) -> {
         return ArrayUtils.isNotEmpty(storage.scsiDisks);
      });
   }

   public static boolean hasClaimedDisks(VsanManagedPMemInfo vSANPMemInfo) {
      return vSANPMemInfo != null && ArrayUtils.isNotEmpty(vSANPMemInfo.localPMemDatastores);
   }

   public static VsanSemiAutoClaimDisksData getNotClaimedDisksData(ManagedObjectReference hostRef, DiskResult[] scsiDisks, List pmemStorage, VsanManagedDisksInfo managedDisks) {
      if (ArrayUtils.isEmpty(scsiDisks) && CollectionUtils.isEmpty(pmemStorage)) {
         return null;
      } else {
         Map claimOptionsPerDiskType = ClaimOptionsUtil.getSupportedClaimOptions(hostRef);
         VsanSemiAutoClaimDisksData data = new VsanSemiAutoClaimDisksData(getEligibleScsiDisks(scsiDisks, claimOptionsPerDiskType), getEligiblePmemStorage(pmemStorage, managedDisks, claimOptionsPerDiskType));
         if (managedDisks != null) {
            populateClaimingSummary(data, managedDisks, pmemStorage);
         }

         VsanAllFlashClaimOptionRecommender recommender = new VsanAllFlashClaimOptionRecommender(data, (Map)null);
         recommender.recommend();
         return data;
      }
   }

   private static VsanDiskData[] getEligibleScsiDisks(DiskResult[] scsiDisks, Map claimOptionsPerDiskType) {
      return ArrayUtils.isEmpty(scsiDisks) ? new VsanDiskData[0] : (VsanDiskData[])Arrays.stream(scsiDisks).filter(DiskManagementUtil::isDiskEligible).map((diskResult) -> {
         return new VsanDiskData(diskResult, claimOptionsPerDiskType);
      }).toArray((x$0) -> {
         return new VsanDiskData[x$0];
      });
   }

   private static VsanPmemDiskData[] getEligiblePmemStorage(List pmemStorage, VsanManagedDisksInfo managedDisks, Map claimOptionsPerDiskType) {
      return CollectionUtils.isEmpty(pmemStorage) ? new VsanPmemDiskData[0] : (VsanPmemDiskData[])pmemStorage.stream().filter((storage) -> {
         return !isPmemStorageInUse(storage.dsRef, managedDisks);
      }).map((storage) -> {
         return new VsanPmemDiskData(storage, claimOptionsPerDiskType);
      }).toArray((x$0) -> {
         return new VsanPmemDiskData[x$0];
      });
   }

   private static void populateClaimingSummary(VsanSemiAutoClaimDisksData data, VsanManagedDisksInfo managedDisks, List pmemStorage) {
      if (ArrayUtils.isNotEmpty(managedDisks.vSANDiskMapInfo)) {
         populateVsanClaimedDisksSummary(data, managedDisks.vSANDiskMapInfo);
      }

      if (hasClaimedDisks(managedDisks.vSANDirectDisks)) {
         populateVsanDirectClaimedDisksSummary(data, managedDisks.vSANDirectDisks);
      }

      if (hasClaimedDisks(managedDisks.vSANPMemInfo) && CollectionUtils.isNotEmpty(pmemStorage)) {
         populatePmemClaimedStorageSummary(data, managedDisks.vSANPMemInfo, pmemStorage);
      }

   }

   private static void populateVsanClaimedDisksSummary(VsanSemiAutoClaimDisksData data, DiskMapInfoEx[] diskMappings) {
      long totalCapacity = 0L;
      long totalCache = 0L;
      DiskMapInfoEx[] var6 = diskMappings;
      int var7 = diskMappings.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         DiskMapInfoEx diskMapping = var6[var8];
         totalCache += BaseUtils.lbaToBytes(diskMapping.mapping.ssd.capacity);
         ScsiDisk[] storageDisks = diskMapping.mapping.nonSsd;
         if (!ArrayUtils.isEmpty(storageDisks)) {
            ScsiDisk firstDisk = storageDisks[0];
            if (firstDisk.ssd) {
               data.allFlashDiskGroupExist = true;
               ++data.numAllFlashGroups;
               data.numAllFlashCapacityDisks += storageDisks.length;
            } else {
               data.hybridDiskGroupExist = true;
               ++data.numHybridGroups;
               data.numHybridCapacityDisks += storageDisks.length;
            }

            ScsiDisk[] var12 = storageDisks;
            int var13 = storageDisks.length;

            for(int var14 = 0; var14 < var13; ++var14) {
               ScsiDisk storageDisk = var12[var14];
               totalCapacity += BaseUtils.lbaToBytes(storageDisk.capacity);
            }
         }
      }

      data.claimedCapacity = totalCapacity;
      data.claimedCache = totalCache;
   }

   private static void populateVsanDirectClaimedDisksSummary(VsanSemiAutoClaimDisksData data, VsanDirectStorage[] vsanDirectStorage) {
      data.claimedDisksSummary.addAll((Collection)Arrays.stream(vsanDirectStorage).filter((storage) -> {
         return ArrayUtils.isNotEmpty(storage.scsiDisks);
      }).map((storage) -> {
         return getClaimedDisksSummary(storage.scsiDisks);
      }).collect(Collectors.toList()));
   }

   private static void populatePmemClaimedStorageSummary(VsanSemiAutoClaimDisksData data, VsanManagedPMemInfo vSANPMemInfo, List pmemStorage) {
      List claimedPmemStorage = (List)pmemStorage.stream().filter((storage) -> {
         return isPmemStorageInUse(storage.dsRef, vSANPMemInfo);
      }).collect(Collectors.toList());
      data.claimedDisksSummary.add(getClaimedDisksSummary(claimedPmemStorage));
   }

   private static ClaimedDisksSummary getClaimedDisksSummary(VsanScsiDisk[] scsiDisks) {
      ClaimedDisksSummary summary = new ClaimedDisksSummary();
      summary.claimOption = ClaimOption.VMFS;
      summary.claimedDisksCount = scsiDisks.length;
      summary.claimedCapacity = Arrays.stream(scsiDisks).mapToLong((disk) -> {
         return BaseUtils.lbaToBytes(disk.capacity);
      }).sum();
      return summary;
   }

   private static ClaimedDisksSummary getClaimedDisksSummary(List pmemStorage) {
      ClaimedDisksSummary summary = new ClaimedDisksSummary();
      summary.claimOption = ClaimOption.PMEM;
      summary.claimedDisksCount = pmemStorage.size();
      summary.claimedCapacity = pmemStorage.stream().mapToLong((storage) -> {
         return storage.capacity.total;
      }).sum();
      return summary;
   }

   public static List collectObjectUuids(IStorageData[] storageEntities) {
      return (List)Arrays.stream(storageEntities).filter((storageEntity) -> {
         return CollectionUtils.isNotEmpty(storageEntity.getObjectUuids());
      }).flatMap((storageEntity) -> {
         return storageEntity.getObjectUuids().stream();
      }).collect(Collectors.toList());
   }
}
