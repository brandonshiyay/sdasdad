package com.vmware.vsan.client.services.virtualobjects;

import com.fasterxml.jackson.databind.JsonNode;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice.FileBackingInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanManagedStorageObjUuidMapping;
import com.vmware.vim.vsan.binding.vim.host.VsanObjectHealth;
import com.vmware.vim.vsan.binding.vim.host.VsanObjectOverallHealth;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectHealthModel;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectModel;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectPlacementModel;
import com.vmware.vsan.client.util.PhysicalDiskJsonParser;
import com.vmware.vsphere.client.vsan.base.data.ObjectHealthComplianceState;
import com.vmware.vsphere.client.vsan.base.data.ObjectHealthIncomplianceReason;
import com.vmware.vsphere.client.vsan.base.data.ObjectHealthPolicyState;
import com.vmware.vsphere.client.vsan.base.data.ObjectHealthRebuildState;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectCompositeHealth;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectHealthState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class VirtualObjectsUtil {
   private static final String VSAN_DIRECT_STORAGE_TYPE = "vsandirect";
   private static final String PMEM_STORAGE_TYPE = "pmem";

   public static VirtualDisk findDisk(VirtualDevice[] virtualDevices, String diskId) {
      if (virtualDevices == null) {
         return null;
      } else {
         VirtualDevice[] var2 = virtualDevices;
         int var3 = virtualDevices.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VirtualDevice device = var2[var4];
            if (device instanceof VirtualDisk) {
               VirtualDisk disk = (VirtualDisk)device;
               FileBackingInfo backing = findBacking(disk, diskId);
               if (backing != null) {
                  return disk;
               }
            }
         }

         return null;
      }
   }

   private static FileBackingInfo findBacking(VirtualDisk disk, String backingId) {
      if (!(disk.backing instanceof FileBackingInfo)) {
         return null;
      } else {
         FileBackingInfo backing = (FileBackingInfo)disk.getBacking();
         String backingObjectId = backing.backingObjectId;
         return StringUtils.isNotEmpty(backingObjectId) && backingObjectId.contains(backingId) ? backing : null;
      }
   }

   public static Map getVsanObjectsHealthMap(VsanObjectOverallHealth healthData) {
      Map healthDataMap = new HashMap();
      if (healthData != null && !ArrayUtils.isEmpty(healthData.objectHealthDetail)) {
         VsanObjectHealth[] var2 = healthData.objectHealthDetail;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanObjectHealth vsanObjectHealth = var2[var4];
            if (!ArrayUtils.isEmpty(vsanObjectHealth.objUuids)) {
               String[] var6 = vsanObjectHealth.objUuids;
               int var7 = var6.length;

               for(int var8 = 0; var8 < var7; ++var8) {
                  String uuid = var6[var8];
                  healthDataMap.put(uuid, new VirtualObjectHealthModel(vsanObjectHealth.health, vsanObjectHealth.healthV2));
               }
            }
         }

         return healthDataMap;
      } else {
         return healthDataMap;
      }
   }

   public static List getObjectUuidsOnVsan(JsonNode disksProperties, String diskVsanUuid) {
      return PhysicalDiskJsonParser.getObjectUuids(disksProperties, diskVsanUuid);
   }

   public static List getObjectUuidsOnVsanDirect(VsanManagedStorageObjUuidMapping[] storageToObjectUuids, String storageId) {
      return getManagedObjectUuids(storageToObjectUuids, "vsandirect", storageId);
   }

   public static List getObjectUuidsOnPmem(VsanManagedStorageObjUuidMapping[] storageToObjectUuids, String storageId) {
      return getManagedObjectUuids(storageToObjectUuids, "pmem", storageId);
   }

   public static String getPmemStorageId(VsanManagedStorageObjUuidMapping[] storageToObjectUuids, String objectId) {
      return ArrayUtils.isEmpty(storageToObjectUuids) ? null : (String)Arrays.stream(storageToObjectUuids).filter((mapping) -> {
         return mapping.storageType.equalsIgnoreCase("pmem");
      }).filter((mapping) -> {
         return ArrayUtils.isNotEmpty(mapping.uuids) && Arrays.stream(mapping.uuids).anyMatch((uuid) -> {
            return uuid.equalsIgnoreCase(objectId);
         });
      }).map((mapping) -> {
         return mapping.storageId;
      }).findFirst().orElse((Object)null);
   }

   public static VirtualObjectPlacementModel buildHostPlacement(Map props) {
      return buildHostPlacement(props, (String)null);
   }

   public static VirtualObjectPlacementModel buildHostPlacement(Map props, String nodeUuid) {
      VirtualObjectPlacementModel hostPlacement = new VirtualObjectPlacementModel();
      if (props != null) {
         hostPlacement.nodeUuid = props.get("config.vsanHostConfig.clusterInfo.nodeUuid").toString();
         hostPlacement.label = (String)props.get("name");
         hostPlacement.iconId = (String)props.get("primaryIconId");
         hostPlacement.faultDomain = (String)props.get("config.vsanHostConfig.faultDomainInfo.name");
         hostPlacement.navigationTarget = (ManagedObjectReference)props.get("__resourceObject");
      } else {
         hostPlacement.nodeUuid = nodeUuid;
         hostPlacement.label = nodeUuid;
         hostPlacement.iconId = "vsphere-icon-host-error";
      }

      return hostPlacement;
   }

   private static List getManagedObjectUuids(VsanManagedStorageObjUuidMapping[] storageToObjectUuids, String storageType, String storageId) {
      if (ArrayUtils.isEmpty(storageToObjectUuids)) {
         return new ArrayList();
      } else {
         VsanManagedStorageObjUuidMapping foundStorageToObjectUuids = (VsanManagedStorageObjUuidMapping)Arrays.stream(storageToObjectUuids).filter((mapping) -> {
            return mapping.storageType.equalsIgnoreCase(storageType) && mapping.storageId.equalsIgnoreCase(storageId) && ArrayUtils.isNotEmpty(mapping.uuids);
         }).findFirst().orElse((Object)null);
         return (List)(foundStorageToObjectUuids != null ? (List)Arrays.stream(foundStorageToObjectUuids.uuids).collect(Collectors.toList()) : new ArrayList());
      }
   }

   public static VsanObjectHealthState aggregateWrapperObjectHealthV1(VirtualObjectModel wrapperModel) {
      VsanObjectHealthState result = null;
      VirtualObjectModel[] var2 = wrapperModel.children;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         VirtualObjectModel child = var2[var4];
         if (child.healthState != null) {
            if (result == null) {
               result = child.healthState;
            } else if (child.healthState.ordinal() > result.ordinal()) {
               result = child.healthState;
            }
         }
      }

      return result;
   }

   public static VsanObjectCompositeHealth aggregateWrapperObjectHealthV2(VirtualObjectModel wrapperModel) {
      VsanObjectCompositeHealth result = null;
      VirtualObjectModel[] var2 = wrapperModel.children;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         VirtualObjectModel child = var2[var4];
         if (child.compositeHealth != null) {
            if (result == null) {
               result = new VsanObjectCompositeHealth();
            }

            if (ObjectHealthComplianceState.INACCESSIBLE_V2.equals(child.compositeHealth.complianceState)) {
               result.complianceState = child.compositeHealth.complianceState;
               result.incomplianceReason = null;
               result.rebuildState = null;
               result.policyState = null;
               return result;
            }

            result.complianceState = (ObjectHealthComplianceState)getWorseState(child.compositeHealth.complianceState, result.complianceState);
            result.incomplianceReason = (ObjectHealthIncomplianceReason)getWorseState(child.compositeHealth.incomplianceReason, result.incomplianceReason);
            result.rebuildState = (ObjectHealthRebuildState)getWorseState(child.compositeHealth.rebuildState, result.rebuildState);
            result.policyState = (ObjectHealthPolicyState)getWorseState(child.compositeHealth.policyState, result.policyState);
         }
      }

      return result;
   }

   public static String getCommonStoragePolicy(VirtualObjectModel wrapperModel) {
      if (ArrayUtils.isEmpty(wrapperModel.children)) {
         return null;
      } else {
         String result = wrapperModel.children[0].storagePolicy;
         return StringUtils.isNotBlank(result) && Arrays.stream(wrapperModel.children).allMatch((child) -> {
            return result.equals(child.storagePolicy);
         }) ? result : null;
      }
   }

   private static Enum getWorseState(Enum valueA, Enum valueB) {
      if (valueA == null) {
         return valueB;
      } else if (valueB == null) {
         return valueA;
      } else {
         return valueA.ordinal() > valueB.ordinal() ? valueA : valueB;
      }
   }
}
