package com.vmware.vsphere.client.vsan.dataprovider;

import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vim.vsan.host.VsanRuntimeInfo;
import com.vmware.vim.binding.vim.vsan.host.VsanRuntimeInfo.DiskIssue;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanManagedStorageObjUuidMapping;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanDirectStorage;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedDisksInfo;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedPMemInfo;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanScsiDisk;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementUtil;
import com.vmware.vsan.client.services.diskmanagement.DiskStatusUtil;
import com.vmware.vsan.client.services.diskmanagement.PmemDiskData;
import com.vmware.vsan.client.services.diskmanagement.managedstorage.ManagedStorageObjectsService;
import com.vmware.vsan.client.services.diskmanagement.pmem.PmemService;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsUtil;
import com.vmware.vsan.client.util.HostClusterStatusUtils;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.base.util.Constants;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import com.vmware.vsphere.client.vsan.data.VsanDirectDiskGroupData;
import com.vmware.vsphere.client.vsan.data.VsanDiskAndGroupData;
import com.vmware.vsphere.client.vsan.data.VsanDiskData;
import com.vmware.vsphere.client.vsan.data.VsanDiskGroupData;
import com.vmware.vsphere.client.vsan.data.VsanSemiAutoClaimDisksData;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanHostPropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   public static final String VSAN_HOST_CLUSTER_STATUS_PROPERTY = "vsanHostClusterStatus";
   public static final String VSAN_HOST_CLUSTER_VSAN_ENABLED_OR_COMPUTE_ONLY = "vsanHostClusterVsanEnabledOrComputeOnly";
   public static final String HOST_VSAN_RUNTIME_INFO = "runtime.vsanRuntimeInfo";
   public static final String STORAGE_ADAPTER_DEVICES = "storageAdapterDevices";
   private static final Log _logger = LogFactory.getLog(VsanHostPropertyProviderAdapter.class);
   @Autowired
   private PmemService pmemService;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private ManagedStorageObjectsService managedStorageObjectsService;
   @Autowired
   private CsdService csdService;

   public VsanHostPropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo hostInfo = new TypeInfo();
      hostInfo.type = HostSystem.class.getSimpleName();
      hostInfo.properties = new String[]{"vsanDisksAndGroupsData", "vsanSemiAutoClaimDisksData", "vsanHostClusterStatus", "vsanHostClusterVsanEnabledOrComputeOnly", "vsanPhysicalDiskVirtualMapping", "vsanStorageAdapterDevices"};
      TypeInfo[] providedProperties = new TypeInfo[]{hostInfo};
      registry.registerDataAdapter(this, providedProperties);
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      List hosts = new ArrayList(QueryUtil.getObjectRefs(propertyRequest.objects));
      String[] properties = QueryUtil.getPropertyNames(propertyRequest.properties);

      try {
         Measure measure = new Measure("Retrieve the dependencies needed for the host property provider adapter");
         Throwable var23 = null;

         ResultSet var10;
         try {
            ManagedObjectReference clusterRef = BaseUtils.getCluster((ManagedObjectReference)hosts.get(0));
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef);
            VsanHostPropertyProviderAdapter.AsyncTasks asyncTasks = this.startLoadingDataAsync(clusterRef, hosts, properties, dataRetriever);
            DataServiceResponse propsFromDS = this.requestPropertiesFromDS(hosts, properties);
            var10 = this.getPropertiesResult(clusterRef, hosts, properties, dataRetriever, propsFromDS, asyncTasks);
         } catch (Throwable var20) {
            var23 = var20;
            throw var20;
         } finally {
            if (measure != null) {
               if (var23 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var19) {
                     var23.addSuppressed(var19);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var10;
      } catch (Exception var22) {
         _logger.error("Could not fetch the requested properties: " + var22);
         ResultSet resultSet = new ResultSet();
         resultSet.error = var22;
         return resultSet;
      }
   }

   private VsanHostPropertyProviderAdapter.AsyncTasks startLoadingDataAsync(ManagedObjectReference clusterRef, List hostRefs, String[] properties, VsanAsyncDataRetriever dataRetriever) {
      VsanHostPropertyProviderAdapter.AsyncTasks asyncTasks = new VsanHostPropertyProviderAdapter.AsyncTasks();
      Arrays.stream(properties).forEach((property) -> {
         byte var7 = -1;
         switch(property.hashCode()) {
         case -1428978447:
            if (property.equals("vsanHostClusterVsanEnabledOrComputeOnly")) {
               var7 = 0;
            }
            break;
         case 260829889:
            if (property.equals("vsanPhysicalDiskVirtualMapping")) {
               var7 = 4;
            }
            break;
         case 920289082:
            if (property.equals("vsanHostClusterStatus")) {
               var7 = 3;
            }
            break;
         case 1063446345:
            if (property.equals("vsanDisksAndGroupsData")) {
               var7 = 1;
            }
            break;
         case 1457701451:
            if (property.equals("vsanSemiAutoClaimDisksData")) {
               var7 = 2;
            }
         }

         switch(var7) {
         case 0:
            if (clusterRef != null) {
               dataRetriever.loadConfigInfoEx();
            }
            break;
         case 1:
            dataRetriever.loadDisks(hostRefs).loadManagedDisks(hostRefs).loadDisksStatuses(hostRefs);
            asyncTasks.hostToStorageObjUuidMapping = this.managedStorageObjectsService.getHostToStorageObjUuidMapping(clusterRef, hostRefs);
            break;
         case 2:
            dataRetriever.loadDisks(hostRefs).loadManagedDisks(hostRefs);
            break;
         case 3:
            dataRetriever.loadHostClusterStatus(hostRefs);
            break;
         case 4:
            dataRetriever.loadDisksProperties("DISKS_VIRTUAL_MAPPING_QUERY", Constants.PHYSICAL_DISK_VIRTUAL_MAPPING_PROPERTIES, hostRefs);
         }

      });
      return asyncTasks;
   }

   private DataServiceResponse requestPropertiesFromDS(List hostRefs, String[] properties) throws Exception {
      Set dsPropertiesToRequest = new HashSet();
      Arrays.stream(properties).forEach((property) -> {
         byte var3 = -1;
         switch(property.hashCode()) {
         case 1063446345:
            if (property.equals("vsanDisksAndGroupsData")) {
               var3 = 0;
            }
            break;
         case 1496642655:
            if (property.equals("vsanStorageAdapterDevices")) {
               var3 = 1;
            }
         }

         switch(var3) {
         case 0:
            dsPropertiesToRequest.add("runtime.vsanRuntimeInfo");
            break;
         case 1:
            dsPropertiesToRequest.add("storageAdapterDevices");
         }

      });
      return dsPropertiesToRequest.isEmpty() ? null : QueryUtil.getProperties((ManagedObjectReference[])hostRefs.toArray(new ManagedObjectReference[0]), (String[])dsPropertiesToRequest.toArray(new String[0]));
   }

   private ResultSet getPropertiesResult(ManagedObjectReference clusterRef, List hostRefs, String[] properties, VsanAsyncDataRetriever dataRetriever, DataServiceResponse propsFromDS, VsanHostPropertyProviderAdapter.AsyncTasks asyncTasks) throws Exception {
      ResultItem[] resultItems = (ResultItem[])hostRefs.stream().map((hostRefx) -> {
         ResultItem resultItem = new ResultItem();
         resultItem.resourceObject = hostRefx;
         resultItem.properties = new PropertyValue[properties.length];
         return resultItem;
      }).toArray((x$0) -> {
         return new ResultItem[x$0];
      });

      for(int propertyIndex = 0; propertyIndex < properties.length; ++propertyIndex) {
         String property = properties[propertyIndex];
         Map propertyValues = new HashMap();
         byte var12 = -1;
         switch(property.hashCode()) {
         case -1428978447:
            if (property.equals("vsanHostClusterVsanEnabledOrComputeOnly")) {
               var12 = 0;
            }
            break;
         case 260829889:
            if (property.equals("vsanPhysicalDiskVirtualMapping")) {
               var12 = 4;
            }
            break;
         case 920289082:
            if (property.equals("vsanHostClusterStatus")) {
               var12 = 3;
            }
            break;
         case 1063446345:
            if (property.equals("vsanDisksAndGroupsData")) {
               var12 = 1;
            }
            break;
         case 1457701451:
            if (property.equals("vsanSemiAutoClaimDisksData")) {
               var12 = 2;
            }
            break;
         case 1496642655:
            if (property.equals("vsanStorageAdapterDevices")) {
               var12 = 5;
            }
         }

         switch(var12) {
         case 0:
            propertyValues = this.getVsanHostClusterVsanEnabledOrComputeOnly(clusterRef, hostRefs, dataRetriever);
            break;
         case 1:
            propertyValues = this.getHostsStorageAndGroupsData(clusterRef, hostRefs, dataRetriever, propsFromDS, asyncTasks);
            break;
         case 2:
            propertyValues = this.getVsanHostsSemiAutoClaimDisksData(clusterRef, hostRefs, dataRetriever);
            break;
         case 3:
            propertyValues = this.getVsanHostsClusterStatuses(dataRetriever);
            break;
         case 4:
            propertyValues = this.getVsanPhysicalDiskVirtualMapping(dataRetriever);
            break;
         case 5:
            propertyValues = this.getVsanHostsStorageAdapterDevices(hostRefs, propsFromDS);
            break;
         default:
            _logger.warn("Skipping unknown property: " + property);
         }

         ResultItem[] var11 = resultItems;
         int var16 = resultItems.length;

         for(int var13 = 0; var13 < var16; ++var13) {
            ResultItem resultItem = var11[var13];
            ManagedObjectReference hostRef = (ManagedObjectReference)resultItem.resourceObject;
            resultItem.properties[propertyIndex] = QueryUtil.createPropValue(property, ((Map)propertyValues).get(hostRef), hostRef);
         }
      }

      return QueryUtil.newResultSet(resultItems);
   }

   private Map getVsanHostClusterVsanEnabledOrComputeOnly(ManagedObjectReference clusterRef, List hostRefs, VsanAsyncDataRetriever dataRetriever) {
      boolean value;
      if (clusterRef == null) {
         value = false;
      } else {
         ConfigInfoEx configInfoEx = dataRetriever.getConfigInfoEx();
         if (configInfoEx == null) {
            value = false;
         } else if (BooleanUtils.isTrue(configInfoEx.enabled)) {
            value = true;
         } else {
            value = this.csdService.isComputeOnlyClusterByConfigInfoEx(configInfoEx);
         }
      }

      return (Map)hostRefs.stream().collect(Collectors.toMap(Function.identity(), (val) -> {
         return value;
      }));
   }

   private Map getVsanHostsClusterStatuses(VsanAsyncDataRetriever dataRetriever) {
      return HostClusterStatusUtils.aggregateClusterStatus(dataRetriever.getHostClusterStatus());
   }

   private Map getVsanPhysicalDiskVirtualMapping(VsanAsyncDataRetriever dataRetriever) {
      return dataRetriever.getDisksProperties("DISKS_VIRTUAL_MAPPING_QUERY");
   }

   private Map getVsanHostsStorageAdapterDevices(List hostRefs, DataServiceResponse propsFromDS) {
      return (Map)hostRefs.stream().collect(Collectors.toMap((hostRef) -> {
         return hostRef;
      }, (hostRef) -> {
         return (Object[])propsFromDS.getProperty(hostRef, "storageAdapterDevices");
      }));
   }

   private Map getVsanHostsSemiAutoClaimDisksData(ManagedObjectReference clusterRef, List hostRefs, VsanAsyncDataRetriever dataRetriever) {
      HashMap hostToSemiAutoClaimDisksData = new HashMap();
      Map hostToDisks = dataRetriever.getDisks();
      Map hostToPmemStorage = this.pmemService.getPmemStorage(clusterRef, false);
      Map hostToManagedDisks = dataRetriever.getManagedDisks();
      hostRefs.forEach((hostRef) -> {
         VsanSemiAutoClaimDisksData disksData = DiskManagementUtil.getNotClaimedDisksData(hostRef, (DiskResult[])hostToDisks.get(hostRef), (List)hostToPmemStorage.get(hostRef), (VsanManagedDisksInfo)hostToManagedDisks.get(hostRef));
         hostToSemiAutoClaimDisksData.put(hostRef, disksData);
      });
      return hostToSemiAutoClaimDisksData;
   }

   private Map getHostsStorageAndGroupsData(ManagedObjectReference clusterRef, List hostsRefs, VsanAsyncDataRetriever dataRetriever, DataServiceResponse propsFromDS, VsanHostPropertyProviderAdapter.AsyncTasks asyncTasks) throws Exception {
      Map hostToDisksAndGroupData = new HashMap();
      Map hostToDisks = dataRetriever.getDisks();
      Map hostToDiskMappingData = dataRetriever.getManagedDisks();
      Map hostToVsanDiskStatuses = dataRetriever.getDisksStatuses();
      Map hostToPmemStorage = this.pmemService.getPmemStorage(clusterRef, true);
      Map hostToStorageObjUuidMapping = (Map)asyncTasks.hostToStorageObjUuidMapping.get();
      hostsRefs.forEach((hostRef) -> {
         Map connectedDisksData = this.getConnectedDisks((DiskResult[])hostToDisks.get(hostRef), getDisksIssuesOnHost((VsanRuntimeInfo)propsFromDS.getProperty(hostRef, "runtime.vsanRuntimeInfo")), (Map)hostToVsanDiskStatuses.get(hostRef));
         List pmemStorage = (List)hostToPmemStorage.get(hostRef);
         VsanManagedStorageObjUuidMapping[] storageToObjectUuids = (VsanManagedStorageObjUuidMapping[])hostToStorageObjUuidMapping.get(hostRef);
         if (connectedDisksData.isEmpty() && CollectionUtils.isEmpty(pmemStorage)) {
            hostToDisksAndGroupData.put(hostRef, (Object)null);
         } else {
            VsanDiskAndGroupData disksGroupsData = new VsanDiskAndGroupData();
            Map isDiskInDiskGroupMap = new HashMap();
            disksGroupsData.connectedDisks = (VsanDiskData[])connectedDisksData.values().toArray(new VsanDiskData[0]);
            VsanManagedDisksInfo managedDisks = (VsanManagedDisksInfo)hostToDiskMappingData.get(hostRef);
            disksGroupsData.vsanGroups = this.getVsanDiskGroups(managedDisks.vSANDiskMapInfo, isDiskInDiskGroupMap, connectedDisksData);
            disksGroupsData.vsanDirectDiskGroupData = this.getVsanDirectDiskGroup(managedDisks.vSANDirectDisks, isDiskInDiskGroupMap, connectedDisksData, storageToObjectUuids);
            disksGroupsData.managedPmemStorage = this.getManagedPmemStorage(managedDisks.vSANPMemInfo, pmemStorage, storageToObjectUuids);
            setStandaloneDisks(connectedDisksData, isDiskInDiskGroupMap, disksGroupsData);
            hostToDisksAndGroupData.put(hostRef, disksGroupsData);
         }
      });
      return hostToDisksAndGroupData;
   }

   private static void setStandaloneDisks(Map connectedDisksData, Map isDiskInDiskGroupMap, VsanDiskAndGroupData disksGroupsData) {
      List vsanDisksData = new ArrayList();
      List disksNotInUseData = new ArrayList();
      List ineligibleDisksData = new ArrayList();
      Iterator var6 = connectedDisksData.values().iterator();

      while(var6.hasNext()) {
         VsanDiskData diskData = (VsanDiskData)var6.next();
         if (diskData != null && diskData.disk != null) {
            String diskId = diskData.disk.uuid;
            if (!isDiskInDiskGroupMap.containsKey(diskId)) {
               if (diskData.ineligible) {
                  ineligibleDisksData.add(diskData);
               } else if (diskData.inUse) {
                  vsanDisksData.add(diskData);
               } else {
                  disksNotInUseData.add(diskData);
               }
            }
         }
      }

      if (ineligibleDisksData.size() > 0) {
         disksGroupsData.ineligibleDisks = (VsanDiskData[])ineligibleDisksData.toArray(new VsanDiskData[0]);
      }

      if (disksNotInUseData.size() > 0) {
         disksGroupsData.disksNotInUse = (VsanDiskData[])disksNotInUseData.toArray(new VsanDiskData[0]);
      }

      if (vsanDisksData.size() > 0) {
         disksGroupsData.vsanDisks = (VsanDiskData[])vsanDisksData.toArray(new VsanDiskData[0]);
      }

   }

   private static HashMap getDisksIssuesOnHost(VsanRuntimeInfo runtimeInfo) {
      if (runtimeInfo != null && !ArrayUtils.isEmpty(runtimeInfo.diskIssues)) {
         HashMap issuesMap = new HashMap();
         DiskIssue[] var2 = runtimeInfo.diskIssues;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            DiskIssue diskIssue = var2[var4];
            String diskId = diskIssue.diskId;
            Object issues;
            if (issuesMap.containsKey(diskId)) {
               issues = (List)issuesMap.get(diskId);
            } else {
               issues = new ArrayList();
               issuesMap.put(diskId, issues);
            }

            ((List)issues).add(diskIssue.issue);
         }

         return issuesMap;
      } else {
         return null;
      }
   }

   private VsanDiskGroupData[] getVsanDiskGroups(DiskMapInfoEx[] diskMappingData, Map isDiskInDiskGroup, Map connectedDisksData) {
      if (connectedDisksData != null && connectedDisksData.size() != 0) {
         if (ArrayUtils.isEmpty(diskMappingData)) {
            return null;
         } else {
            VsanDiskGroupData[] groups = new VsanDiskGroupData[diskMappingData.length];
            int i = 0;
            DiskMapInfoEx[] var6 = diskMappingData;
            int var7 = diskMappingData.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               DiskMapInfoEx diskMapInfoEx = var6[var8];
               DiskMapping mapping = diskMapInfoEx.mapping;
               String ssdId = mapping.ssd.uuid;
               VsanDiskGroupData groupData = new VsanDiskGroupData();
               groupData.ssd = (VsanDiskData)connectedDisksData.get(ssdId);
               groupData.ssd.diskGroupUuid = ssdId;
               groupData.ssd.isCacheDisk = true;
               groupData.mounted = diskMapInfoEx.isMounted;
               groupData.encrypted = diskMapInfoEx.encryptionInfo != null && diskMapInfoEx.encryptionInfo.encryptionEnabled;
               groupData.unlockedEncrypted = diskMapInfoEx.unlockedEncrypted != null && diskMapInfoEx.unlockedEncrypted;
               groupData.isAllFlash = diskMapInfoEx.isAllFlash;
               isDiskInDiskGroup.put(ssdId, true);
               ArrayList disks = new ArrayList();
               ScsiDisk[] var14 = mapping.nonSsd;
               int var15 = var14.length;

               for(int var16 = 0; var16 < var15; ++var16) {
                  ScsiDisk disk = var14[var16];
                  String diskId = disk.uuid;
                  VsanDiskData diskData = (VsanDiskData)connectedDisksData.get(diskId);
                  diskData.diskGroupUuid = ssdId;
                  diskData.isCacheDisk = false;
                  disks.add(diskData);
                  isDiskInDiskGroup.put(diskId, true);
               }

               groupData.disks = (VsanDiskData[])disks.toArray(new VsanDiskData[0]);
               groups[i++] = groupData;
            }

            return groups;
         }
      } else {
         return null;
      }
   }

   private VsanDirectDiskGroupData getVsanDirectDiskGroup(VsanDirectStorage[] vsanDirectStorages, Map isDiskInDiskGroupMap, Map connectedDisksData, VsanManagedStorageObjUuidMapping[] storageToObjectUuids) {
      if (!DiskManagementUtil.hasClaimedDisks(vsanDirectStorages)) {
         return null;
      } else {
         VsanDirectDiskGroupData vsanDirectDiskGroupData = new VsanDirectDiskGroupData();
         vsanDirectDiskGroupData.disks = new ArrayList();
         vsanDirectDiskGroupData.diskToCapacity = new HashMap();
         vsanDirectDiskGroupData.diskToObjectUuids = new HashMap();
         VsanScsiDisk[] vsanDirectDisks = (VsanScsiDisk[])Arrays.stream(vsanDirectStorages).filter((storage) -> {
            return ArrayUtils.isNotEmpty(storage.scsiDisks);
         }).flatMap((storage) -> {
            return Arrays.stream(storage.scsiDisks);
         }).toArray((x$0) -> {
            return new VsanScsiDisk[x$0];
         });
         VsanScsiDisk[] var7 = vsanDirectDisks;
         int var8 = vsanDirectDisks.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            VsanScsiDisk vsanDirectDisk = var7[var9];
            isDiskInDiskGroupMap.put(vsanDirectDisk.getUuid(), true);
            VsanDiskData disk = (VsanDiskData)connectedDisksData.get(vsanDirectDisk.getUuid());
            disk.inUse = true;
            disk.ineligible = false;
            disk.diskStatus = DiskStatusUtil.getVsanDirectDiskStatus(vsanDirectDisk);
            vsanDirectDiskGroupData.disks.add(disk);
            vsanDirectDiskGroupData.diskToCapacity.put(vsanDirectDisk.getUuid(), DiskManagementUtil.getVsanDirectDiskCapacity(vsanDirectDisk));
            vsanDirectDiskGroupData.diskToObjectUuids.put(vsanDirectDisk.getUuid(), VirtualObjectsUtil.getObjectUuidsOnVsanDirect(storageToObjectUuids, vsanDirectDisk.getUuid()));
         }

         return vsanDirectDiskGroupData;
      }
   }

   private PmemDiskData[] getManagedPmemStorage(VsanManagedPMemInfo vSANPMemInfo, List pmemStorage, VsanManagedStorageObjUuidMapping[] storageToObjectUuids) {
      return CollectionUtils.isEmpty(pmemStorage) ? null : (PmemDiskData[])pmemStorage.stream().filter((storage) -> {
         return DiskManagementUtil.isPmemStorageInUse(storage.dsRef, vSANPMemInfo);
      }).map((storage) -> {
         PmemDiskData pmemDiskData = new PmemDiskData(storage, storageToObjectUuids);
         pmemDiskData.claimOption = ClaimOption.PMEM;
         return pmemDiskData;
      }).toArray((x$0) -> {
         return new PmemDiskData[x$0];
      });
   }

   private Map getConnectedDisks(DiskResult[] results, Map diskIssues, Map vsanDiskStatuses) {
      Map connectedDisks = new HashMap();
      if (ArrayUtils.isEmpty(results)) {
         return connectedDisks;
      } else {
         DiskResult[] var5 = results;
         int var6 = results.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            DiskResult result = var5[var7];
            connectedDisks.put(result.disk.uuid, new VsanDiskData(result, diskIssues, vsanDiskStatuses));
         }

         return connectedDisks;
      }
   }

   class AsyncTasks {
      CompletableFuture hostToStorageObjUuidMapping;
   }
}
