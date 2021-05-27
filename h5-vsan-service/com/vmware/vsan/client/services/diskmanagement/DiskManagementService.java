package com.vmware.vsan.client.services.diskmanagement;

import com.google.common.collect.Sets;
import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.host.StorageDeviceInfo;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Adapter;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Path;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Target;
import com.vmware.vim.binding.vim.vsan.host.ClusterStatus;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vim.vsan.host.DiskResult.State;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VSANWitnessHostInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanManagedStorageObjUuidMapping;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedDisksInfo;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.diskmanagement.managedstorage.ManagedStorageObjectsService;
import com.vmware.vsan.client.services.diskmanagement.pmem.PmemService;
import com.vmware.vsan.client.services.stretchedcluster.model.VsanHostsResult;
import com.vmware.vsan.client.util.HostClusterStatusUtils;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.dataservice.query.DataServiceHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.data.VsanCapabilityData;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.base.util.Constants;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterService;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class DiskManagementService {
   private static final Logger logger = LoggerFactory.getLogger(DiskManagementService.class);
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanStretchedClusterService stretchedClusterService;
   @Autowired
   private PmemService pmemService;
   @Autowired
   private ManagedStorageObjectsService managedStorageObjectsService;
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private DataServiceHelper dataServiceHelper;

   @TsService
   public List listHosts(ManagedObjectReference objRef) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objRef);

      try {
         Measure measure = new Measure("Collect Disk Mappings");
         Throwable var4 = null;

         ArrayList var39;
         try {
            VsanHostsResult vsanHosts = this.stretchedClusterService.collectVsanHosts(clusterRef, measure);
            List allHosts = new ArrayList(vsanHosts.getAll());
            if (allHosts.isEmpty()) {
               List var40 = Collections.EMPTY_LIST;
               return var40;
            }

            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadDisks((List)allHosts).loadManagedDisks((List)allHosts).loadHostCapabilities(allHosts).loadHostClusterStatus(allHosts).loadDisksStatuses(allHosts).loadHostStorageDeviceInfos(allHosts).loadDisksProperties(Constants.PHYSICAL_DISK_VIRTUAL_MAPPING_PROPERTIES, allHosts);
            CompletableFuture hostToStorageObjUuidMappingFuture = this.managedStorageObjectsService.getHostToStorageObjUuidMapping(clusterRef, allHosts);
            Measure hostPropsMeasure = measure.start("Query data service host properties.");
            Map hostDsProperties = QueryUtil.getProperties((ManagedObjectReference[])allHosts.toArray(new ManagedObjectReference[0]), HostData.DS_HOST_PROPERTIES).getMap();
            hostPropsMeasure.close();
            Map hostClusterStates = HostClusterStatusUtils.aggregateClusterStatus(dataRetriever.getHostClusterStatus());
            Collection networkPartitions = new HashSet();
            Iterator var13 = hostClusterStates.values().iterator();

            while(var13.hasNext()) {
               ClusterStatus status = (ClusterStatus)var13.next();
               if (status != null && status.memberUuid != null) {
                  networkPartitions.add(Sets.newHashSet(status.memberUuid));
               }
            }

            Map hostCapabilities = HostData.mapCapabilities(dataRetriever.getHostCapabilities(), clusterRef);
            Map vsanDisks = dataRetriever.getDisks();
            Map hostToManagedDisks = dataRetriever.getManagedDisks();
            Map hostToDisksStatusMap = dataRetriever.getDisksStatuses();
            Map deviceInfos = dataRetriever.getHostStorageDeviceInfos();
            Map hostToDisksProperties = dataRetriever.getDisksProperties();
            Map hostToPmemStorage = this.pmemService.getPmemStorage(clusterRef, false);
            Map hostToStorageObjUuidMapping = (Map)hostToStorageObjUuidMappingFuture.get();
            List result = new ArrayList();
            Iterator var22 = allHosts.iterator();

            while(var22.hasNext()) {
               ManagedObjectReference hostRef = (ManagedObjectReference)var22.next();
               if (((Map)hostDsProperties.get(hostRef)).get("name") != null) {
                  ClusterStatus status = (ClusterStatus)hostClusterStates.get(hostRef);
                  Integer partitionGroup = null;
                  if (status != null) {
                     partitionGroup = HostData.getNetworkPartitionGroup(status.nodeUuid, networkPartitions);
                  }

                  HostData hostData = HostData.create(hostRef, vsanHosts.witnesses.contains(hostRef), vsanHosts.metadataOnlyNodes.contains(hostRef), (Map)hostDsProperties.get(hostRef), (VsanManagedDisksInfo)hostToManagedDisks.get(hostRef), (DiskResult[])vsanDisks.get(hostRef), (Map)hostToDisksStatusMap.get(hostRef), (StorageDeviceInfo)deviceInfos.get(hostRef), status, partitionGroup, (VsanCapabilityData)hostCapabilities.get(hostRef), Utils.getJsonRootNode((String)hostToDisksProperties.get(hostRef)), (List)hostToPmemStorage.get(hostRef), (VsanManagedStorageObjUuidMapping[])hostToStorageObjUuidMapping.get(hostRef));
                  result.add(hostData);
               }
            }

            result.sort(Comparator.comparing(HostData::getName));
            var39 = result;
         } catch (Throwable var36) {
            var4 = var36;
            throw var36;
         } finally {
            if (measure != null) {
               if (var4 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var35) {
                     var4.addSuppressed(var35);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var39;
      } catch (Exception var38) {
         logger.error("Unable to get hosts' disks data.");
         throw new VsanUiLocalizableException(var38);
      }
   }

   @TsService
   public boolean hasVsanDirectDisks(ManagedObjectReference moRef) {
      List hostRefs = this.getHostRefs(moRef);
      if (CollectionUtils.isEmpty(hostRefs)) {
         return false;
      } else {
         try {
            Measure measure = new Measure("Collect disk mappings to check for vSAN Direct disks");
            Throwable var4 = null;

            boolean var24;
            try {
               VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, moRef).loadManagedDisks(hostRefs);
               Map managedDisksInfos = dataRetriever.getManagedDisks();
               Iterator var7 = hostRefs.iterator();

               while(var7.hasNext()) {
                  ManagedObjectReference hostRef = (ManagedObjectReference)var7.next();
                  VsanManagedDisksInfo managedDisksInfo = (VsanManagedDisksInfo)managedDisksInfos.get(hostRef);
                  if (managedDisksInfo != null && DiskManagementUtil.hasClaimedDisks(managedDisksInfo.vSANDirectDisks)) {
                     boolean var10 = true;
                     return var10;
                  }
               }

               var24 = false;
            } catch (Throwable var21) {
               var4 = var21;
               throw var21;
            } finally {
               if (measure != null) {
                  if (var4 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var20) {
                        var4.addSuppressed(var20);
                     }
                  } else {
                     measure.close();
                  }
               }

            }

            return var24;
         } catch (Exception var23) {
            logger.warn("Unable to fetch vSAN managed disks.", var23);
            return false;
         }
      }
   }

   private List getHostRefs(ManagedObjectReference moRef) {
      List hostRefs = new ArrayList();
      if (this.vmodlHelper.isCluster(moRef)) {
         try {
            PropertyValue[] hostProps = this.dataServiceHelper.getHostsWithConnectionState(moRef).getPropertyValues();
            VsanHostsResult hostsResult = new VsanHostsResult(hostProps, (VSANWitnessHostInfo[])null);
            hostRefs.addAll(hostsResult.connectedMembers);
         } catch (Exception var5) {
            logger.error("Unable to fetch cluster's hosts.", var5);
            return null;
         }
      } else if (this.vmodlHelper.isHost(moRef)) {
         hostRefs.add(moRef);
      }

      return hostRefs;
   }

   @TsService
   public boolean hasNetworkPartitionsOrDisconnectedMembers(ManagedObjectReference clusterRef) {
      HashSet hosts = new HashSet();

      try {
         Measure measure = new Measure("Has network partitions or disconnected memebers");
         Throwable var4 = null;

         boolean var26;
         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadWitnessHosts();
            PropertyValue[] hostProps = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), new String[]{"runtime.connectionState"}).getPropertyValues();
            if (ArrayUtils.isEmpty(hostProps)) {
               var26 = false;
               return var26;
            }

            PropertyValue[] var7 = hostProps;
            int var8 = hostProps.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               PropertyValue val = var7[var9];
               if (val.propertyName.equals("runtime.connectionState")) {
                  if (!ConnectionState.connected.equals(val.value)) {
                     boolean var27 = true;
                     return var27;
                  }

                  ManagedObjectReference hostRef = (ManagedObjectReference)val.resourceObject;
                  hosts.add(hostRef);
               }
            }

            var26 = this.hasNetworkPartition(clusterRef, dataRetriever, hosts);
         } catch (Throwable var23) {
            var4 = var23;
            throw var23;
         } finally {
            if (measure != null) {
               if (var4 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var22) {
                     var4.addSuppressed(var22);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var26;
      } catch (Exception var25) {
         logger.warn("Failed to list hosts, presumably empty cluster.", var25);
         return false;
      }
   }

   public boolean hasNetworkPartition(ManagedObjectReference clusterRef, VsanAsyncDataRetriever dataRetriever, Set hosts) {
      try {
         VSANWitnessHostInfo[] witnesses = dataRetriever.getWitnessHosts();
         if (witnesses != null) {
            VSANWitnessHostInfo[] var5 = witnesses;
            int var6 = witnesses.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               VSANWitnessHostInfo info = var5[var7];
               ManagedObjectReference witnessRef = new ManagedObjectReference(info.host.getType(), info.host.getValue(), clusterRef.getServerGuid());
               hosts.add(witnessRef);
            }
         }

         dataRetriever.loadHostClusterStatus(new ArrayList(hosts));
         Map hostClusterStates = HostClusterStatusUtils.aggregateClusterStatus(dataRetriever.getHostClusterStatus());
         Iterator var12 = hostClusterStates.values().iterator();

         while(var12.hasNext()) {
            ClusterStatus status = (ClusterStatus)var12.next();
            int members = status.memberUuid != null ? status.memberUuid.length : 0;
            if (members != hosts.size()) {
               return true;
            }
         }
      } catch (Exception var10) {
         logger.error("Failed to load witness hosts and cluster status. " + var10);
      }

      return false;
   }

   @TsService
   public List listEligibleDisks(ManagedObjectReference clusterRef, ManagedObjectReference hostRef, Boolean flashOnly) throws Throwable {
      Measure measure = new Measure("Collect Eligible Disks");
      Throwable var5 = null;

      try {
         List hostList = Arrays.asList(hostRef);
         VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadDisks(hostList).loadHostStorageDeviceInfos(hostList);
         Map vsanDisks = dataRetriever.getDisks();
         Map deviceInfos = dataRetriever.getHostStorageDeviceInfos();
         List availableDisks = new ArrayList();
         DiskResult[] var11 = (DiskResult[])vsanDisks.get(hostRef);
         int var12 = var11.length;

         for(int var13 = 0; var13 < var12; ++var13) {
            DiskResult d = var11[var13];
            if ((flashOnly == null || d.disk.ssd == flashOnly) && State.eligible == State.valueOf(d.state)) {
               StorageDeviceInfo deviceInfo = (StorageDeviceInfo)deviceInfos.get(hostRef);
               Map disksMap = DiskData.mapDiskPaths(deviceInfo);
               availableDisks.add(DiskData.fromScsiDisk(d.disk, (String)null, false, (String)null, (Target)DiskData.mapAvailableTargets(deviceInfo).get(((Path)disksMap.get(d.disk.uuid)).target), (Adapter)DiskData.mapAvailableAdapters(deviceInfo).get(((Path)disksMap.get(d.disk.uuid)).adapter), d.state));
            }
         }

         ArrayList var26 = availableDisks;
         return var26;
      } catch (Throwable var24) {
         var5 = var24;
         throw var24;
      } finally {
         if (measure != null) {
            if (var5 != null) {
               try {
                  measure.close();
               } catch (Throwable var23) {
                  var5.addSuppressed(var23);
               }
            } else {
               measure.close();
            }
         }

      }
   }
}
