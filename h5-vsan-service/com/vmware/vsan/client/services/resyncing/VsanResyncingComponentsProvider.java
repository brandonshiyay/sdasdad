package com.vmware.vsan.client.services.resyncing;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.vsan.host.ClusterStatus;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentityAndHealth;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectInformation;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanSystemEx;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.RepairTimerInfo;
import com.vmware.vim.vsan.binding.vim.vsan.RuntimeStatsHostMap;
import com.vmware.vim.vsan.binding.vim.vsan.host.RuntimeStats;
import com.vmware.vim.vsan.binding.vim.vsan.host.StatsType;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.fileservice.VsanFileServiceConfigService;
import com.vmware.vsan.client.services.fileservice.model.FileServiceFeature;
import com.vmware.vsan.client.services.fileservice.model.FileSharesPaginationSpec;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceShare;
import com.vmware.vsan.client.services.resyncing.data.DelayTimerData;
import com.vmware.vsan.client.services.resyncing.data.HostResyncTrafficData;
import com.vmware.vsan.client.services.resyncing.data.RepairTimerData;
import com.vmware.vsan.client.services.resyncing.data.ResyncComponent;
import com.vmware.vsan.client.services.resyncing.data.ResyncMonitorData;
import com.vmware.vsan.client.services.resyncing.data.VsanSyncingObjectsQuerySpec;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectsFilter;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.ProfileUtils;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VsanResyncingComponentsProvider {
   private static final String RUNTIME_STAT_REPAIR_TIMER = "repairTimerInfo";
   private static final String[] RUNTIME_STATS = new String[]{"repairTimerInfo"};
   private static final int TIMER_DEFAULT_VALUE = 0;
   @Autowired
   private VsanResyncingIscsiTargetComponentsProvider iscsiTargetComponentsProvider;
   @Autowired
   private VsanResyncingComponentsRetriever vsanSyncComponentsRetriever;
   @Autowired
   private VsanFileServiceConfigService fileServiceConfigService;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanClient vsanClient;
   private static final Log _logger = LogFactory.getLog(VsanResyncingComponentsProvider.class);
   private static final String RESYNC_THROTTLING_PROPERTY = "vsanResyncThrottling";

   @TsService
   public HostResyncTrafficData[] getHostsResyncTraffic(ManagedObjectReference clusterRef) throws Exception {
      Map hostsToResyncTrafficMap = this.getHostsToResyncTrafficMap(clusterRef);
      if (hostsToResyncTrafficMap != null && hostsToResyncTrafficMap.size() != 0) {
         DataServiceResponse response = QueryUtil.getProperties((ManagedObjectReference[])hostsToResyncTrafficMap.keySet().toArray(new ManagedObjectReference[0]), new String[]{"name", "primaryIconId"});
         if (response == null) {
            return new HostResyncTrafficData[0];
         } else {
            Object resourceObject;
            HostResyncTrafficData data;
            for(Iterator var4 = response.getResourceObjects().iterator(); var4.hasNext(); data.primaryIconId = (String)response.getProperty(resourceObject, "primaryIconId")) {
               resourceObject = var4.next();
               data = (HostResyncTrafficData)hostsToResyncTrafficMap.get(resourceObject);
               data.name = (String)response.getProperty(resourceObject, "name");
            }

            return (HostResyncTrafficData[])hostsToResyncTrafficMap.values().toArray(new HostResyncTrafficData[hostsToResyncTrafficMap.size()]);
         }
      } else {
         return new HostResyncTrafficData[0];
      }
   }

   @TsService
   public boolean getIsResyncThrottlingSupported(ManagedObjectReference clusterRef) {
      boolean resyncThrottlingSupported = VsanCapabilityUtils.isResyncThrottlingSupported(clusterRef);
      return resyncThrottlingSupported;
   }

   private Map getHostsToResyncTrafficMap(ManagedObjectReference clusterRef) throws Exception {
      RuntimeStatsHostMap[] runtimeStats = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
         Measure measure = new Measure("vsanConfigSystem.getRuntimeStats");
         Throwable var7 = null;

         try {
            runtimeStats = vsanConfigSystem.getRuntimeStats(clusterRef, new String[]{StatsType.resyncIopsInfo.toString()});
         } catch (Throwable var30) {
            var7 = var30;
            throw var30;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var29) {
                     var7.addSuppressed(var29);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Throwable var32) {
         var4 = var32;
         throw var32;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var28) {
                  var4.addSuppressed(var28);
               }
            } else {
               conn.close();
            }
         }

      }

      Map hostToStatsMap = new HashMap();
      if (runtimeStats != null) {
         hostToStatsMap = new HashMap();
         RuntimeStatsHostMap[] var35 = runtimeStats;
         int var36 = runtimeStats.length;

         for(int var37 = 0; var37 < var36; ++var37) {
            RuntimeStatsHostMap stats = var35[var37];
            HostResyncTrafficData data = new HostResyncTrafficData();
            if (stats.stats != null && stats.stats.resyncIopsInfo != null) {
               data.resyncTraffic = stats.stats.resyncIopsInfo.resyncIops;
            } else {
               _logger.warn("Empty stats returned for host: " + stats);
            }

            ManagedObjectReference host = stats.host;
            VmodlHelper.assignServerGuid(host, clusterRef.getServerGuid());
            hostToStatsMap.put(stats.host, data);
         }
      }

      return hostToStatsMap;
   }

   @TsService
   public ResyncMonitorData getResyncingData(ManagedObjectReference clusterRef, int limit) {
      return this.getVsanDatastoreResyncingData(clusterRef, limit, (String[])null, (String)null);
   }

   @TsService
   public ResyncMonitorData getResyncingDataForAutoRefresh(ManagedObjectReference clusterRef) {
      return this.getVsanDatastoreResyncingData(clusterRef, 0, new String[]{null}, (String)null);
   }

   @TsService
   public ResyncMonitorData getVsanDatastoreResyncingData(ManagedObjectReference clusterRef, int limit, String[] resyncTypes, String resyncStatus) {
      if (clusterRef != null && this.isVsanEnabledOnCluster(clusterRef)) {
         VsanAsyncDataRetriever dataRetriever = null;
         Measure measure = new Measure("Collect Resyncing objects information");
         Throwable var7 = null;

         try {
            dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef);
            dataRetriever.loadTargetVrConfigIdentities();
         } catch (Throwable var39) {
            var7 = var39;
            throw var39;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var34) {
                     var7.addSuppressed(var34);
                  }
               } else {
                  measure.close();
               }
            }

         }

         Future[] repairTimerDataFutures = null;
         Future configInfoExFuture = null;

         try {
            Measure measure = new Measure("Retrieving delay/repair timer data");
            Throwable var9 = null;

            try {
               repairTimerDataFutures = this.getRepairTimerDataFutures(clusterRef, measure);
               configInfoExFuture = this.getConfigInfoExFuture(clusterRef, measure);
            } catch (Throwable var38) {
               var9 = var38;
               throw var38;
            } finally {
               if (measure != null) {
                  if (var9 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var37) {
                        var9.addSuppressed(var37);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var42) {
            _logger.error("Error while try to retrieve repair or delay time", var42);
         }

         ResyncMonitorData resyncMonitorData = this.initResyncMonitorData(clusterRef, limit, resyncTypes, resyncStatus);
         if (resyncMonitorData.components != null) {
            _logger.info("ResyncMonitorData.getVsanObjectUuids: " + StringUtils.join(resyncMonitorData.getVsanObjectUuids(), ","));
            this.initializeIdentitiesDataRetrievers(dataRetriever, clusterRef, resyncMonitorData.getVsanObjectUuids());
         } else {
            _logger.debug("No resyncing components found.");
         }

         try {
            resyncMonitorData.isVsanClusterPartitioned = this.getIsVsanClusterPartitioned(clusterRef);
         } catch (Exception var36) {
            _logger.error("Error while try go get if cluster is partitioned", var36);
         }

         resyncMonitorData.isResyncThrottlingSupported = this.getIsResyncThrottlingSupported(clusterRef);
         if (resyncMonitorData.isResyncThrottlingSupported) {
            try {
               resyncMonitorData.resyncThrottlingValue = (Integer)QueryUtil.getProperty(clusterRef, "vsanResyncThrottling", (Object)null);
            } catch (Exception var35) {
               _logger.error("Error while try go get cluster's resync throttling property", var35);
            }
         }

         resyncMonitorData.repairTimerData = this.getRepairTimerData(repairTimerDataFutures);
         resyncMonitorData.delayTimerData = this.getDelayTimerData(configInfoExFuture);
         this.buildResyncingObjects(clusterRef, resyncMonitorData, dataRetriever);
         return resyncMonitorData;
      } else {
         return new ResyncMonitorData();
      }
   }

   private ResyncMonitorData initResyncMonitorData(ManagedObjectReference clusterRef, int limit, String[] resyncTypes, String resyncStatus) {
      _logger.debug("Getting resyncing components on the vsan datastore.");
      VsanSyncingObjectsQuerySpec spec = new VsanSyncingObjectsQuerySpec();
      spec.resyncTypes = resyncTypes;
      spec.status = resyncStatus;
      if (limit >= 0) {
         spec.limit = limit;
      }

      try {
         return this.vsanSyncComponentsRetriever.getVsanResyncObjects(clusterRef, spec);
      } catch (Exception var7) {
         throw new VsanUiLocalizableException("vsan.resyncing.resync.objects.retrieve.error", var7);
      }
   }

   private boolean getIsVsanClusterPartitioned(ManagedObjectReference clusterRef) {
      if (clusterRef != null && this.isVsanEnabledOnCluster(clusterRef)) {
         Collection hostsInCluster = this.getVsanHostResourcesInCluster(clusterRef);
         if (hostsInCluster != null && !hostsInCluster.isEmpty()) {
            int numberHostsInUse = 0;
            int numberHostsInClusterNode = 0;
            Iterator var5 = hostsInCluster.iterator();

            while(var5.hasNext()) {
               VsanResyncingComponentsProvider.VsanHostResourceData hostData = (VsanResyncingComponentsProvider.VsanHostResourceData)var5.next();
               if (hostData != null && hostData.isVsanEnabled != null && hostData.isVsanEnabled) {
                  if (numberHostsInUse == 0 && hostData.vsanHostClusterStatus != null && hostData.vsanHostClusterStatus.memberUuid != null) {
                     numberHostsInClusterNode = hostData.vsanHostClusterStatus.memberUuid.length;
                  }

                  ++numberHostsInUse;
               }
            }

            numberHostsInUse += this.getNumberOfWitnessHosts(clusterRef);
            return numberHostsInClusterNode != numberHostsInUse;
         } else {
            return false;
         }
      } else {
         _logger.warn("Null cluster reference or vsan not enabled on cluster, returning false.");
         return false;
      }
   }

   private Collection getVsanHostResourcesInCluster(ManagedObjectReference clusterRef) {
      if (clusterRef == null) {
         _logger.error("Null cluster reference encountered.");
         return null;
      } else {
         PropertyValue[] hostProperties = null;

         try {
            hostProperties = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), new String[]{"vsanHostClusterStatus", "config.vsanHostConfig.enabled"}).getPropertyValues();
         } catch (Exception var10) {
            _logger.error("Failed to get hosts in cluster!", var10);
         }

         if (hostProperties == null) {
            return null;
         } else {
            Map hosts = new HashMap();
            PropertyValue[] var4 = hostProperties;
            int var5 = hostProperties.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               PropertyValue propValue = var4[var6];
               ManagedObjectReference hostRef = (ManagedObjectReference)propValue.resourceObject;
               VsanResyncingComponentsProvider.VsanHostResourceData hostData = (VsanResyncingComponentsProvider.VsanHostResourceData)hosts.get(hostRef);
               if (hostData == null) {
                  hostData = new VsanResyncingComponentsProvider.VsanHostResourceData();
                  hosts.put(hostRef, hostData);
               }

               if ("vsanHostClusterStatus".equals(propValue.propertyName)) {
                  hostData.vsanHostClusterStatus = (ClusterStatus)propValue.value;
               } else if ("config.vsanHostConfig.enabled".equals(propValue.propertyName)) {
                  hostData.isVsanEnabled = BooleanUtils.isTrue((Boolean)propValue.value);
               }
            }

            return hosts.values();
         }
      }
   }

   private int getNumberOfWitnessHosts(ManagedObjectReference clusterRef) {
      int witnessHosts = 0;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      int var37;
      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();

         try {
            Measure measure = new Measure("stretchedClusterSystem.getNumOfWitnessHosts");
            Throwable var7 = null;

            try {
               witnessHosts = stretchedClusterSystem.getNumOfWitnessHosts(clusterRef);
            } catch (Throwable var32) {
               var7 = var32;
               throw var32;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var31) {
                        var7.addSuppressed(var31);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var34) {
            _logger.error("Could not retrieve witness hosts for cluster " + var34.getMessage());
         }

         var37 = witnessHosts;
      } catch (Throwable var35) {
         var4 = var35;
         throw var35;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var30) {
                  var4.addSuppressed(var30);
               }
            } else {
               conn.close();
            }
         }

      }

      return var37;
   }

   private void buildResyncingObjects(ManagedObjectReference clusterRef, ResyncMonitorData resyncMonitorData, VsanAsyncDataRetriever dataRetriever) {
      if (resyncMonitorData.components != null) {
         Map virtualObjectsFilterToObjectIdentities;
         Map vsanUuidToPolicyName;
         try {
            VsanObjectIdentityAndHealth vsanObjectIdentityAndHealth = dataRetriever.getObjectIdentities();
            virtualObjectsFilterToObjectIdentities = VsanResyncingComponentsUtil.getVirtualObjectsFilterToObjectIdentities(vsanObjectIdentityAndHealth);
            VsanObjectInformation[] vsanObjectInformations = this.getVsanObjectInformationsFromDataRetriever(clusterRef, dataRetriever);
            Map storagePolicies = ProfileUtils.getPoliciesIdNamePairs(dataRetriever.getStoragePolicies());
            vsanUuidToPolicyName = VsanResyncingComponentsUtil.getVsanUuidToStoragePolicyName(vsanObjectIdentityAndHealth, vsanObjectInformations, storagePolicies);
         } catch (Exception var9) {
            _logger.error("Failed to Collect Resyncing objects information. Returning partial results. ", var9);
            return;
         }

         this.buildVms(clusterRef, resyncMonitorData, virtualObjectsFilterToObjectIdentities, vsanUuidToPolicyName);
         this.buildIscsiObjects(clusterRef, resyncMonitorData, virtualObjectsFilterToObjectIdentities, vsanUuidToPolicyName);
         this.buildFileShares(clusterRef, resyncMonitorData, virtualObjectsFilterToObjectIdentities, vsanUuidToPolicyName);
         this.buildVrTargets(clusterRef, resyncMonitorData, virtualObjectsFilterToObjectIdentities, vsanUuidToPolicyName, dataRetriever);
         List orphanedSyncObjects = this.getOrphanedSyncObjects(resyncMonitorData, virtualObjectsFilterToObjectIdentities);
         resyncMonitorData.processOtherObjects((List)virtualObjectsFilterToObjectIdentities.get(VirtualObjectsFilter.OTHERS), orphanedSyncObjects, vsanUuidToPolicyName);
      }
   }

   private void buildVms(ManagedObjectReference clusterRef, ResyncMonitorData resyncMonitorData, Map vsanObjectIdentitiesData, Map uuidToPolicyName) {
      if (!CollectionUtils.isEmpty((Collection)vsanObjectIdentitiesData.get(VirtualObjectsFilter.VMS))) {
         Map vmDataMap = VsanResyncingComponentsUtil.getVmData(clusterRef, (List)vsanObjectIdentitiesData.get(VirtualObjectsFilter.VMS));
         resyncMonitorData.processVmObjects((List)vsanObjectIdentitiesData.get(VirtualObjectsFilter.VMS), vmDataMap, uuidToPolicyName);
      }

   }

   private void buildIscsiObjects(ManagedObjectReference clusterRef, ResyncMonitorData resyncMonitorData, Map vsanObjectIdentitiesData, Map uuidToPolicyName) {
      List iscsiIdentityData = (List)vsanObjectIdentitiesData.get(VirtualObjectsFilter.ISCSI_TARGETS);
      if (!CollectionUtils.isEmpty(iscsiIdentityData)) {
         Map iscsiObjects = this.iscsiTargetComponentsProvider.getIscsiResyncObjects(clusterRef, resyncMonitorData.getVsanObjectUuids());
         uuidToPolicyName.putAll(this.getIscsiExtraObjectsPolicy(clusterRef, iscsiIdentityData, iscsiObjects));
         resyncMonitorData.processIscsiObjects(iscsiIdentityData, uuidToPolicyName, iscsiObjects);
      }

   }

   private void buildFileShares(ManagedObjectReference clusterRef, ResyncMonitorData resyncMonitorData, Map vsanObjectIdentitiesData, Map uuidToPolicyName) {
      List sharesIdentityData = (List)vsanObjectIdentitiesData.get(VirtualObjectsFilter.FILE_SHARES);
      if (!CollectionUtils.isEmpty(sharesIdentityData)) {
         Map featureMap = this.fileServiceConfigService.isFeatureSupportedOnRuntime(clusterRef, new FileServiceFeature[]{FileServiceFeature.PAGINATION});
         List allShares;
         if (BooleanUtils.isTrue((Boolean)featureMap.get(FileServiceFeature.PAGINATION))) {
            allShares = this.fileServiceConfigService.listAllShares(clusterRef);
         } else {
            FileSharesPaginationSpec spec = new FileSharesPaginationSpec();
            spec.pageSize = 32;
            allShares = this.fileServiceConfigService.listSharesPerDomain(clusterRef, spec, false).shares;
         }

         List resyncingShares = new ArrayList();
         Set resyncingObjectsUuids = resyncMonitorData.getVsanObjectUuids();
         Iterator var10 = allShares.iterator();

         while(var10.hasNext()) {
            VsanFileServiceShare share = (VsanFileServiceShare)var10.next();
            this.filterOutObjectUuidsFromFileShare(share, resyncingObjectsUuids);
            if (CollectionUtils.isNotEmpty(share.objectUuids)) {
               resyncingShares.add(share);
            }
         }

         resyncMonitorData.processFileShares(sharesIdentityData, uuidToPolicyName, resyncingShares);
      }

   }

   private void buildVrTargets(ManagedObjectReference clusterRef, ResyncMonitorData resyncMonitorData, Map vsanObjectIdentitiesData, Map uuidToPolicyName, VsanAsyncDataRetriever dataRetriever) {
      if (!CollectionUtils.isEmpty((Collection)vsanObjectIdentitiesData.get(VirtualObjectsFilter.VR_TARGETS))) {
         HashMap vrWrappersMap = new HashMap();
         ResyncComponent defaultVrWrapper = new ResyncComponent();
         defaultVrWrapper.name = Utils.getLocalizedString("vsan.resyncing.components.hbr.wrapper.generic");
         defaultVrWrapper.iconId = "vr-replication-wrapper";
         vrWrappersMap.put((Object)null, defaultVrWrapper);
         VsanObjectIdentity[] allHbrCfgs = null;

         try {
            allHbrCfgs = dataRetriever.getTargetVrConfigIdentities().identities;
         } catch (Exception var14) {
            _logger.warn("Unable to collect VR Target wrappers. All related objects will be under single category", var14);
         }

         if (allHbrCfgs != null) {
            VsanObjectIdentity[] var9 = allHbrCfgs;
            int var10 = allHbrCfgs.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               VsanObjectIdentity hbrCfg = var9[var11];
               ResyncComponent vrWrapper = new ResyncComponent();
               vrWrapper.name = Utils.getLocalizedString("vsan.resyncing.components.hbr.wrapper", hbrCfg.description);
               vrWrapper.iconId = "vr-replication-wrapper";
               vrWrappersMap.put(hbrCfg.uuid, vrWrapper);
            }
         }

         resyncMonitorData.processVrObjects((List)vsanObjectIdentitiesData.get(VirtualObjectsFilter.VR_TARGETS), vrWrappersMap, uuidToPolicyName);
      }
   }

   private void filterOutObjectUuidsFromFileShare(VsanFileServiceShare share, Set uuids) {
      List filteredUuids = new ArrayList();
      Iterator var4 = share.objectUuids.iterator();

      while(var4.hasNext()) {
         String uuid = (String)var4.next();
         if (uuids.contains(uuid)) {
            filteredUuids.add(uuid);
         }
      }

      share.objectUuids = filteredUuids;
   }

   private DelayTimerData getDelayTimerData(Future configInfoExFuture) {
      DelayTimerData delayTimerData = new DelayTimerData();
      if (configInfoExFuture == null) {
         delayTimerData.isSupported = false;
      } else {
         delayTimerData.isSupported = true;

         try {
            ConfigInfoEx configInfoEx = (ConfigInfoEx)configInfoExFuture.get();
            if (configInfoEx != null && configInfoEx.getExtendedConfig() != null) {
               delayTimerData.delayTimer = configInfoEx.getExtendedConfig().objectRepairTimer;
            } else {
               delayTimerData.errorMessage = Utils.getLocalizedString("vsan.resyncing.delayTimer.error");
               _logger.error("Cannot retrieve the Delay Timer value because the configuration is null!");
            }
         } catch (Exception var4) {
            delayTimerData.errorMessage = Utils.getLocalizedString("vsan.resyncing.delayTimer.error");
            _logger.error("Cannot retrieve Delay Timer information: ", var4);
         }
      }

      return delayTimerData;
   }

   public RepairTimerData getRepairTimerData(Future[] repairTimerDataFutures) {
      RepairTimerData repairTimerData = new RepairTimerData();
      if (repairTimerDataFutures == null) {
         repairTimerData.isSupported = false;
      } else if (ArrayUtils.isEmpty(repairTimerDataFutures)) {
         repairTimerData.isSupported = true;
      } else {
         repairTimerData.isSupported = true;
         long maxTimer = Long.MIN_VALUE;
         long minTimer = Long.MAX_VALUE;
         long objectsCount = 0L;
         long objectsCountWithRepairTimer = 0L;
         long todayInMilliseconds = (new Date()).getTime();
         Future[] var13 = repairTimerDataFutures;
         int var14 = repairTimerDataFutures.length;

         for(int var15 = 0; var15 < var14; ++var15) {
            Future repairTimerDataFuture = var13[var15];

            try {
               RuntimeStats runtimeStats = (RuntimeStats)repairTimerDataFuture.get();
               RepairTimerInfo repairTimerInfo = runtimeStats.repairTimerInfo;
               if (repairTimerInfo == null) {
                  _logger.warn("No runtime stats received for host!");
               } else if (repairTimerInfo.objectCount <= 0) {
                  _logger.debug("No objects scheduled for resyncing on the host");
               } else {
                  if (repairTimerInfo.maxTimeToRepair >= 0) {
                     maxTimer = Math.max(maxTimer, todayInMilliseconds + (long)(repairTimerInfo.maxTimeToRepair * 1000));
                  }

                  if (repairTimerInfo.minTimeToRepair >= 0) {
                     minTimer = Math.min(minTimer, todayInMilliseconds + (long)(repairTimerInfo.minTimeToRepair * 1000));
                  }

                  objectsCountWithRepairTimer += (long)repairTimerInfo.objectCountWithRepairTimer;
                  objectsCount += (long)repairTimerInfo.objectCount;
               }
            } catch (Exception var19) {
               _logger.error("Cannot retrieve Repair Timer Data: ", var19);
            }
         }

         repairTimerData.maxTimer = maxTimer;
         repairTimerData.minTimer = minTimer;
         repairTimerData.objectsCount = objectsCount;
         repairTimerData.objectsCountWithRepairTimer = objectsCountWithRepairTimer;
         repairTimerData.objectsCountPending = objectsCount - objectsCountWithRepairTimer;
      }

      return repairTimerData;
   }

   public Future[] getRepairTimerDataFutures(ManagedObjectReference clusterRef, Measure measure) throws Exception {
      ManagedObjectReference[] hosts = null;

      try {
         hosts = (ManagedObjectReference[])QueryUtil.getProperty(clusterRef, "host");
      } catch (Exception var22) {
         _logger.warn("Cannot retrieve hosts for cluster: " + clusterRef, var22);
      }

      if (ArrayUtils.isEmpty(hosts)) {
         return new Future[0];
      } else {
         List futures = new ArrayList(hosts.length);
         ManagedObjectReference[] var5 = hosts;
         int var6 = hosts.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            ManagedObjectReference hostRef = var5[var7];
            if (VsanCapabilityUtils.isRepairTimerInResyncStatsSupported(hostRef)) {
               VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
               Throwable var10 = null;

               try {
                  VsanSystemEx vsanSystemEx = conn.getVsanSystemEx(hostRef);
                  Future future = measure.newFuture("vsanSystemEx.getRuntimeStats");
                  vsanSystemEx.getRuntimeStats(RUNTIME_STATS, (String)null, future);
                  futures.add(future);
               } catch (Throwable var21) {
                  var10 = var21;
                  throw var21;
               } finally {
                  if (conn != null) {
                     if (var10 != null) {
                        try {
                           conn.close();
                        } catch (Throwable var20) {
                           var10.addSuppressed(var20);
                        }
                     } else {
                        conn.close();
                     }
                  }

               }
            }
         }

         if (futures.isEmpty()) {
            return null;
         } else {
            return (Future[])futures.toArray(new Future[0]);
         }
      }
   }

   private Future getConfigInfoExFuture(ManagedObjectReference clusterRef, Measure measure) throws Exception {
      if (VsanCapabilityUtils.isClusterConfigSystemSupportedOnVc(clusterRef)) {
         Future result = measure.newFuture("VsanVcClusterConfigSystem.getConfigInfoEx");
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         Future var7;
         try {
            VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
            vsanConfigSystem.getConfigInfoEx(clusterRef, result);
            var7 = result;
         } catch (Throwable var16) {
            var5 = var16;
            throw var16;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var15) {
                     var5.addSuppressed(var15);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var7;
      } else {
         return null;
      }
   }

   private Map getIscsiExtraObjectsPolicy(ManagedObjectReference clusterRef, List iscsiIdentityData, Map iscsiObjects) {
      Map extraHealthData = new HashMap();
      if (iscsiObjects != null) {
         Set iscsiExtraIdentities = new HashSet();
         Iterator var6 = iscsiObjects.keySet().iterator();

         while(var6.hasNext()) {
            String iscsiObjectUuid = (String)var6.next();
            boolean matchFound = false;
            Iterator var9 = iscsiIdentityData.iterator();

            while(var9.hasNext()) {
               VsanObjectIdentity iscsiIdentity = (VsanObjectIdentity)var9.next();
               if (iscsiObjectUuid.equals(iscsiIdentity.uuid)) {
                  matchFound = true;
                  break;
               }
            }

            if (!matchFound) {
               iscsiExtraIdentities.add(iscsiObjectUuid);
            }
         }

         if (iscsiExtraIdentities.size() > 0) {
            try {
               Measure measure = new Measure("Collect ISCSI Resyncing objects information");
               Throwable var24 = null;

               try {
                  VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef);
                  this.initializeIdentitiesDataRetrievers(dataRetriever, clusterRef, iscsiExtraIdentities);
                  VsanObjectIdentityAndHealth vsanObjectIdentityAndHealth = dataRetriever.getObjectIdentities();
                  VsanObjectInformation[] vsanObjectInformations = this.getVsanObjectInformationsFromDataRetriever(clusterRef, dataRetriever);
                  Map storagePolicies = ProfileUtils.getPoliciesIdNamePairs(dataRetriever.getStoragePolicies());
                  extraHealthData = VsanResyncingComponentsUtil.getVsanUuidToStoragePolicyName(vsanObjectIdentityAndHealth, vsanObjectInformations, storagePolicies);
               } catch (Throwable var20) {
                  var24 = var20;
                  throw var20;
               } finally {
                  if (measure != null) {
                     if (var24 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var19) {
                           var24.addSuppressed(var19);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }
            } catch (Exception var22) {
               _logger.error("Failed to Collect ISCSI Resyncing objects information. Returning partial results. ", var22);
            }
         }
      }

      return (Map)extraHealthData;
   }

   private VsanAsyncDataRetriever initializeIdentitiesDataRetrievers(VsanAsyncDataRetriever dataRetriever, ManagedObjectReference clusterRef, Set uuIds) {
      dataRetriever.loadObjectIdentities(uuIds).loadStoragePolicies();
      if (!VsanCapabilityUtils.isObjectsHealthV2SupportedOnVc(clusterRef)) {
         dataRetriever.loadObjectInformation(uuIds);
      }

      return dataRetriever;
   }

   private VsanObjectInformation[] getVsanObjectInformationsFromDataRetriever(ManagedObjectReference clusterRef, VsanAsyncDataRetriever dataRetriever) throws ExecutionException, InterruptedException {
      VsanObjectInformation[] vsanObjectInformations;
      if (!VsanCapabilityUtils.isObjectsHealthV2SupportedOnVc(clusterRef)) {
         vsanObjectInformations = dataRetriever.getObjectInformation();
      } else {
         vsanObjectInformations = new VsanObjectInformation[0];
      }

      return vsanObjectInformations;
   }

   private List getOrphanedSyncObjects(ResyncMonitorData resyncMonitorData, Map vsanObjectIdentitiesData) {
      List orphanedObjects = new ArrayList();
      Iterator var4 = resyncMonitorData.getVsanObjectUuids().iterator();

      while(var4.hasNext()) {
         String syncObjectUuid = (String)var4.next();
         boolean identityFound = false;
         VirtualObjectsFilter[] var7 = VirtualObjectsFilter.values();
         int var8 = var7.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            VirtualObjectsFilter filter = var7[var9];
            if (filter != null && !CollectionUtils.isEmpty((Collection)vsanObjectIdentitiesData.get(filter))) {
               Iterator var11 = ((List)vsanObjectIdentitiesData.get(filter)).iterator();

               while(var11.hasNext()) {
                  VsanObjectIdentity identity = (VsanObjectIdentity)var11.next();
                  if (syncObjectUuid.equals(identity.uuid)) {
                     identityFound = true;
                     break;
                  }
               }

               if (identityFound) {
                  break;
               }
            }
         }

         if (!identityFound) {
            orphanedObjects.add(syncObjectUuid);
         }
      }

      return orphanedObjects;
   }

   private Boolean isVsanEnabledOnCluster(ManagedObjectReference clusterRef) {
      try {
         return (Boolean)QueryUtil.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled", (Object)null);
      } catch (Exception var3) {
         throw new VsanUiLocalizableException("vsan.common.cluster.configuration.error", var3);
      }
   }

   private static class VsanHostResourceData {
      public ClusterStatus vsanHostClusterStatus;
      public Boolean isVsanEnabled;

      private VsanHostResourceData() {
      }

      // $FF: synthetic method
      VsanHostResourceData(Object x0) {
         this();
      }
   }
}
