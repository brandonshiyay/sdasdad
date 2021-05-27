package com.vmware.vsan.client.services.physicaldisks;

import com.fasterxml.jackson.databind.JsonNode;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.ObjectIdentityConstraint;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RelationalConstraint;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vsan.client.services.diskmanagement.PmemDiskData;
import com.vmware.vsan.client.services.diskmanagement.StorageCapacity;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.data.HostPhysicalMappingData;
import com.vmware.vsphere.client.vsan.data.PhysicalDiskData;
import com.vmware.vsphere.client.vsan.data.VsanDiskAndGroupData;
import com.vmware.vsphere.client.vsan.data.VsanDiskData;
import com.vmware.vsphere.client.vsan.data.VsanDiskGroupData;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class VsanDiskMappingsProvider {
   private static final Log logger = LogFactory.getLog(VsanDiskMappingsProvider.class);
   private static final String[] PHYSICAL_DISK_MAPPINGS_HOST_PROPERTIES = new String[]{"vsanDisksAndGroupsData", "vsanPhysicalDiskVirtualMapping", "vsanStorageAdapterDevices", "name", "config.vsanHostConfig.faultDomainInfo.name", "primaryIconId"};

   public List getVsanHostsPhysicalDiskData(ManagedObjectReference clusterRef) throws Exception {
      Measure measure = new Measure("getVsanHostsPhysicalDiskData");
      Throwable var3 = null;

      try {
         QuerySpec querySpec = this.getClusterHostsQuerySpec(clusterRef, PHYSICAL_DISK_MAPPINGS_HOST_PROPERTIES);
         Measure getClusterHostsProperties = measure.start("getClusterHostsProperties");
         Throwable var7 = null;

         ResultItem[] resultItems;
         try {
            resultItems = QueryUtil.getData(querySpec).items;
         } catch (Throwable var35) {
            var7 = var35;
            throw var35;
         } finally {
            if (getClusterHostsProperties != null) {
               if (var7 != null) {
                  try {
                     getClusterHostsProperties.close();
                  } catch (Throwable var34) {
                     var7.addSuppressed(var34);
                  }
               } else {
                  getClusterHostsProperties.close();
               }
            }

         }

         if (resultItems == null) {
            List var40 = Collections.emptyList();
            return var40;
         } else {
            List hostPhysicalMappingsData = new ArrayList();
            ResultItem[] var41 = resultItems;
            int var8 = resultItems.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               ResultItem resultItem = var41[var9];
               ManagedObjectReference hostRef = (ManagedObjectReference)resultItem.resourceObject;
               VsanDiskMappingsProvider.HostMappingData hostMappingData = this.getHostMappingData(resultItem);
               List hostDisks = this.getHostDisks(hostMappingData.diskAndGroupData, hostMappingData.json, hostRef, clusterRef);
               HostPhysicalMappingData hostDisksData = new HostPhysicalMappingData(clusterRef, hostRef, hostMappingData.hostName, hostMappingData.primaryIconId, hostDisks, hostMappingData.vsanStorageAdapterDevices, hostMappingData.faultDomain);
               hostPhysicalMappingsData.add(hostDisksData);
            }

            ArrayList var42 = hostPhysicalMappingsData;
            return var42;
         }
      } catch (Throwable var37) {
         var3 = var37;
         throw var37;
      } finally {
         if (measure != null) {
            if (var3 != null) {
               try {
                  measure.close();
               } catch (Throwable var33) {
                  var3.addSuppressed(var33);
               }
            } else {
               measure.close();
            }
         }

      }
   }

   private QuerySpec getClusterHostsQuerySpec(ManagedObjectReference clusterRef, String[] properties) {
      ObjectIdentityConstraint clusterConstraint = QueryUtil.createObjectIdentityConstraint(clusterRef);
      RelationalConstraint clusterHostsConstraint = QueryUtil.createRelationalConstraint("host", clusterConstraint, true, HostSystem.class.getSimpleName());
      QuerySpec querySpecHosts = QueryUtil.buildQuerySpec((Constraint)clusterHostsConstraint, properties);
      return querySpecHosts;
   }

   private VsanDiskMappingsProvider.HostMappingData getHostMappingData(ResultItem resultItem) {
      String jsonString = "";
      VsanDiskAndGroupData diskAndGroupData = null;
      Object[] vsanStorageAdapterDevices = null;
      String hostName = "";
      String primaryIconId = "";
      String faultDomain = "";
      PropertyValue[] var8 = resultItem.properties;
      int var9 = var8.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         PropertyValue propValue = var8[var10];
         String var12 = propValue.propertyName;
         byte var13 = -1;
         switch(var12.hashCode()) {
         case -826278890:
            if (var12.equals("primaryIconId")) {
               var13 = 4;
            }
            break;
         case 3373707:
            if (var12.equals("name")) {
               var13 = 3;
            }
            break;
         case 260829889:
            if (var12.equals("vsanPhysicalDiskVirtualMapping")) {
               var13 = 1;
            }
            break;
         case 707737491:
            if (var12.equals("config.vsanHostConfig.faultDomainInfo.name")) {
               var13 = 5;
            }
            break;
         case 1063446345:
            if (var12.equals("vsanDisksAndGroupsData")) {
               var13 = 0;
            }
            break;
         case 1496642655:
            if (var12.equals("vsanStorageAdapterDevices")) {
               var13 = 2;
            }
         }

         switch(var13) {
         case 0:
            diskAndGroupData = (VsanDiskAndGroupData)propValue.value;
            break;
         case 1:
            jsonString = (String)propValue.value;
            break;
         case 2:
            vsanStorageAdapterDevices = (Object[])((Object[])propValue.value);
            break;
         case 3:
            hostName = (String)propValue.value;
            break;
         case 4:
            primaryIconId = (String)propValue.value;
            break;
         case 5:
            faultDomain = (String)propValue.value;
            break;
         default:
            logger.warn("Unknown property received: " + propValue.propertyName + " = " + propValue.value);
         }
      }

      return new VsanDiskMappingsProvider.HostMappingData(jsonString, diskAndGroupData, vsanStorageAdapterDevices, hostName, primaryIconId, faultDomain);
   }

   private List getHostDisks(VsanDiskAndGroupData diskAndGroupData, JsonNode json, ManagedObjectReference hostRef, ManagedObjectReference clusterRef) {
      if (diskAndGroupData == null) {
         return null;
      } else {
         List hostDisks = new ArrayList();
         int var7;
         int var8;
         if (diskAndGroupData.vsanGroups != null) {
            VsanDiskGroupData[] var6 = diskAndGroupData.vsanGroups;
            var7 = var6.length;

            for(var8 = 0; var8 < var7; ++var8) {
               VsanDiskGroupData groupData = var6[var8];
               hostDisks.addAll(this.getPhysicalDisksManagedByVsan(groupData, json, hostRef, clusterRef));
            }
         }

         PhysicalDiskData physicalDiskData;
         if (diskAndGroupData.vsanDirectDiskGroupData != null) {
            Iterator var11 = diskAndGroupData.vsanDirectDiskGroupData.disks.iterator();

            while(var11.hasNext()) {
               VsanDiskData diskData = (VsanDiskData)var11.next();
               StorageCapacity diskCapacity = (StorageCapacity)diskAndGroupData.vsanDirectDiskGroupData.diskToCapacity.get(diskData.disk.uuid);
               List objectUuids = (List)diskAndGroupData.vsanDirectDiskGroupData.diskToObjectUuids.get(diskData.disk.uuid);
               physicalDiskData = PhysicalDiskData.fromVsanDirectDisk(diskData, diskCapacity, objectUuids, hostRef, clusterRef);
               hostDisks.add(physicalDiskData);
            }
         }

         if (ArrayUtils.isNotEmpty(diskAndGroupData.managedPmemStorage)) {
            PmemDiskData[] var12 = diskAndGroupData.managedPmemStorage;
            var7 = var12.length;

            for(var8 = 0; var8 < var7; ++var8) {
               PmemDiskData pmemStorage = var12[var8];
               physicalDiskData = PhysicalDiskData.fromPmemStorage(pmemStorage, hostRef, clusterRef);
               hostDisks.add(physicalDiskData);
            }
         }

         return hostDisks;
      }
   }

   private List getPhysicalDisksManagedByVsan(VsanDiskGroupData groupData, JsonNode json, ManagedObjectReference hostRef, ManagedObjectReference clusterRef) {
      List hostDisks = new ArrayList();
      if (groupData.disks == null) {
         return hostDisks;
      } else {
         PhysicalDiskData cacheHostDisk = PhysicalDiskData.fromVsanDisk(groupData.ssd, json, hostRef, clusterRef);
         hostDisks.add(cacheHostDisk);
         VsanDiskData[] var7 = groupData.disks;
         int var8 = var7.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            VsanDiskData diskData = var7[var9];
            PhysicalDiskData capacityHostDisk = PhysicalDiskData.fromVsanDisk(diskData, json, hostRef, clusterRef);
            capacityHostDisk.vsanDiskGroupUuid = cacheHostDisk.vsanDiskGroupUuid;
            hostDisks.add(capacityHostDisk);
         }

         return hostDisks;
      }
   }

   private class HostMappingData {
      public VsanDiskAndGroupData diskAndGroupData;
      public Object[] vsanStorageAdapterDevices;
      public String hostName;
      public String primaryIconId;
      public String faultDomain;
      public JsonNode json;

      public HostMappingData(String jsonString, VsanDiskAndGroupData diskAndGroupData, Object[] vsanStorageAdapterDevices, String hostName, String primaryIconId, String faultDomain) {
         this.json = Utils.getJsonRootNode(jsonString);
         this.diskAndGroupData = diskAndGroupData;
         this.vsanStorageAdapterDevices = vsanStorageAdapterDevices;
         this.hostName = hostName;
         this.primaryIconId = primaryIconId;
         this.faultDomain = faultDomain;
      }
   }
}
