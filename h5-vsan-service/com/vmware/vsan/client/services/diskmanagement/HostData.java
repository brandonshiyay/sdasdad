package com.vmware.vsan.client.services.diskmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.StorageDeviceInfo;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Adapter;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Path;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Target;
import com.vmware.vim.binding.vim.vsan.host.ClusterStatus;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vim.vsan.host.DiskResult.State;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapability;
import com.vmware.vim.vsan.binding.vim.cluster.VsanManagedStorageObjUuidMapping;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedDisksInfo;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanScsiDisk;
import com.vmware.vsan.client.services.common.data.ConnectionState;
import com.vmware.vsan.client.services.diskmanagement.claiming.AvailabilityState;
import com.vmware.vsphere.client.vsan.base.data.VsanCapabilityData;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class HostData {
   private static final Log logger = LogFactory.getLog(HostData.class);
   public static final String[] DS_HOST_PROPERTIES = new String[]{"name", "primaryIconId", "config.vsanHostConfig.faultDomainInfo.name", "runtime.connectionState", "runtime.inMaintenanceMode", "config.product.version"};
   private static final String UNKNOWN_KEY = "vsan.common.unknown";
   private static final String BLANK_VSAN_UUID_PATTERN = "00000000-0000-0000-0000-000000000000";
   public String name;
   public ConnectionState state;
   public boolean isInMaintenanceMode;
   public ManagedObjectReference hostRef;
   public Map disks;
   public Map pmemStorage;
   public String iconId;
   public String faultDomain;
   public Integer networkPartitionGroup;
   public boolean isWitnessHost;
   public boolean isMetadataWitnessHost;
   public DiskGroupData[] diskGroups;
   public HostData.HealthStatus healthStatus;
   public String version;
   public VsanCapabilityData capabilities;
   public StorageCapacity capacity;
   public List objectUuids;

   public String getName() {
      return this.name;
   }

   private static HostData parseHostProperties(Map hostProperties) {
      HostData hostData = new HostData();
      hostData.name = getStringProperty(hostProperties, "name");
      hostData.iconId = getStringProperty(hostProperties, "primaryIconId");
      hostData.faultDomain = getStringProperty(hostProperties, "config.vsanHostConfig.faultDomainInfo.name");
      hostData.isInMaintenanceMode = getBooleanProperty(hostProperties, "runtime.inMaintenanceMode");
      hostData.version = getStringProperty(hostProperties, "config.product.version");
      com.vmware.vim.binding.vim.HostSystem.ConnectionState hostState = (com.vmware.vim.binding.vim.HostSystem.ConnectionState)hostProperties.get("runtime.connectionState");
      hostData.state = ConnectionState.fromHostState(hostState);
      return hostData;
   }

   public static HostData create(ManagedObjectReference hostRef, boolean isWitness, boolean isMetadataWitnessHost, Map hostProperties, VsanManagedDisksInfo managedDisks, DiskResult[] scsiDisks, Map vsanDiskStatuses, StorageDeviceInfo hostDeviceInfo, ClusterStatus healthStatus, Integer networkPartition, VsanCapabilityData capabilities, JsonNode vsanDisksProperties, List pmemStorage, VsanManagedStorageObjUuidMapping[] storageToObjectUuids) {
      HostData hostData = parseHostProperties(hostProperties);
      hostData.hostRef = hostRef;
      hostData.isWitnessHost = isWitness;
      hostData.isMetadataWitnessHost = isMetadataWitnessHost;
      hostData.networkPartitionGroup = networkPartition;
      hostData.healthStatus = HostData.HealthStatus.fromVmodl(healthStatus);
      hostData.capabilities = capabilities;
      hostData.disks = getDisksByAvailability(scsiDisks, managedDisks, hostDeviceInfo, vsanDisksProperties, vsanDiskStatuses, storageToObjectUuids);
      hostData.pmemStorage = getPmemStorageByAvailability(pmemStorage, managedDisks, storageToObjectUuids);
      hostData.diskGroups = getDiskGroups(hostRef, managedDisks, (List)hostData.disks.get(AvailabilityState.IN_USE_BY_VSAN), (List)hostData.disks.get(AvailabilityState.ONLY_MANAGED_BY_VSAN), (List)hostData.pmemStorage.get(AvailabilityState.ONLY_MANAGED_BY_VSAN));
      hostData.objectUuids = DiskManagementUtil.collectObjectUuids(hostData.diskGroups);
      hostData.capacity = StorageCapacity.aggregate(hostData.diskGroups);
      return hostData;
   }

   private static Map getDisksByAvailability(DiskResult[] allDisks, VsanManagedDisksInfo managedDisks, StorageDeviceInfo hostDeviceInfo, JsonNode vsanDisksProperties, Map vsanDiskStatuses, VsanManagedStorageObjUuidMapping[] storageToObjectUuids) {
      final Multimap disksByAvailability = ArrayListMultimap.create();
      Map vsanDirectDiskStatuses = DiskStatusUtil.getVsanDirectDiskStatuses(managedDisks.vSANDirectDisks);
      Map vsanDirectDisksCapacity = DiskManagementUtil.getVsanDirectDisksCapacity(managedDisks.vSANDirectDisks);
      Map targetsMap = DiskData.mapAvailableTargets(hostDeviceInfo);
      Map adaptersMap = DiskData.mapAvailableAdapters(hostDeviceInfo);
      Map disksMap = DiskData.mapDiskPaths(hostDeviceInfo);
      if (allDisks != null) {
         DiskResult[] var12 = allDisks;
         int var13 = allDisks.length;

         for(int var14 = 0; var14 < var13; ++var14) {
            DiskResult diskResult = var12[var14];
            DiskData diskData = createDiskData(targetsMap, adaptersMap, disksMap, diskResult);
            if (StringUtils.isNotEmpty(diskData.vsanUuid) && !isVsanUuidBlank(diskData.vsanUuid)) {
               diskData.updateVsanDetails(vsanDiskStatuses, vsanDisksProperties);
               disksByAvailability.put(AvailabilityState.IN_USE_BY_VSAN, diskData);
            } else if (vsanDirectDiskStatuses.containsKey(diskResult.disk.uuid)) {
               diskData.updateVsanDirectDetails(vsanDirectDiskStatuses, vsanDirectDisksCapacity, storageToObjectUuids);
               disksByAvailability.put(AvailabilityState.ONLY_MANAGED_BY_VSAN, diskData);
            } else {
               switch(State.valueOf(diskResult.state)) {
               case ineligible:
                  disksByAvailability.put(AvailabilityState.INELIGIBLE, diskData);
                  break;
               case eligible:
                  disksByAvailability.put(AvailabilityState.ELIGIBLE, diskData);
                  break;
               case inUse:
                  diskData.updateVsanDetails(vsanDiskStatuses, vsanDisksProperties);
                  disksByAvailability.put(AvailabilityState.IN_USE_BY_VSAN, diskData);
                  break;
               default:
                  logger.warn("Unknown disk status: " + diskResult.state);
               }
            }
         }
      }

      return new HashMap() {
         {
            this.put(AvailabilityState.ELIGIBLE, new ArrayList(disksByAvailability.get(AvailabilityState.ELIGIBLE)));
            this.put(AvailabilityState.INELIGIBLE, new ArrayList(disksByAvailability.get(AvailabilityState.INELIGIBLE)));
            this.put(AvailabilityState.IN_USE_BY_VSAN, new ArrayList(disksByAvailability.get(AvailabilityState.IN_USE_BY_VSAN)));
            this.put(AvailabilityState.ONLY_MANAGED_BY_VSAN, new ArrayList(disksByAvailability.get(AvailabilityState.ONLY_MANAGED_BY_VSAN)));
         }
      };
   }

   private static Map getPmemStorageByAvailability(List allPmemStorage, VsanManagedDisksInfo managedDisks, VsanManagedStorageObjUuidMapping[] storageToObjectUuids) {
      final Multimap pmemStorageByAvailability = ArrayListMultimap.create();
      if (CollectionUtils.isNotEmpty(allPmemStorage)) {
         allPmemStorage.stream().map((storage) -> {
            return new PmemDiskData(storage, storageToObjectUuids);
         }).forEach((storage) -> {
            AvailabilityState availabilityState = storage.getAvailabilityState(managedDisks);
            if (availabilityState == AvailabilityState.ONLY_MANAGED_BY_VSAN) {
               storage.claimOption = ClaimOption.PMEM;
            }

            pmemStorageByAvailability.put(availabilityState, storage);
         });
      }

      return new HashMap() {
         {
            this.put(AvailabilityState.ELIGIBLE, new ArrayList(pmemStorageByAvailability.get(AvailabilityState.ELIGIBLE)));
            this.put(AvailabilityState.INELIGIBLE, new ArrayList(pmemStorageByAvailability.get(AvailabilityState.INELIGIBLE)));
            this.put(AvailabilityState.ONLY_MANAGED_BY_VSAN, new ArrayList(pmemStorageByAvailability.get(AvailabilityState.ONLY_MANAGED_BY_VSAN)));
         }
      };
   }

   private static boolean isVsanUuidBlank(String vsanUuid) {
      return "00000000-0000-0000-0000-000000000000".equals(vsanUuid);
   }

   private static DiskGroupData[] getDiskGroups(ManagedObjectReference hostRef, VsanManagedDisksInfo managedDisks, List vsanDisks, List vsanDirectDisks, List pmemStorage) {
      List diskGroups = new ArrayList();
      if (ArrayUtils.isNotEmpty(managedDisks.vSANDiskMapInfo)) {
         diskGroups.addAll(getVsanDiskGroups(hostRef, managedDisks, vsanDisks));
      }

      if (DiskManagementUtil.hasClaimedDisks(managedDisks.vSANDirectDisks)) {
         diskGroups.add(getVsanDirectDiskGroup(hostRef, managedDisks, vsanDirectDisks));
      }

      if (DiskManagementUtil.hasClaimedDisks(managedDisks.vSANPMemInfo) && CollectionUtils.isNotEmpty(pmemStorage)) {
         diskGroups.add(getPmemDiskGroup(hostRef, managedDisks, pmemStorage));
      }

      return (DiskGroupData[])diskGroups.toArray(new DiskGroupData[0]);
   }

   private static List getVsanDiskGroups(ManagedObjectReference hostRef, VsanManagedDisksInfo managedDisks, List vsanDisks) {
      Map uuidToDisk = (Map)vsanDisks.stream().collect(Collectors.toMap((disk) -> {
         return disk.disk.uuid;
      }, (disk) -> {
         return disk;
      }));
      return (List)Arrays.stream(managedDisks.vSANDiskMapInfo).map((mapping) -> {
         return DiskGroupData.fromVsanDiskGroupMapping(hostRef, mapping, uuidToDisk);
      }).collect(Collectors.toList());
   }

   private static DiskGroupData getVsanDirectDiskGroup(ManagedObjectReference hostRef, VsanManagedDisksInfo managedDisks, List vsanDirectDisks) {
      Map uuidToDisk = (Map)vsanDirectDisks.stream().collect(Collectors.toMap((disk) -> {
         return disk.disk.uuid;
      }, (disk) -> {
         return disk;
      }));
      VsanScsiDisk[] managedScsiDisks = (VsanScsiDisk[])Arrays.stream(managedDisks.vSANDirectDisks).filter((storage) -> {
         return ArrayUtils.isNotEmpty(storage.scsiDisks);
      }).flatMap((storage) -> {
         return Arrays.stream(storage.scsiDisks);
      }).toArray((x$0) -> {
         return new VsanScsiDisk[x$0];
      });
      return DiskGroupData.fromVsanDirectDisks(hostRef, managedScsiDisks, uuidToDisk);
   }

   private static DiskGroupData getPmemDiskGroup(ManagedObjectReference hostRef, VsanManagedDisksInfo managedDisks, List pmemStorage) {
      Map dsRefToStorage = (Map)pmemStorage.stream().collect(Collectors.toMap((storage) -> {
         return storage.dsRef;
      }, (storage) -> {
         return storage;
      }));
      return DiskGroupData.fromPmemStorage(hostRef, managedDisks.vSANPMemInfo.localPMemDatastores, dsRefToStorage);
   }

   public static Integer getNetworkPartitionGroup(String nodeUuid, Collection partitionGroups) {
      Iterator iterator = partitionGroups.iterator();

      for(int index = 0; iterator.hasNext(); ++index) {
         Set group = (Set)iterator.next();
         if (group.contains(nodeUuid)) {
            return index + 1;
         }
      }

      return null;
   }

   public static Map mapCapabilities(VsanCapability[] vsanCapabilities, ManagedObjectReference clusterRef) {
      Map hostCapabilities = new HashMap();
      if (vsanCapabilities != null) {
         VsanCapability[] var3 = vsanCapabilities;
         int var4 = vsanCapabilities.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanCapability data = var3[var5];
            VsanCapabilityData capabilityData = VsanCapabilityData.fromVsanCapability(data);
            ManagedObjectReference hostRef = new ManagedObjectReference(data.target.getType(), data.target.getValue(), clusterRef.getServerGuid());
            hostCapabilities.put(hostRef, capabilityData);
         }
      }

      return hostCapabilities;
   }

   private static DiskData createDiskData(Map targetsMap, Map adaptersMap, Map disksMap, DiskResult vsanDisk) {
      Target target = null;
      Adapter adapter = null;
      if (disksMap.containsKey(vsanDisk.disk.uuid)) {
         Path path = (Path)disksMap.get(vsanDisk.disk.uuid);
         if (targetsMap.containsKey(path.target)) {
            target = (Target)targetsMap.get(path.target);
         }

         if (adaptersMap.containsKey(path.adapter)) {
            adapter = (Adapter)adaptersMap.get(path.adapter);
         }
      }

      String vsanUuid = vsanDisk.vsanUuid;
      return DiskData.fromScsiDisk(vsanDisk.disk, vsanUuid, target, adapter, vsanDisk.state);
   }

   private static String getStringProperty(Map properties, String propertyName) {
      try {
         return properties.get(propertyName).toString();
      } catch (Exception var3) {
         logger.warn("Unable to extract '" + propertyName + "' property: ", var3);
         return Utils.getLocalizedString("vsan.common.unknown");
      }
   }

   private static Boolean getBooleanProperty(Map properties, String propertyName) {
      try {
         return Boolean.parseBoolean(properties.get(propertyName).toString());
      } catch (Exception var3) {
         logger.warn("Unable to extract '" + propertyName + "' property: ", var3);
         return null;
      }
   }

   @TsModel
   public static enum HealthStatus {
      HEALTHY,
      UNHEALTHY,
      UNKNOWN;

      public static HostData.HealthStatus fromVmodl(ClusterStatus status) {
         return status == null ? UNKNOWN : (HostData.HealthStatus)EnumUtils.fromStringIgnoreCase(HostData.HealthStatus.class, status.health, UNKNOWN);
      }
   }
}
