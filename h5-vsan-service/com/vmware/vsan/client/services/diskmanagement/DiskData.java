package com.vmware.vsan.client.services.diskmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.BlockAdapterTargetTransport;
import com.vmware.vim.binding.vim.host.FibreChannelOverEthernetTargetTransport;
import com.vmware.vim.binding.vim.host.FibreChannelTargetTransport;
import com.vmware.vim.binding.vim.host.InternetScsiTargetTransport;
import com.vmware.vim.binding.vim.host.ParallelScsiTargetTransport;
import com.vmware.vim.binding.vim.host.PcieTargetTransport;
import com.vmware.vim.binding.vim.host.RdmaTargetTransport;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.host.SerialAttachedTargetTransport;
import com.vmware.vim.binding.vim.host.StorageDeviceInfo;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Adapter;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Path;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Target;
import com.vmware.vim.binding.vim.vsan.host.VsanDiskInfo;
import com.vmware.vim.binding.vim.vsan.host.DiskResult.State;
import com.vmware.vim.vsan.binding.vim.cluster.VsanManagedStorageObjUuidMapping;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsUtil;
import com.vmware.vsan.client.util.PhysicalDiskJsonParser;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import com.vmware.vsphere.client.vsan.data.DiskLocalityType;
import com.vmware.vsphere.client.vsan.data.VsanDiskData;
import com.vmware.vsphere.client.vsan.util.FormatUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TsModel
public class DiskData implements IStorageData {
   private static final Logger logger = LoggerFactory.getLogger(DiskData.class);
   private static final String ADAPTER_ID_PREFIX = "key-vim.host.PlugStoreTopology.Adapter-";
   private static final String DEVICE_ID_PREFIX = "key-vim.host.PlugStoreTopology.Device-";
   public String name;
   public boolean isFlash;
   public boolean isMappedAsCache;
   public StorageCapacity capacity;
   public DiskData.DeviceState deviceState;
   public List objectUuids;
   public String uuid;
   public String vsanUuid;
   public String diskGroup;
   public DiskLocalityType driveLocality;
   public String[] physicalLocation;
   public String diskAdapter;
   public DiskData.StorageDeviceTransport transportType;
   public String vendor;
   public ScsiDisk disk;
   public ClaimOption claimOption;
   public DiskStatus diskStatus;

   public static DiskData fromScsiDisk(ScsiDisk disk, String diskGroup, boolean isMappedAsCache, String vsanUuid, Target target, Adapter adapter, String claimState) {
      DiskData data = new DiskData();
      data.disk = disk;
      if (data.disk != null && data.disk.vsanDiskInfo == null && StringUtils.isNotEmpty(vsanUuid)) {
         VsanDiskInfo info = new VsanDiskInfo();
         info.setVsanUuid(vsanUuid);
         data.disk.vsanDiskInfo = info;
      }

      data.name = DiskManagementUtil.getDiskName(disk);
      data.isFlash = BooleanUtils.isTrue(disk.ssd);
      data.isMappedAsCache = isMappedAsCache;
      data.capacity = new StorageCapacity(BaseUtils.lbaToBytes(disk.capacity), 0L, (Long)null);
      data.deviceState = DiskData.DeviceState.fromScsiState(disk.operationalState);
      data.uuid = disk.uuid;
      if (adapter != null) {
         data.diskAdapter = extractDiskId(adapter.key, "key-vim.host.PlugStoreTopology.Adapter-");
      }

      if (target != null) {
         data.transportType = DiskData.StorageDeviceTransport.getTransport(target);
      }

      data.vsanUuid = vsanUuid;
      data.diskGroup = diskGroup;
      data.physicalLocation = disk.physicalLocation;
      data.vendor = disk.vendor + disk.model + FormatUtil.getStorageFormatted(data.capacity.total, 1L, 1073741824L);
      data.driveLocality = DiskManagementUtil.getDiskLocality(disk);
      if (!claimState.equals(State.inUse.name())) {
         data.diskStatus = DiskStatusUtil.getNotClaimedDiskStatus(data.disk, (Exception)null);
      }

      return data;
   }

   public static DiskData fromScsiDisk(ScsiDisk disk, String vsanUuid, Target target, Adapter adapter, String claimState) {
      return fromScsiDisk(disk, (String)null, false, vsanUuid, target, adapter, claimState);
   }

   public static DiskData fromVsanDiskData(VsanDiskData disk, String groupVsanUuid) {
      ScsiDisk scsiDisk = disk.disk;
      DiskData diskData = new DiskData();
      diskData.disk = scsiDisk;
      diskData.vsanUuid = disk.vsanUuid;
      diskData.diskGroup = groupVsanUuid;
      diskData.isFlash = BooleanUtils.isTrue(scsiDisk.ssd);
      diskData.deviceState = DiskData.DeviceState.fromScsiState(scsiDisk.operationalState);
      diskData.name = DiskManagementUtil.getDiskName(scsiDisk);
      diskData.diskStatus = disk.diskStatus;
      return diskData;
   }

   public boolean isDeviceStateHealthy() {
      return this.deviceState == DiskData.DeviceState.OK;
   }

   public boolean isHealthy() {
      return this.isDeviceStateHealthy() && this.diskStatus != null && this.diskStatus.isDiskHealthy;
   }

   public static String getDiskIcon(boolean isHealthy, boolean isFlash) {
      if (!isHealthy) {
         return isFlash ? "ssd-alert-icon" : "disk-error-icon";
      } else {
         return isFlash ? "ssd-disk-icon" : "disk-icon";
      }
   }

   public void updateVsanDetails(Map diskStatuses, JsonNode disksProperties) {
      this.diskStatus = (DiskStatus)diskStatuses.get(this.vsanUuid);
      this.capacity = new StorageCapacity(this.capacity.total, PhysicalDiskJsonParser.getUsedCapacity(disksProperties, this.vsanUuid), PhysicalDiskJsonParser.getReservedCapacity(disksProperties, this.vsanUuid));
      this.objectUuids = VirtualObjectsUtil.getObjectUuidsOnVsan(disksProperties, this.vsanUuid);
   }

   public void updateVsanDirectDetails(Map diskStatuses, Map disksCapacity, VsanManagedStorageObjUuidMapping[] storageToObjectUuids) {
      this.claimOption = ClaimOption.VMFS;
      this.isMappedAsCache = false;
      this.diskStatus = (DiskStatus)diskStatuses.get(this.uuid);
      this.capacity = new StorageCapacity(this.capacity.total, ((StorageCapacity)disksCapacity.get(this.uuid)).used, (Long)null);
      this.objectUuids = VirtualObjectsUtil.getObjectUuidsOnVsanDirect(storageToObjectUuids, this.uuid);
   }

   public StorageCapacity getCapacity() {
      return this.capacity;
   }

   public List getObjectUuids() {
      return this.objectUuids;
   }

   private static String extractDiskId(String deviceId, String prefix) {
      if (!deviceId.startsWith(prefix)) {
         throw new IllegalStateException("illegal device ID: " + deviceId + ", should start with " + prefix);
      } else {
         return deviceId.substring(prefix.length());
      }
   }

   public static Map mapAvailableTargets(StorageDeviceInfo info) {
      Map result = new HashMap();
      if (info != null && info.plugStoreTopology != null && info.plugStoreTopology.target != null) {
         Target[] var2 = info.plugStoreTopology.target;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Target target = var2[var4];
            result.put(target.getKey(), target);
         }
      }

      return result;
   }

   public static Map mapAvailableAdapters(StorageDeviceInfo info) {
      Map result = new HashMap();
      if (info != null && info.plugStoreTopology != null && info.plugStoreTopology.adapter != null) {
         Adapter[] var2 = info.plugStoreTopology.adapter;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Adapter adapter = var2[var4];
            result.put(adapter.getKey(), adapter);
         }
      }

      return result;
   }

   public static Map mapDiskPaths(StorageDeviceInfo info) {
      Map disksMap = new HashMap();
      if (info != null && info.plugStoreTopology != null && info.plugStoreTopology.path != null) {
         Path[] var2 = info.plugStoreTopology.path;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Path path = var2[var4];
            if (!StringUtils.isEmpty(path.device)) {
               disksMap.put(extractDiskId(path.device, "key-vim.host.PlugStoreTopology.Device-"), path);
            }
         }

         return disksMap;
      } else {
         return disksMap;
      }
   }

   @TsModel
   public static enum DeviceState {
      OK,
      OFF,
      LOST,
      ERROR,
      UNKNOWN;

      public static DiskData.DeviceState fromScsiState(String[] stateKeys) {
         Set states = new HashSet();
         String[] var2 = stateKeys;
         int var3 = stateKeys.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            String key = var2[var4];
            states.add(com.vmware.vim.binding.vim.host.ScsiLun.State.valueOf(key));
         }

         if (states.contains(com.vmware.vim.binding.vim.host.ScsiLun.State.ok)) {
            return OK;
         } else if (states.contains(com.vmware.vim.binding.vim.host.ScsiLun.State.off)) {
            return OFF;
         } else if (states.contains(com.vmware.vim.binding.vim.host.ScsiLun.State.lostCommunication)) {
            return LOST;
         } else {
            return states.contains(com.vmware.vim.binding.vim.host.ScsiLun.State.error) ? ERROR : UNKNOWN;
         }
      }
   }

   @TsModel
   public static enum StorageDeviceTransport {
      FCOETRANSPORT,
      FCTRANSPORT,
      ISCSITRANSPORT,
      PARALLELTRANSPORT,
      BLOCKTRANSPORT,
      SASTRANSPORT,
      PCIETRANSPORT,
      RDMATRANSPORT,
      UNKNOWN;

      public static DiskData.StorageDeviceTransport getTransport(Target target) {
         if (target != null && target.transport != null) {
            if (target.transport instanceof FibreChannelOverEthernetTargetTransport) {
               return FCOETRANSPORT;
            } else if (target.transport instanceof FibreChannelTargetTransport) {
               return FCTRANSPORT;
            } else if (target.transport instanceof InternetScsiTargetTransport) {
               return ISCSITRANSPORT;
            } else if (target.transport instanceof ParallelScsiTargetTransport) {
               return PARALLELTRANSPORT;
            } else if (target.transport instanceof BlockAdapterTargetTransport) {
               return BLOCKTRANSPORT;
            } else if (target.transport instanceof SerialAttachedTargetTransport) {
               return SASTRANSPORT;
            } else if (target.transport instanceof PcieTargetTransport) {
               return PCIETRANSPORT;
            } else if (target.transport instanceof RdmaTargetTransport) {
               return RDMATRANSPORT;
            } else {
               DiskData.logger.warn("Unknown transport type: " + target.transport + ". Returning UNKNOWN instead.");
               return UNKNOWN;
            }
         } else {
            return null;
         }
      }
   }
}
