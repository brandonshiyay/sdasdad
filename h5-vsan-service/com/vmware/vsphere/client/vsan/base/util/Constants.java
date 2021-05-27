package com.vmware.vsphere.client.vsan.base.util;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class Constants {
   public static final String CLUSTER_HOST_PROPERTY = "host";
   public static final String CURRENT_TIME_ON_HOST_PROPERTY = "currentTimeOnHost";
   public static final String UUID_PROPERTY = "uuid";
   public static final String WITNESS_TYPE_KEY = "Witness";
   public static final String COMPONENT_TYPE_KEY = "Component";
   public static final String RAID_0_TYPE_KEY = "RAID_0";
   public static final String RAID_1_TYPE_KEY = "RAID_1";
   public static final String RAID_5_TYPE_KEY = "RAID_5";
   public static final String RAID_6_TYPE_KEY = "RAID_6";
   public static final String RAID_D_TYPE_KEY = "RAID_D";
   public static final String CONCATENATION_TYPE_KEY = "Concatenation";
   public static final String TYPE_KEY = "type";
   public static final String CONFIG_KEY = "config";
   public static final String CONTENT_KEY = "content";
   public static final String DOM_OBJECTS_KEY = "dom_objects";
   public static final String LSOM_OBJECTS_KEY = "lsom_objects";
   public static final String LSOM_OBJECTS_COUNT_KEY = "lsom_objects_count";
   public static final String DISK_OBJECTS_KEY = "disk_objects";
   public static final String DISK_HEALTH_KEY = "disk_health";
   public static final String DISK_HEALTH_FLAGS_KEY = "healthFlags";
   public static final String FORMAT_VERSION_KEY = "formatVersion";
   public static final String PUBLIC_FORMAT_VERSION_KEY = "publicFormatVersion";
   public static final String SELF_ONLY_KEY = "self_only";
   public static final String DISK_RESERVED_CAPACITY_KEY = "capacityReserved";
   public static final String DISK_USED_CAPACITY_KEY = "capacityUsed";
   public static final String COMPONENT_UUID_KEY = "componentUuid";
   public static final String COMPONENT_BYTES_TO_SYNC_KEY = "bytesToSync";
   public static final String COMPONENT_RECOVERY_ETA_KEY = "recoveryETA";
   public static final String ATTRIBUTE_KEY = "attributes";
   public static final String OWNER_KEY = "owner";
   public static final String COMPONENT_STATE_KEY = "componentState";
   public static final String COMPONENT_FLAGS_KEY = "flags";
   public static final String SSD_UUID_KEY = "ssdUuid";
   public static final String DISK_UUID_KEY = "diskUuid";
   public static final String PBM_PROFILES = "pbmProfiles";
   public static final String PBM_REQUIREMENT_STORAGE_PROFILE = "PbmRequirementStorageProfile";
   public static final String PROFILE_CONTENT = "profileContent";
   public static final String DATASTORE_VSAN_TYPE = "vsan";
   public static final String DATASTORE_VSAN_DIRECT_TYPE = "vsanD";
   public static final String DATASTORE_PMEM_TYPE = "PMEM";
   public static final String FAULT_DOMAIN_TYPE = "FaultDomain";
   public static final String HOST_TYPE = "host";
   public static final String DISK_GROUP_TYPE = "Diskgroup";
   public static final String CACHE_DISK_TYPE = "SSD";
   public static final String CAPACITY_DISK_TYPE = "Disk";
   public static final String VMWARE_VSAN_NAMESPACE = "VSAN";
   public static final String[] PHYSICAL_DISK_HEALTH_AND_VERSION_PROPERTIES = new String[]{"disk_health", "formatVersion", "publicFormatVersion", "self_only"};
   public static final String[] PHYSICAL_DISK_VIRTUAL_MAPPING_PROPERTIES = new String[]{"lsom_objects", "lsom_objects_count", "capacityReserved", "capacityUsed", "self_only"};
}
