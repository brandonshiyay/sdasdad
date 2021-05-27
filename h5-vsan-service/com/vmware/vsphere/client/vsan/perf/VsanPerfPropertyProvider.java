package com.vmware.vsphere.client.vsan.perf;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.pbm.profile.ProfileId;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.KeyValue;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.dvs.HostMember.PnicBacking;
import com.vmware.vim.binding.vim.dvs.HostMember.PnicSpec;
import com.vmware.vim.binding.vim.fault.Timedout;
import com.vmware.vim.binding.vim.fault.VimFault;
import com.vmware.vim.binding.vim.host.HostProxySwitch;
import com.vmware.vim.binding.vim.host.OpaqueNetworkInfo;
import com.vmware.vim.binding.vim.host.OpaqueSwitch;
import com.vmware.vim.binding.vim.host.PhysicalNic;
import com.vmware.vim.binding.vim.host.PortGroup;
import com.vmware.vim.binding.vim.host.VirtualNic;
import com.vmware.vim.binding.vim.host.HostProxySwitch.HostLagConfig;
import com.vmware.vim.binding.vim.host.NetworkPolicy.NicOrderPolicy;
import com.vmware.vim.binding.vim.host.PortGroup.Port;
import com.vmware.vim.binding.vim.vm.DefinedProfileSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualController;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice.FileBackingInfo;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vim.vsan.host.ConfigInfo.NetworkInfo.PortConfig;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.RuntimeFault;
import com.vmware.vim.binding.vmodl.fault.InvalidArgument;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectInformation;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityType;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfMetricSeriesCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfNodeInformation;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfTimeRange;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfTimeRangeQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfTopQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerformanceManager;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfsvcConfig;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.PropertyConstraint;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.services.common.data.StorageCompliance;
import com.vmware.vsan.client.services.config.SpaceEfficiencyConfig;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.services.networkdiagnostics.NetworkDiagnosticsService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.NoOpMeasure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectHealthState;
import com.vmware.vsphere.client.vsan.base.impl.PbmDataProvider;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.iscsi.providers.VsanIscsiPropertyProvider;
import com.vmware.vsphere.client.vsan.perf.model.ActiveVmnicDataSpec;
import com.vmware.vsphere.client.vsan.perf.model.CapacityHistoryBasicInfo;
import com.vmware.vsphere.client.vsan.perf.model.DiskGroup;
import com.vmware.vsphere.client.vsan.perf.model.EntityPerfStateObject;
import com.vmware.vsphere.client.vsan.perf.model.HostDiskGroupsData;
import com.vmware.vsphere.client.vsan.perf.model.HostPnicsData;
import com.vmware.vsphere.client.vsan.perf.model.HostVnicsData;
import com.vmware.vsphere.client.vsan.perf.model.PerfEditData;
import com.vmware.vsphere.client.vsan.perf.model.PerfEntityStateData;
import com.vmware.vsphere.client.vsan.perf.model.PerfGraphMetricsData;
import com.vmware.vsphere.client.vsan.perf.model.PerfMetricsInfo;
import com.vmware.vsphere.client.vsan.perf.model.PerfMonitorCommonPropsData;
import com.vmware.vsphere.client.vsan.perf.model.PerfPhysicalAdapterEntity;
import com.vmware.vsphere.client.vsan.perf.model.PerfQuerySpec;
import com.vmware.vsphere.client.vsan.perf.model.PerfStatsObjectInfo;
import com.vmware.vsphere.client.vsan.perf.model.PerfTimeRangeData;
import com.vmware.vsphere.client.vsan.perf.model.PerfTopContributorsEntity;
import com.vmware.vsphere.client.vsan.perf.model.PerfTopContributorsEntityType;
import com.vmware.vsphere.client.vsan.perf.model.PerfTopContributorsQuerySpec;
import com.vmware.vsphere.client.vsan.perf.model.PerfVirtualDiskEntity;
import com.vmware.vsphere.client.vsan.perf.model.PerfVirtualMachineDiskData;
import com.vmware.vsphere.client.vsan.perf.model.PerfVscsiEntity;
import com.vmware.vsphere.client.vsan.perf.model.ServerObjectInfo;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

public class VsanPerfPropertyProvider {
   private static final Log logger = LogFactory.getLog(VsanPerfMutationProvider.class);
   private static final VsanProfiler profiler = new VsanProfiler(VsanPerfPropertyProvider.class);
   private static final String DATASTORE_URL_PREFIX = "ds://";
   public static final String[] HOST_NETWORK_PROPS = new String[]{"name", "config.vsanHostConfig.clusterInfo.nodeUuid", "network", "config.vsanHostConfig.networkInfo.port", "config.network.vnic", "config.network.opaqueNetwork", "config.network.opaqueSwitch", "config.network.pnic", "config.network.portgroup", "config.network.proxySwitch"};
   private static final long MILISECONDS_IN_HOUR = 3600000L;
   private static final String CAPACITY_HISTORY_SPACE_EFFICIENCY_SAVED_BY_KEY = "savedByDedup";
   private static final String CAPACITY_HISTORY_SPACE_EFFICIENCY_RATIO_KEY = "dedupRatio";
   private static final String CAPACITY_ENTITY_TYPE = "vsan-cluster-capacity";
   private static final String ENTITY_REF_ID_KEY = "entityRefId";
   private static final String IOINSIGHT_ENTITY_TYPE = "ioinsight";
   private static final String IOINSIGHT_HISTOGRAM_ENTITY_TYPE = "ioinsight-histogram";
   @Autowired
   private PermissionService permissionService;
   @Autowired
   private VsanIscsiPropertyProvider iscsiPropertyProvider;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private VsanInventoryHelper vsanInventoryHelper;
   @Autowired
   private CsdService csdService;
   @Autowired
   private PbmDataProvider pbmDataProvider;
   @Autowired
   private VsanConfigService vsanConfigService;
   @Autowired
   private NetworkDiagnosticsService networkDiagnosticsService;
   @Autowired
   private QueryExecutor queryExecutor;

   @TsService
   public List getEntitiesInfo(ManagedObjectReference clusterRef) throws Exception {
      List entities = new ArrayList();
      DataServiceResponse result = QueryUtil.getProperties(clusterRef, new String[]{"name", "primaryIconId", "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.defaultConfig.uuid"});
      ServerObjectInfo clusterInfo = new ServerObjectInfo();
      clusterInfo.isCluster = true;
      clusterInfo.name = (String)result.getProperty(clusterRef, "name");
      clusterInfo.vsanUuid = (String)result.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.defaultConfig.uuid");
      clusterInfo.icon = (String)result.getProperty(clusterRef, "primaryIconId");
      entities.add(clusterInfo);
      DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), new String[]{"name", "primaryIconId", "config.vsanHostConfig.clusterInfo.nodeUuid"});
      if (response == null) {
         return entities;
      } else {
         Iterator var6 = response.getResourceObjects().iterator();

         while(var6.hasNext()) {
            Object resourceObject = var6.next();
            ManagedObjectReference hostRef = (ManagedObjectReference)resourceObject;
            ServerObjectInfo hostInfo = new ServerObjectInfo();
            hostInfo.isCluster = false;
            hostInfo.name = (String)response.getProperty(hostRef, "name");
            hostInfo.vsanUuid = (String)response.getProperty(hostRef, "config.vsanHostConfig.clusterInfo.nodeUuid");
            hostInfo.icon = (String)response.getProperty(hostRef, "primaryIconId");
            entities.add(hostInfo);
         }

         return entities;
      }
   }

   @TsService
   public List getEntityPerfStateForWildcards(ManagedObjectReference clusterRef, PerfQuerySpec[] specs) throws Exception {
      Map entitiesDataMap = new HashMap();
      PerfQuerySpec[] var4 = specs;
      int var5 = specs.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         PerfQuerySpec spec = var4[var6];
         List stateDataList = new ArrayList();
         long startTime = spec.startTime;

         for(long endTime = startTime + 3600000L; endTime <= spec.endTime; endTime += 3600000L) {
            PerfQuerySpec tempSpec = new PerfQuerySpec();
            tempSpec.startTime = startTime;
            tempSpec.endTime = endTime;
            tempSpec.entityType = spec.entityType;
            tempSpec.entityUuid = spec.entityUuid;
            stateDataList.addAll(this.getEntityPerfState(clusterRef, new PerfQuerySpec[]{tempSpec}));
            startTime = endTime;
         }

         if (!CollectionUtils.isEmpty(stateDataList)) {
            entitiesDataMap = this.aggregateStateDataAndUpdateMap(stateDataList, (Map)entitiesDataMap);
         }
      }

      return new ArrayList(((Map)entitiesDataMap).values());
   }

   private Map aggregateStateDataAndUpdateMap(List stateDataList, Map entitiesDataMap) {
      Iterator var3 = stateDataList.iterator();

      while(var3.hasNext()) {
         PerfEntityStateData stateData = (PerfEntityStateData)var3.next();
         PerfEntityStateData aggregatedStateData = (PerfEntityStateData)entitiesDataMap.get(stateData.entityRefId);
         if (aggregatedStateData == null) {
            aggregatedStateData = new PerfEntityStateData();
            aggregatedStateData.metricsSeries = new ArrayList();
            aggregatedStateData.timeStamps = new ArrayList();
         }

         if (aggregatedStateData.timeStamps.size() > 0 && stateData.timeStamps.size() > 0 && ((String)aggregatedStateData.timeStamps.get(aggregatedStateData.timeStamps.size() - 1)).equalsIgnoreCase((String)stateData.timeStamps.get(0))) {
            stateData.timeStamps.remove(0);
            stateData.metricsSeries = this.removeFirstPoint(stateData.metricsSeries);
         }

         aggregatedStateData.entityRefId = stateData.entityRefId;
         aggregatedStateData.metricsSeries = this.aggregateMetricsData(aggregatedStateData.metricsSeries, stateData.metricsSeries);
         aggregatedStateData.timeStamps.addAll(stateData.timeStamps);
         aggregatedStateData.metricsCollectInterval = stateData.metricsCollectInterval;
         entitiesDataMap.put(stateData.entityRefId, aggregatedStateData);
      }

      return entitiesDataMap;
   }

   private List removeFirstPoint(List metricsSeries) {
      if (metricsSeries.isEmpty()) {
         return metricsSeries;
      } else {
         Iterator var2 = metricsSeries.iterator();

         while(var2.hasNext()) {
            PerfGraphMetricsData data = (PerfGraphMetricsData)var2.next();
            if (data != null && !data.values.isEmpty()) {
               data.values.remove(0);
            }
         }

         return metricsSeries;
      }
   }

   private List aggregateMetricsData(List base, List newMetrics) {
      if (CollectionUtils.isEmpty(base)) {
         return newMetrics;
      } else {
         Iterator var3 = base.iterator();

         while(true) {
            while(var3.hasNext()) {
               PerfGraphMetricsData data = (PerfGraphMetricsData)var3.next();
               Iterator var5 = newMetrics.iterator();

               while(var5.hasNext()) {
                  PerfGraphMetricsData newData = (PerfGraphMetricsData)var5.next();
                  if (newData.key.equals(data.key)) {
                     data.values.addAll(newData.values);
                     break;
                  }
               }
            }

            return base;
         }
      }
   }

   @TsService
   public List getClusterDiskMappings(ManagedObjectReference clusterRef) throws Exception {
      List hostDiskgroups = new ArrayList();
      DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), new String[]{"name", "config.vsanHostConfig.storageInfo.diskMapping"});
      if (response == null) {
         return hostDiskgroups;
      } else {
         Iterator var4 = response.getResourceObjects().iterator();

         while(true) {
            ManagedObjectReference hostRef;
            DiskMapping[] diskMappings;
            do {
               if (!var4.hasNext()) {
                  return hostDiskgroups;
               }

               Object resourceObject = var4.next();
               hostRef = (ManagedObjectReference)resourceObject;
               diskMappings = (DiskMapping[])((DiskMapping[])response.getProperty(hostRef, "config.vsanHostConfig.storageInfo.diskMapping"));
            } while(ArrayUtils.isEmpty(diskMappings));

            List groups = new ArrayList();
            DiskMapping[] var9 = diskMappings;
            int var10 = diskMappings.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               DiskMapping diskMapping = var9[var11];
               groups.add(DiskGroup.fromDiskMapping(diskMapping));
            }

            HostDiskGroupsData diskgroupData = new HostDiskGroupsData();
            diskgroupData.hostName = (String)response.getProperty(hostRef, "name");
            diskgroupData.diskgroups = groups;
            hostDiskgroups.add(diskgroupData);
         }
      }
   }

   @TsService
   public CapacityHistoryBasicInfo getCapacityHistoryBasicInfo(ManagedObjectReference objectRef) throws Exception {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objectRef);
      Validate.notNull(clusterRef);
      CapacityHistoryBasicInfo info = new CapacityHistoryBasicInfo();
      info.clusterRef = clusterRef;
      info.isEmptyCluster = this.vsanInventoryHelper.getNumberOfClusterHosts(clusterRef) == 0;
      info.entityTypes = this.getPerfEntityTypes(clusterRef);
      info.isPerformanceServiceEnabled = this.getPerfServiceEnabled(clusterRef);
      if (!info.isPerformanceServiceEnabled) {
         ManagedObjectReference vcRoot = VmodlHelper.getRootFolder(clusterRef.getServerGuid());
         ManagedObjectReference[] refs = new ManagedObjectReference[]{clusterRef, vcRoot};
         String[] privileges = new String[]{"Host.Inventory.EditCluster", "StorageProfile.View"};
         info.hasEditPolicyPermission = this.permissionService.havePermissions(refs, privileges);
      }

      return info;
   }

   @TsService
   public PerfEntityStateData getHistoricalSpaceReport(ManagedObjectReference objectRef, PerfQuerySpec[] specs, boolean filterChartPoints) throws Exception {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objectRef);
      Validate.notNull(clusterRef);
      String uuid = "";
      ConfigInfoEx configInfoEx = this.vsanConfigService.getConfigInfoEx(clusterRef);
      SpaceEfficiencyConfig spaceEfficiencyConfig = SpaceEfficiencyConfig.fromVmodl(configInfoEx.dataEfficiencyConfig);
      if (configInfoEx.getDefaultConfig() != null && configInfoEx.getDefaultConfig().getUuid() != null) {
         uuid = configInfoEx.getDefaultConfig().getUuid();
         List allCharts = this.getAllChartsForCapacityHistory(clusterRef, specs, uuid);
         if (allCharts == null) {
            return null;
         } else {
            PerfEntityStateData capacityChart = null;
            Iterator var10 = allCharts.iterator();

            while(var10.hasNext()) {
               PerfEntityStateData chart = (PerfEntityStateData)var10.next();
               if (chart.entityRefId.indexOf("vsan-cluster-capacity") > -1) {
                  if (!this.isValuableMetric(chart)) {
                     return null;
                  }

                  capacityChart = chart;
               }
            }

            if (!spaceEfficiencyConfig.isEnabled()) {
               capacityChart = this.removeSpaceEfficiencyData(capacityChart);
            }

            this.deleteTotalDpOverhead(capacityChart);
            return filterChartPoints ? this.filterInvalidChartPoints(capacityChart) : capacityChart;
         }
      } else {
         logger.error("Failed to retrieve uuid for cluster: " + clusterRef);
         throw new VsanUiLocalizableException("vsan.perf.query.uuid.error");
      }
   }

   private List getAllChartsForCapacityHistory(ManagedObjectReference clusterRef, PerfQuerySpec[] specs, String uuid) throws Exception {
      PerfQuerySpec[] var4 = specs;
      int var5 = specs.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         PerfQuerySpec spec = var4[var6];
         spec.entityUuid = uuid;
      }

      List perfState = this.getEntityPerfState(clusterRef, specs);
      if (perfState.isEmpty()) {
         return null;
      } else {
         Map entitiesDataMap = new HashMap();
         Map entitiesDataMap = this.aggregateStateDataAndUpdateMap(perfState, entitiesDataMap);
         return new ArrayList(entitiesDataMap.values());
      }
   }

   private void deleteTotalDpOverhead(PerfEntityStateData capacityChart) {
      for(int index = 0; index < capacityChart.metricsSeries.size(); ++index) {
         if ("totalDpOverhead".equalsIgnoreCase(((PerfGraphMetricsData)capacityChart.metricsSeries.get(index)).key)) {
            capacityChart.metricsSeries.remove(index);
            break;
         }
      }

   }

   @TsService
   public List getEntityPerfState(ManagedObjectReference clusterRef, PerfQuerySpec[] specs) throws Exception {
      if (ArrayUtils.isEmpty(specs)) {
         logger.error("Invalid perf query specs are passed.");
      }

      List querySpecs = this.createQuerySpecs(specs);
      EntityPerfStateObject perfState = new EntityPerfStateObject();
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var6 = null;

      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

         try {
            VsanProfiler.Point p = profiler.point("perfMgr.queryVsanPerf");
            Throwable var48 = null;

            try {
               perfState.metrics = perfMgr.queryVsanPerf((VsanPerfQuerySpec[])querySpecs.toArray(new VsanPerfQuerySpec[0]), clusterRef);
            } catch (Throwable var40) {
               var48 = var40;
               throw var40;
            } finally {
               if (p != null) {
                  if (var48 != null) {
                     try {
                        p.close();
                     } catch (Throwable var39) {
                        var48.addSuppressed(var39);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Timedout var42) {
            perfState.errorMessage = var42.getLocalizedMessage();
         } catch (VimFault var43) {
            throw var43;
         } catch (InvalidArgument var44) {
            if (var44.getInvalidProperty().equals("entityRefId")) {
               RuntimeFault runtimeFault = new RuntimeFault();
               runtimeFault.setMessage("InvalidEntityRefID");
               throw runtimeFault;
            }

            throw Utils.getMethodFault(var44);
         } catch (Exception var45) {
            throw Utils.getMethodFault(var45);
         }
      } catch (Throwable var46) {
         var6 = var46;
         throw var46;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var38) {
                  var6.addSuppressed(var38);
               }
            } else {
               conn.close();
            }
         }

      }

      return Arrays.stream(specs).anyMatch((spec) -> {
         return spec.entityType.equals("ioinsight-histogram");
      }) ? this.parseIoInsightStateObjectChartData(querySpecs, perfState) : this.parseStateObjectChartData(perfState, querySpecs);
   }

   @TsService
   public List getTopContributors(ManagedObjectReference clusterRef, PerfTopContributorsQuerySpec spec) {
      VsanPerfTopQuerySpec querySpec = PerfTopContributorsQuerySpec.toVmodl(spec);
      VsanPerfEntityMetricCSV[] vsanPerfEntityMetricCSVS = new VsanPerfEntityMetricCSV[0];
      DataServiceResponse vmProperties = null;
      Map mappings = null;
      if (PerfTopContributorsEntityType.VIRTUAL_MACHINE.equals(spec.entity)) {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var8 = null;

         try {
            VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

            try {
               Measure measure = new Measure("VsanPerfPropertyProvider.getTopContributors");
               Throwable var11 = null;

               try {
                  vsanPerfEntityMetricCSVS = perfMgr.queryVsanPerfTopEntities(clusterRef, querySpec);
               } catch (Throwable var38) {
                  var11 = var38;
                  throw var38;
               } finally {
                  if (measure != null) {
                     if (var11 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var37) {
                           var11.addSuppressed(var37);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }
            } catch (Exception var41) {
               logger.error("Failed to retrieve Top Contributors: ", var41);
               throw new VsanUiLocalizableException(var41);
            }
         } catch (Throwable var42) {
            var8 = var42;
            throw var42;
         } finally {
            if (conn != null) {
               if (var8 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var36) {
                     var8.addSuppressed(var36);
                  }
               } else {
                  conn.close();
               }
            }

         }

         vmProperties = this.getVmProperties(clusterRef, vsanPerfEntityMetricCSVS);
      } else {
         try {
            ManagedObjectReference[] hosts = (ManagedObjectReference[])QueryUtil.getProperty(clusterRef, "host", (Object)null);
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(new NoOpMeasure(), clusterRef).loadTopContributors(querySpec).loadDiskMappings(Arrays.asList(hosts));
            mappings = dataRetriever.getDiskMappings();
            vsanPerfEntityMetricCSVS = dataRetriever.getTopContributors();
         } catch (Exception var39) {
            logger.warn("Failed to get host to disk groups mapping for cluster: " + clusterRef);
         }
      }

      List result = new ArrayList();
      VsanPerfEntityMetricCSV[] var47 = vsanPerfEntityMetricCSVS;
      int var48 = vsanPerfEntityMetricCSVS.length;

      for(int var49 = 0; var49 < var48; ++var49) {
         VsanPerfEntityMetricCSV entityMetricCSV = var47[var49];
         boolean isVmOnVsanDatastore = this.isVmOnVsanDatastore(spec.entity, vmProperties, entityMetricCSV);
         PerfTopContributorsEntity entity = PerfTopContributorsEntity.parsePerfEntityMetricCSV(entityMetricCSV, vmProperties, mappings, spec.entity, isVmOnVsanDatastore);
         if (entity != null) {
            result.add(entity);
         }
      }

      return result;
   }

   private boolean isVmOnVsanDatastore(PerfTopContributorsEntityType entityType, DataServiceResponse vmProperties, VsanPerfEntityMetricCSV entityMetricCSV) {
      boolean isVmOnVsanDatastore = false;
      if (PerfTopContributorsEntityType.VIRTUAL_MACHINE.equals(entityType)) {
         isVmOnVsanDatastore = vmProperties.getMap().entrySet().stream().filter((entry) -> {
            return filterVm(entityMetricCSV.entityRefId, entry);
         }).map((filteredEntity) -> {
            return (ManagedObjectReference)filteredEntity.getKey();
         }).anyMatch((vmRef) -> {
            return this.vsanInventoryHelper.getVsanDatastore(vmRef) != null;
         });
      }

      return isVmOnVsanDatastore;
   }

   private static boolean filterVm(String entityRefId, Entry entry) {
      return entityRefId.contains((String)((Map)entry.getValue()).get("config.instanceUuid"));
   }

   private DataServiceResponse getVmProperties(ManagedObjectReference clusterRef, VsanPerfEntityMetricCSV[] vsanPerfEntityMetricCSVS) {
      List vmNodeUuids = (List)Arrays.stream(vsanPerfEntityMetricCSVS).map(VsanPerfPropertyProvider::extractNodeUuid).collect(Collectors.toList());
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select("name", "primaryIconId", "config.instanceUuid").from(clusterRef).join(VirtualMachine.class).on("vm").where().propertyEqualsAnyOf("config.instanceUuid", (Collection)vmNodeUuids).end().build();
      return this.queryExecutor.execute(requestSpec).getDataServiceResponse();
   }

   private static String extractNodeUuid(VsanPerfEntityMetricCSV vsanPerfEntityMetricCSV) {
      return QueryUtil.extractNodeUuid(vsanPerfEntityMetricCSV.entityRefId);
   }

   private List parseIoInsightStateObjectChartData(List querySpecs, EntityPerfStateObject perfState) {
      VsanPerfEntityMetricCSV[] histogramMetrics = (VsanPerfEntityMetricCSV[])Arrays.stream(perfState.metrics).filter((metric) -> {
         return metric.entityRefId.startsWith("ioinsight-histogram");
      }).toArray((x$0) -> {
         return new VsanPerfEntityMetricCSV[x$0];
      });
      Arrays.stream(histogramMetrics).forEach((metric) -> {
         perfState.metrics = (VsanPerfEntityMetricCSV[])((VsanPerfEntityMetricCSV[])ArrayUtils.removeElement(perfState.metrics, metric));
      });
      List stateDataList = this.parseStateObjectChartData(perfState, querySpecs);
      this.mergeIoInsightMetrics(histogramMetrics, stateDataList);
      Collections.sort(stateDataList, new Comparator() {
         public int compare(PerfEntityStateData entityStateData1, PerfEntityStateData entityStateData2) {
            return entityStateData1.entityRefId.startsWith("ioinsight") ? 1 : -1;
         }
      });
      return stateDataList;
   }

   private List createQuerySpecs(PerfQuerySpec[] specs) {
      List querySpecs = new ArrayList(specs.length);
      PerfQuerySpec[] var3 = specs;
      int var4 = specs.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         PerfQuerySpec spec = var3[var5];
         querySpecs.add(PerfQuerySpec.toVmodl(spec));
         if (spec.entityType.equals("ioinsight")) {
            spec.entityType = "ioinsight-histogram";
            VsanPerfQuerySpec histogramSpec = PerfQuerySpec.toVmodl(spec);
            querySpecs.add(histogramSpec);
         }
      }

      return querySpecs;
   }

   private void mergeIoInsightMetrics(VsanPerfEntityMetricCSV[] histogramMetrics, List perfEntityStateData) {
      Arrays.stream(histogramMetrics).forEach((ioInsightHistogramMetricsCSV) -> {
         if (!ArrayUtils.isEmpty(ioInsightHistogramMetricsCSV.value)) {
            String ioInsightEntityRefId = ioInsightHistogramMetricsCSV.entityRefId.replace("ioinsight-histogram", "ioinsight");
            PerfEntityStateData ioInsightMetricStateData = (PerfEntityStateData)perfEntityStateData.stream().filter((data) -> {
               return data.entityRefId.equals(ioInsightEntityRefId);
            }).findFirst().orElse((Object)null);
            if (ioInsightMetricStateData != null) {
               VsanPerfMetricSeriesCSV[] var4 = ioInsightHistogramMetricsCSV.value;
               int var5 = var4.length;

               for(int var6 = 0; var6 < var5; ++var6) {
                  VsanPerfMetricSeriesCSV metricCSV = var4[var6];
                  PerfGraphMetricsData metricsData = new PerfGraphMetricsData();
                  metricsData.key = metricCSV.metricId.label;
                  String[] rawValues = metricCSV.values.split(",");
                  Double newValue = 0.0D;
                  if (rawValues.length == 1) {
                     newValue = Double.parseDouble(rawValues[0]);
                  }

                  metricsData.values = new ArrayList();
                  metricsData.values.add(newValue);
                  ioInsightMetricStateData.metricsSeries.add(metricsData);
               }

            }
         }
      });
   }

   private List parseStateObjectChartData(EntityPerfStateObject perfState, List querySpecs) {
      List stateDataList = new ArrayList();
      PerfMetricsInfo metricsInfo = PerfMetricsInfo.extractMetricsInfo(perfState.metrics);

      for(int metricIndex = 0; metricIndex < perfState.metrics.length; ++metricIndex) {
         long startTime = 0L;
         long endTime = 0L;
         if (querySpecs.size() == 1) {
            startTime = ((VsanPerfQuerySpec)querySpecs.get(0)).startTime.getTimeInMillis();
            endTime = ((VsanPerfQuerySpec)querySpecs.get(0)).endTime.getTimeInMillis();
         } else {
            VsanPerfQuerySpec spec = (VsanPerfQuerySpec)querySpecs.get(metricIndex);
            startTime = spec.startTime.getTimeInMillis();
            endTime = spec.endTime.getTimeInMillis();
         }

         VsanPerfEntityMetricCSV metric = perfState.metrics[metricIndex];
         if (!StringUtils.isEmpty(metric.sampleInfo) && !ArrayUtils.isEmpty(metric.value)) {
            stateDataList.add(PerfEntityStateData.parsePerfEntityMetricCSV(metric, startTime, endTime));
         } else if (!MapUtils.isEmpty(metricsInfo.entityRefIdToIntervalMap) && !MapUtils.isEmpty(metricsInfo.entityRefIdToMetricIdMap) && metricsInfo.entityRefIdToIntervalMap.get(metric.entityRefId) != null) {
            int interval = (Integer)metricsInfo.entityRefIdToIntervalMap.get(metric.entityRefId);
            List metricIds = (List)metricsInfo.entityRefIdToMetricIdMap.get(metric.entityRefId);
            stateDataList.add(PerfEntityStateData.parsePerfEntityMetricCSV(metric, startTime, endTime, interval, metricIds));
         }
      }

      return stateDataList;
   }

   private PerfEntityStateData removeSpaceEfficiencyData(PerfEntityStateData chart) {
      Iterator iterator = chart.metricsSeries.iterator();

      while(true) {
         PerfGraphMetricsData metric;
         do {
            if (!iterator.hasNext()) {
               return chart;
            }

            metric = (PerfGraphMetricsData)iterator.next();
         } while(!metric.key.equalsIgnoreCase("savedByDedup") && !metric.key.equalsIgnoreCase("dedupRatio"));

         iterator.remove();
      }
   }

   private PerfEntityStateData filterInvalidChartPoints(PerfEntityStateData metric) {
      if (!this.isValuableMetric(metric)) {
         return null;
      } else if (this.hasLimitedNumberOfMetrics((PerfGraphMetricsData)metric.metricsSeries.get(0))) {
         return metric;
      } else {
         int intervalBetweenPoints = this.getIntervalBetweenPoints(metric);
         List newSampleInfos = new ArrayList();

         for(int infosIndex = 0; infosIndex < metric.timeStamps.size(); ++infosIndex) {
            if (this.isVisiblePoint(infosIndex, intervalBetweenPoints)) {
               newSampleInfos.add(metric.timeStamps.get(infosIndex));
            }
         }

         if (newSampleInfos.size() == 0) {
            return null;
         } else {
            metric.timeStamps = newSampleInfos;

            PerfGraphMetricsData value;
            ArrayList newValues;
            for(Iterator var9 = metric.metricsSeries.iterator(); var9.hasNext(); value.values = newValues) {
               value = (PerfGraphMetricsData)var9.next();
               newValues = new ArrayList();

               for(int valuesIndex = 0; valuesIndex < value.values.size(); ++valuesIndex) {
                  if (this.isVisiblePoint(valuesIndex, intervalBetweenPoints)) {
                     newValues.add(value.values.get(valuesIndex));
                  }
               }
            }

            boolean isEmptyChart = true;
            Iterator var11 = metric.metricsSeries.iterator();

            while(true) {
               while(var11.hasNext()) {
                  PerfGraphMetricsData value = (PerfGraphMetricsData)var11.next();
                  Iterator var13 = value.values.iterator();

                  while(var13.hasNext()) {
                     Double pointValue = (Double)var13.next();
                     if (pointValue != null) {
                        isEmptyChart = false;
                        break;
                     }
                  }
               }

               return isEmptyChart ? null : metric;
            }
         }
      }
   }

   private boolean isValuableMetric(PerfEntityStateData metric) {
      return metric != null && !metric.metricsSeries.isEmpty() && !metric.timeStamps.isEmpty();
   }

   private boolean hasLimitedNumberOfMetrics(PerfGraphMetricsData metricsData) {
      return metricsData != null && !metricsData.values.isEmpty() && metricsData.values.stream().filter((value) -> {
         return value != null;
      }).count() < 24L;
   }

   private int getIntervalBetweenPoints(PerfEntityStateData metric) {
      int metricsInterval = metric.metricsCollectInterval == 0 ? 60 : metric.metricsCollectInterval;
      return (int)Math.floor((double)(3600 / metricsInterval));
   }

   private boolean isVisiblePoint(int index, int interval) {
      return index % interval == 0;
   }

   @TsService
   public Boolean getPerfServiceEnabled(ManagedObjectReference clusterRef) {
      VsanPerfsvcConfig perfsvcConfig = this.getPerfServiceConfig(clusterRef);
      return this.getPerfServiceEnabled(clusterRef, perfsvcConfig);
   }

   private VsanPerfsvcConfig getPerfServiceConfig(ManagedObjectReference clusterRef) {
      if (VsanCapabilityUtils.isPerfSvcAutoConfigSupportedOnVc(clusterRef)) {
         try {
            return this.vsanConfigService.getConfigInfoEx(clusterRef).perfsvcConfig;
         } catch (Exception var3) {
            logger.error("Failed to retrieve performance service status: ", var3);
         }
      }

      logger.warn("No performance service config is fetched.");
      return null;
   }

   private Boolean getPerfServiceEnabled(ManagedObjectReference clusterRef, VsanPerfsvcConfig perfsvcConfig) {
      if (perfsvcConfig != null) {
         return perfsvcConfig.enabled;
      } else {
         VsanObjectInformation vsanPerfObjInfo = this.getPerfServiceObject(clusterRef);
         return vsanPerfObjInfo != null ? vsanPerfObjInfo.vsanObjectUuid != null : false;
      }
   }

   private VsanObjectInformation getPerfServiceObject(ManagedObjectReference clusterRef) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

         try {
            Measure measure = new Measure("perfMgr.queryStatsObjectInformation");
            Throwable var6 = null;

            try {
               Object var7;
               try {
                  var7 = perfMgr.queryStatsObjectInformation(clusterRef);
                  return (VsanObjectInformation)var7;
               } catch (Throwable var33) {
                  var7 = var33;
                  var6 = var33;
                  throw var33;
               }
            } finally {
               if (measure != null) {
                  if (var6 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var32) {
                        var6.addSuppressed(var32);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var35) {
            logger.error("Failed to retrieve the performance service stats object information: ", var35);
         }
      } catch (Throwable var36) {
         var3 = var36;
         throw var36;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var31) {
                  var3.addSuppressed(var31);
               }
            } else {
               conn.close();
            }
         }

      }

      logger.warn("No performance stats object is fetched.");
      return null;
   }

   @TsService
   public PerfEditData getPerfEditData(ManagedObjectReference clusterRef) {
      Measure measure = new Measure("Fetch all needed data for perf edit");
      Throwable var3 = null;

      PerfEditData var8;
      try {
         PerfEditData perfEditData = new PerfEditData();
         VsanAsyncDataRetriever retriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadConfigInfoEx().loadStatsObjectInformation();
         ConfigInfoEx configInfoEx = retriever.getConfigInfoEx();
         VsanPerfsvcConfig perfsvcConfig;
         if (configInfoEx == null) {
            logger.error("Unable to fetch ConfigInfoEx");
            perfsvcConfig = null;
            return perfsvcConfig;
         }

         perfsvcConfig = configInfoEx.perfsvcConfig;
         perfEditData.isPerformanceEnabled = this.getPerfServiceEnabled(clusterRef, perfsvcConfig);
         if (!perfEditData.isPerformanceEnabled) {
            var8 = perfEditData;
            return var8;
         }

         if (VsanCapabilityUtils.isFileAnalyticsSupportedOnVc(clusterRef)) {
            perfEditData.isFileAnalyticsEnabled = this.isFileServiceEnabled(configInfoEx) && this.isFileAnalyticsEnabled(configInfoEx);
         }

         perfEditData.perfStatsObjectInfo = this.getPerfStatsInfo(clusterRef, retriever.getStatsObjectInformation());
         perfEditData.policyId = this.getConfiguredPolicy(clusterRef, perfsvcConfig, perfEditData.perfStatsObjectInfo);
         var8 = perfEditData;
      } catch (Throwable var19) {
         var3 = var19;
         throw var19;
      } finally {
         if (measure != null) {
            if (var3 != null) {
               try {
                  measure.close();
               } catch (Throwable var18) {
                  var3.addSuppressed(var18);
               }
            } else {
               measure.close();
            }
         }

      }

      return var8;
   }

   @TsService
   public PerfTimeRangeData[] getSavedTimeRanges(ManagedObjectReference clusterRef) throws Exception {
      List list = new ArrayList();
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

         try {
            VsanProfiler.Point p = profiler.point("perfMgr.queryTimeRanges");
            Throwable var7 = null;

            try {
               VsanPerfTimeRange[] ranges = perfMgr.queryTimeRanges(clusterRef, new VsanPerfTimeRangeQuerySpec());
               if (!ArrayUtils.isEmpty(ranges)) {
                  VsanPerfTimeRange[] var9 = ranges;
                  int var10 = ranges.length;

                  for(int var11 = 0; var11 < var10; ++var11) {
                     VsanPerfTimeRange range = var9[var11];
                     PerfTimeRangeData t = new PerfTimeRangeData();
                     t.name = range.name;
                     t.from = range.startTime.getTime();
                     t.to = range.endTime.getTime();
                     list.add(t);
                  }
               }
            } catch (Throwable var37) {
               var7 = var37;
               throw var37;
            } finally {
               if (p != null) {
                  if (var7 != null) {
                     try {
                        p.close();
                     } catch (Throwable var36) {
                        var7.addSuppressed(var36);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Exception var39) {
         }
      } catch (Throwable var40) {
         var4 = var40;
         throw var40;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var35) {
                  var4.addSuppressed(var35);
               }
            } else {
               conn.close();
            }
         }

      }

      PerfTimeRangeData[] t = new PerfTimeRangeData[list.size()];
      return (PerfTimeRangeData[])list.toArray(t);
   }

   @TsService
   public String getConfiguredPolicy(ManagedObjectReference clusterRef) throws Exception {
      boolean isPerfSvcAutoConfigSupported = VsanCapabilityUtils.isPerfSvcAutoConfigSupportedOnVc(clusterRef);
      if (isPerfSvcAutoConfigSupported) {
         VsanPerfsvcConfig perfsvcConfig = this.getPerfServiceConfig(clusterRef);
         String profileId = this.getPerfPolicy(perfsvcConfig);
         if (profileId != null) {
            return profileId;
         }
      }

      return this.getPerfStatsInfo(clusterRef).spbmProfileUuid;
   }

   private String getConfiguredPolicy(ManagedObjectReference clusterRef, VsanPerfsvcConfig perfsvcConfig, PerfStatsObjectInfo perfStatsObjectInfo) {
      boolean isPerfSvcAutoConfigSupported = VsanCapabilityUtils.isPerfSvcAutoConfigSupportedOnVc(clusterRef);
      if (isPerfSvcAutoConfigSupported) {
         String profileId = this.getPerfPolicy(perfsvcConfig);
         if (profileId != null) {
            return profileId;
         }
      }

      return perfStatsObjectInfo.spbmProfileUuid;
   }

   private String getPerfPolicy(VsanPerfsvcConfig perfsvcConfig) {
      if (perfsvcConfig != null && perfsvcConfig.profile instanceof DefinedProfileSpec) {
         DefinedProfileSpec profileSpec = (DefinedProfileSpec)perfsvcConfig.profile;
         if (profileSpec != null) {
            return profileSpec.profileId;
         }
      }

      return null;
   }

   @TsService
   public PerfStatsObjectInfo getPerfStatsInfo(ManagedObjectReference clusterRef) {
      try {
         VsanObjectInformation vsanObjectInfo = this.getPerfServiceObject(clusterRef);
         return this.getPerfStatsInfo(clusterRef, vsanObjectInfo);
      } catch (Exception var3) {
         return new PerfStatsObjectInfo();
      }
   }

   public PerfStatsObjectInfo getPerfStatsInfo(ManagedObjectReference clusterRef, VsanObjectInformation vsanObjectInfo) {
      PerfStatsObjectInfo perfStatsObject = PerfStatsObjectInfo.fromVmodl(vsanObjectInfo);
      if (perfStatsObject == null) {
         return null;
      } else {
         VsanPerfsvcConfig perfsvcConfig = this.getPerfServiceConfig(clusterRef);
         if (perfsvcConfig != null) {
            perfStatsObject.serviceEnabled = perfsvcConfig.enabled;
            perfStatsObject.networkDiagnosticModeEnabled = BooleanUtils.isTrue(perfsvcConfig.diagnosticMode);
            perfStatsObject.verboseModeEnabled = BooleanUtils.isTrue(perfsvcConfig.verboseMode);
         } else {
            perfStatsObject.serviceEnabled = vsanObjectInfo.vsanObjectUuid != null;
            perfStatsObject.verboseModeEnabled = this.getLegacyVerboseModeEnabled(clusterRef);
         }

         this.updatePolicy(clusterRef, perfStatsObject);
         return perfStatsObject;
      }
   }

   private void updatePolicy(ManagedObjectReference clusterRef, PerfStatsObjectInfo perfStatsObject) {
      ProfileId[] profileIds = this.pbmDataProvider.getProfileIds(clusterRef);
      if (ArrayUtils.isNotEmpty(profileIds)) {
         Arrays.stream(profileIds).filter((profileId) -> {
            return profileId.uniqueId.equals(perfStatsObject.spbmProfileUuid);
         }).findFirst().ifPresent((storageProfileId) -> {
            perfStatsObject.spbmProfile = this.pbmDataProvider.getProfile(clusterRef, storageProfileId);
         });
      }

   }

   private boolean getLegacyVerboseModeEnabled(ManagedObjectReference clusterRef) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      VsanPerfNodeInformation[] nodeInformation;
      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();
         VsanProfiler.Point p = profiler.point("perfMgr.queryNodeInformation");
         Throwable var7 = null;

         try {
            nodeInformation = perfMgr.queryNodeInformation(clusterRef);
         } catch (Throwable var30) {
            var7 = var30;
            throw var30;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var29) {
                     var7.addSuppressed(var29);
                  }
               } else {
                  p.close();
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

      VsanPerfNodeInformation statsMasterNode = getPerfMasterNode(nodeInformation);
      return statsMasterNode != null && statsMasterNode.masterInfo != null ? BooleanUtils.isTrue(statsMasterNode.masterInfo.verboseMode) : false;
   }

   private Map getPerfEntityTypes(ManagedObjectReference clusterRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      Map var8;
      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

         try {
            VsanProfiler.Point p = profiler.point("perfMgr.getSupportedEntityTypes");
            Throwable var6 = null;

            try {
               VsanPerfEntityType[] entityTypes = perfMgr.getSupportedEntityTypes();
               var8 = this.handlePerfEntityTypes(entityTypes);
            } catch (Throwable var33) {
               var6 = var33;
               throw var33;
            } finally {
               if (p != null) {
                  if (var6 != null) {
                     try {
                        p.close();
                     } catch (Throwable var32) {
                        var6.addSuppressed(var32);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Exception var35) {
            throw Utils.getMethodFault(var35);
         }
      } catch (Throwable var36) {
         var3 = var36;
         throw var36;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var31) {
                  var3.addSuppressed(var31);
               }
            } else {
               conn.close();
            }
         }

      }

      return var8;
   }

   public Map handlePerfEntityTypes(VsanPerfEntityType[] types) {
      if (ArrayUtils.isEmpty(types)) {
         return null;
      } else {
         Map entitySpecMap = new HashMap();
         VsanPerfEntityType[] var3 = types;
         int var4 = types.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanPerfEntityType entitySpec = var3[var5];
            entitySpecMap.put(entitySpec.id, entitySpec);
         }

         return entitySpecMap;
      }
   }

   @TsService
   public List getDiskMappings(ManagedObjectReference host) throws Exception {
      List groups = new ArrayList();
      DiskMapping[] diskMappings = (DiskMapping[])QueryUtil.getProperty(host, "config.vsanHostConfig.storageInfo.diskMapping", (Object)null);
      if (ArrayUtils.isEmpty(diskMappings)) {
         return groups;
      } else {
         DiskMapping[] var4 = diskMappings;
         int var5 = diskMappings.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            DiskMapping diskMapping = var4[var6];
            groups.add(DiskGroup.fromDiskMapping(diskMapping));
         }

         return groups;
      }
   }

   private static VsanPerfNodeInformation getPerfMasterNode(VsanPerfNodeInformation[] nodes) {
      if (ArrayUtils.isNotEmpty(nodes)) {
         VsanPerfNodeInformation[] var1 = nodes;
         int var2 = nodes.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            VsanPerfNodeInformation n = var1[var3];
            if (n.isStatsMaster) {
               return n;
            }
         }
      }

      return null;
   }

   @TsService
   public PerfMonitorCommonPropsData getPerfMonitorCommonPropsData(ManagedObjectReference objectRef) {
      PerfMonitorCommonPropsData data = new PerfMonitorCommonPropsData();
      data.clusterRef = BaseUtils.getCluster(objectRef);

      try {
         Measure measure = new Measure("Collect performance service related data");
         Throwable var5 = null;

         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, data.clusterRef).loadConfigInfoEx().loadNodeInformation().loadSupportedEntityTypes().loadStatsObjectInformation();
            ManagedObjectReference vcRef = VmodlHelper.getRootFolder(data.clusterRef.getServerGuid());
            Map vcPermissions = this.permissionService.queryPermissions(vcRef, new String[]{"StorageProfile.View", "Global.Diagnostics"});
            data.hasEditPrivilege = this.permissionService.hasPermissions(data.clusterRef, new String[]{"Host.Inventory.EditCluster"}) && (Boolean)vcPermissions.get("StorageProfile.View");
            data.hasIoInsightViewPrivilege = (Boolean)vcPermissions.get("Global.Diagnostics");
            data.entityTypes = this.handlePerfEntityTypes(dataRetriever.getSupportedEntityTypes());
            VsanPerfNodeInformation masterNode = getPerfMasterNode(dataRetriever.getNodeInformation());
            data.currentTimeOnMasterNode = this.getCurrentTimeOnMasterNode(masterNode);
            if (masterNode != null && masterNode.masterInfo != null && masterNode.masterInfo.verboseMode != null) {
               data.isVerboseModeEnabled = BooleanUtils.isTrue(masterNode.masterInfo.verboseMode);
            }

            ConfigInfoEx configInfo = dataRetriever.getConfigInfoEx();
            data.isIscsiServiceEnabled = this.isIscsiServiceEnabled(configInfo);
            data.isFileServiceEnabled = this.isFileServiceEnabled(configInfo);
            data.isEmptyClusterForIscsi = this.iscsiPropertyProvider.isEmptyClusterForIscsi(data.clusterRef);
            boolean isPerfSvcAutoConfigSupported = VsanCapabilityUtils.isPerfSvcAutoConfigSupportedOnVc(data.clusterRef);
            if (isPerfSvcAutoConfigSupported && configInfo != null && configInfo.perfsvcConfig != null) {
               data.isPerformanceServiceEnabled = configInfo.perfsvcConfig.enabled;
            } else {
               VsanObjectInformation objectInfo = dataRetriever.getStatsObjectInformation();
               data.isPerformanceServiceEnabled = objectInfo != null && objectInfo.vsanObjectUuid != null;
            }

            data.isIoInsightSupported = this.getIoInsightSupported(objectRef);
            data.isComputeOnlyCluster = VsanCapabilityUtils.isCsdSupported(data.clusterRef) && this.csdService.isComputeOnlyClusterByConfigInfoEx(configInfo);
            data.mountedRemoteDatastores = this.csdService.isCsdSupported(objectRef) ? this.csdService.getMountedDatastores(objectRef) : Collections.EMPTY_LIST;
            data.isPmemManagedByVsan = this.isPmemManagedByVsan(configInfo, objectRef);
            data.isTopContributorsSupported = this.getTopContributorsSupported(objectRef);
            if (HostSystem.class.getSimpleName().equalsIgnoreCase(objectRef.getType()) && VsanCapabilityUtils.isNetworkDiagnosticsSupportedOnVc(objectRef)) {
               data.networkDiagnostics = this.networkDiagnosticsService.getNetworkDiagnostics(objectRef);
            }
         } catch (Throwable var20) {
            var5 = var20;
            throw var20;
         } finally {
            if (measure != null) {
               if (var5 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var19) {
                     var5.addSuppressed(var19);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return data;
      } catch (Exception var22) {
         throw new VsanUiLocalizableException(var22);
      }
   }

   private boolean isIscsiServiceEnabled(ConfigInfoEx configInfo) {
      return configInfo != null && configInfo.getIscsiConfig() != null && configInfo.getIscsiConfig().getEnabled() != null ? configInfo.getIscsiConfig().getEnabled() : false;
   }

   private boolean isFileServiceEnabled(ConfigInfoEx configInfo) {
      return configInfo != null && configInfo.fileServiceConfig != null && configInfo.fileServiceConfig.enabled;
   }

   private boolean isFileAnalyticsEnabled(ConfigInfoEx configInfoEx) {
      return configInfoEx != null && configInfoEx.fileServiceConfig != null && BooleanUtils.isTrue(configInfoEx.fileServiceConfig.fileAnalyticsEnabled);
   }

   private Long getCurrentTimeOnMasterNode(VsanPerfNodeInformation perfMasterNode) throws Exception {
      if (perfMasterNode != null) {
         PropertyConstraint masterNode = QueryUtil.createPropertyConstraint(HostSystem.class.getSimpleName(), "name", com.vmware.vise.data.query.Comparator.EQUALS, perfMasterNode.hostname);
         String[] properties = new String[]{"currentTimeOnHost"};
         ResultSet resultSet = QueryUtil.getData(QueryUtil.buildQuerySpec((Constraint)masterNode, properties));
         DataServiceResponse response = QueryUtil.getDataServiceResponse(resultSet, properties);
         PropertyValue[] var6 = response.getPropertyValues();
         int var7 = var6.length;
         byte var8 = 0;
         if (var8 < var7) {
            PropertyValue propertyValue = var6[var8];
            Calendar time = (Calendar)propertyValue.value;
            return time.getTimeInMillis();
         }
      }

      return null;
   }

   private boolean getIoInsightSupported(ManagedObjectReference objectRef) throws Exception {
      if (!this.vmodlHelper.isCluster(objectRef) && !this.vmodlHelper.isOfType(objectRef, HostSystem.class)) {
         if (this.vmodlHelper.isOfType(objectRef, VirtualMachine.class)) {
            ManagedObjectReference hostRef = (ManagedObjectReference)QueryUtil.getProperty(objectRef, "host");
            return VsanCapabilityUtils.isIoInsightSupported(hostRef);
         } else {
            return false;
         }
      } else {
         return VsanCapabilityUtils.isIoInsightSupported(objectRef);
      }
   }

   private boolean getTopContributorsSupported(ManagedObjectReference objectRef) {
      return this.vmodlHelper.isCluster(objectRef) ? VsanCapabilityUtils.isTopContributorsSupported(objectRef) : false;
   }

   private boolean isPmemManagedByVsan(ConfigInfoEx configInfo, ManagedObjectReference objectRef) {
      if (!this.isPmemEnabled(configInfo)) {
         return false;
      } else if (this.vmodlHelper.isCluster(objectRef)) {
         return VsanCapabilityUtils.isManagedPMemSupportedOnVC(objectRef);
      } else {
         return this.vmodlHelper.isOfType(objectRef, HostSystem.class) ? VsanCapabilityUtils.isManagedPMemSupported(objectRef) : false;
      }
   }

   private boolean isPmemEnabled(ConfigInfoEx configInfo) {
      return configInfo != null && configInfo.vsanPMemConfig != null && BooleanUtils.isTrue(configInfo.vsanPMemConfig.enabled);
   }

   private Set getActiveVmnicsForStandardNetworkConfiguration(PortGroup[] portGroups, VirtualNic vn) {
      Set activePnics = new HashSet();
      if (ArrayUtils.isEmpty(portGroups)) {
         return activePnics;
      } else {
         PortGroup[] var4 = portGroups;
         int var5 = portGroups.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            PortGroup pgroup = var4[var6];
            if (!ArrayUtils.isEmpty(pgroup.port) && pgroup.computedPolicy != null && pgroup.computedPolicy.nicTeaming != null) {
               Port[] var8 = pgroup.port;
               int var9 = var8.length;

               for(int var10 = 0; var10 < var9; ++var10) {
                  Port p = var8[var10];
                  if (p.key != null && p.key.equals(vn.port)) {
                     NicOrderPolicy nicOrder = pgroup.computedPolicy.nicTeaming.nicOrder;
                     if (nicOrder != null) {
                        activePnics.addAll(Arrays.asList(nicOrder.activeNic));
                        break;
                     }
                  }
               }
            }
         }

         return activePnics;
      }
   }

   private Set getActiveVmnicsFromDistributedSwitch(String switchUuid, HostProxySwitch[] proxySwitches, String[] uplinks) {
      Set activePnics = new HashSet();
      if (!ArrayUtils.isEmpty(proxySwitches) && !ArrayUtils.isEmpty(uplinks) && !StringUtils.isBlank(switchUuid)) {
         HostProxySwitch[] var5 = proxySwitches;
         int var6 = proxySwitches.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            HostProxySwitch proxySwitch = var5[var7];
            if (switchUuid.equals(proxySwitch.dvsUuid) && !ArrayUtils.isEmpty(proxySwitch.uplinkPort)) {
               List activeUplinkKeys = new ArrayList();
               String[] var10 = uplinks;
               int var11 = uplinks.length;

               int var12;
               for(var12 = 0; var12 < var11; ++var12) {
                  String uplink = var10[var12];
                  KeyValue[] var14 = proxySwitch.uplinkPort;
                  int var15 = var14.length;

                  int var16;
                  for(var16 = 0; var16 < var15; ++var16) {
                     KeyValue kv = var14[var16];
                     if (kv.value.equals(uplink)) {
                        activeUplinkKeys.add(kv.key);
                     }
                  }

                  if (!ArrayUtils.isEmpty(proxySwitch.hostLag)) {
                     HostLagConfig[] var25 = proxySwitch.hostLag;
                     var15 = var25.length;

                     for(var16 = 0; var16 < var15; ++var16) {
                        HostLagConfig lagConfig = var25[var16];
                        if (lagConfig.lagName.equals(uplink) && !ArrayUtils.isEmpty(lagConfig.uplinkPort)) {
                           KeyValue[] var18 = lagConfig.uplinkPort;
                           int var19 = var18.length;

                           for(int var20 = 0; var20 < var19; ++var20) {
                              KeyValue k = var18[var20];
                              activeUplinkKeys.add(k.key);
                           }
                        }
                     }
                  }
               }

               if (!CollectionUtils.isEmpty(activeUplinkKeys) && proxySwitch.spec != null && proxySwitch.spec.backing != null) {
                  PnicBacking backing = (PnicBacking)proxySwitch.spec.backing;
                  if (!ArrayUtils.isEmpty(backing.pnicSpec)) {
                     PnicSpec[] var23 = backing.pnicSpec;
                     var12 = var23.length;

                     for(int var24 = 0; var24 < var12; ++var24) {
                        PnicSpec spec = var23[var24];
                        if (activeUplinkKeys.contains(spec.uplinkPortKey)) {
                           activePnics.add(spec.pnicDevice);
                        }
                     }
                  }
               }
            }
         }

         return activePnics;
      } else {
         return activePnics;
      }
   }

   private ActiveVmnicDataSpec getDvsConfigurationsFromHostNetworks(ManagedObjectReference[] networks) throws Exception {
      ActiveVmnicDataSpec vmnicSpec = new ActiveVmnicDataSpec();
      List switches = new ArrayList();
      Map uuidSwitchMap = new HashMap();
      Multimap switchNetworkMap = ArrayListMultimap.create();
      Map networkUplinksMap = new HashMap();
      PropertyValue[] pv = QueryUtil.getProperties(networks, new String[]{"config.distributedVirtualSwitch", "config.defaultPortConfig.uplinkTeamingPolicy.uplinkPortOrder.activeUplinkPort"}).getPropertyValues();
      PropertyValue[] props;
      int var10;
      if (!ArrayUtils.isEmpty(pv)) {
         props = pv;
         int var9 = pv.length;

         for(var10 = 0; var10 < var9; ++var10) {
            PropertyValue property = props[var10];
            ManagedObjectReference _network = (ManagedObjectReference)property.resourceObject;
            String var13 = property.propertyName;
            byte var14 = -1;
            switch(var13.hashCode()) {
            case -418780824:
               if (var13.equals("config.distributedVirtualSwitch")) {
                  var14 = 0;
               }
               break;
            case 406754676:
               if (var13.equals("config.defaultPortConfig.uplinkTeamingPolicy.uplinkPortOrder.activeUplinkPort")) {
                  var14 = 1;
               }
            }

            switch(var14) {
            case 0:
               ManagedObjectReference _switch = (ManagedObjectReference)property.value;
               if (_switch != null) {
                  switches.add(_switch);
                  switchNetworkMap.put(_switch, _network);
               }
               break;
            case 1:
               String[] activeUplinks = (String[])((String[])property.value);
               if (activeUplinks != null) {
                  networkUplinksMap.put(_network, activeUplinks);
               }
            }
         }
      }

      if (!CollectionUtils.isEmpty(switches)) {
         props = QueryUtil.getProperties((ManagedObjectReference[])switches.toArray(new ManagedObjectReference[0]), new String[]{"uuid"}).getPropertyValues();
         if (!ArrayUtils.isEmpty(props)) {
            PropertyValue[] var17 = props;
            var10 = props.length;

            for(int var18 = 0; var18 < var10; ++var18) {
               PropertyValue prop = var17[var18];
               uuidSwitchMap.put((String)prop.value, (ManagedObjectReference)prop.resourceObject);
            }
         }
      }

      vmnicSpec.switches = switches;
      vmnicSpec.uuidSwitchMap = uuidSwitchMap;
      vmnicSpec.switchNetworkMap = switchNetworkMap;
      vmnicSpec.networkUplinksMap = networkUplinksMap;
      return vmnicSpec;
   }

   @TsService
   public List getHostPhysicalAdapterMapping(ManagedObjectReference serverObjRef) throws Exception {
      List hostPnics = new ArrayList();
      DataServiceResponse response = this.getPnicQueryData(serverObjRef);
      if (response == null) {
         return Collections.EMPTY_LIST;
      } else {
         Iterator var4 = response.getResourceObjects().iterator();

         while(true) {
            HashSet activePnics;
            ManagedObjectReference hostRef;
            String hostUuid;
            PhysicalNic[] physicalNics;
            VirtualNic[] virtualNics;
            PortGroup[] portGroups;
            HostProxySwitch[] proxySwitches;
            ManagedObjectReference[] networks;
            PortConfig[] portConfigs;
            OpaqueNetworkInfo[] opaqueNetworkInfos;
            OpaqueSwitch[] opaqueSwitches;
            do {
               do {
                  if (!var4.hasNext()) {
                     return hostPnics;
                  }

                  Object resourceObject = var4.next();
                  activePnics = new HashSet();
                  hostRef = (ManagedObjectReference)resourceObject;
                  hostUuid = (String)response.getProperty(hostRef, "config.vsanHostConfig.clusterInfo.nodeUuid");
                  physicalNics = (PhysicalNic[])response.getProperty(hostRef, "config.network.pnic");
                  virtualNics = (VirtualNic[])response.getProperty(hostRef, "config.network.vnic");
                  portGroups = (PortGroup[])response.getProperty(hostRef, "config.network.portgroup");
                  proxySwitches = (HostProxySwitch[])response.getProperty(hostRef, "config.network.proxySwitch");
                  networks = (ManagedObjectReference[])response.getProperty(hostRef, "network");
                  portConfigs = (PortConfig[])response.getProperty(hostRef, "config.vsanHostConfig.networkInfo.port");
                  opaqueNetworkInfos = (OpaqueNetworkInfo[])response.getProperty(hostRef, "config.network.opaqueNetwork");
                  opaqueSwitches = (OpaqueSwitch[])response.getProperty(hostRef, "config.network.opaqueSwitch");
               } while(ArrayUtils.isEmpty(portConfigs));
            } while(ArrayUtils.isEmpty(virtualNics));

            ActiveVmnicDataSpec vmnicSpec = null;
            if (networks != null) {
               vmnicSpec = this.getDvsConfigurationsFromHostNetworks(networks);
            }

            List vsanUsedVnics = NetworkUtils.getVsanUsedVnics(portConfigs, virtualNics);
            Iterator var19 = vsanUsedVnics.iterator();

            while(var19.hasNext()) {
               VirtualNic vnic = (VirtualNic)var19.next();
               if (NetworkUtils.isStandardModeVnic(vnic)) {
                  activePnics.addAll(this.getActiveVmnicsForStandardNetworkConfiguration(portGroups, vnic));
               } else {
                  String opaqueNetworkId;
                  if (NetworkUtils.isDistributedSwitchModeVnic(vnic)) {
                     opaqueNetworkId = vnic.spec.distributedVirtualPort.switchUuid;
                     String[] uplinkArr = this.getActiveUplinkNamesOnHost(vmnicSpec, vnic);
                     activePnics.addAll(this.getActiveVmnicsFromDistributedSwitch(opaqueNetworkId, proxySwitches, uplinkArr));
                  } else if (NetworkUtils.isNsxOpaqueSwitchModeVnic(vnic)) {
                     opaqueNetworkId = vnic.spec.opaqueNetwork.opaqueNetworkId;
                     String opaqueNetworkType = vnic.spec.opaqueNetwork.opaqueNetworkType;
                     OpaqueNetworkInfo opaqueNetwork = NetworkUtils.getOpaqueNetwork(opaqueNetworkId, opaqueNetworkType, opaqueNetworkInfos);
                     List switches = NetworkUtils.getOpaqueSwitchesAttachedToOpaqueNetwork(opaqueNetwork, opaqueSwitches);
                     activePnics.addAll(NetworkUtils.getPhysicalNicNamesFromOpaqueSwitches(physicalNics, switches));
                  }
               }
            }

            String[] activePnicArr = new String[activePnics.size()];
            activePnicArr = (String[])activePnics.toArray(activePnicArr);
            Arrays.sort(activePnicArr);
            List pnics = new ArrayList();
            String[] var26 = activePnicArr;
            int var29 = activePnicArr.length;

            for(int var30 = 0; var30 < var29; ++var30) {
               String pnic = var26[var30];
               PerfPhysicalAdapterEntity entity = new PerfPhysicalAdapterEntity();
               entity.hostUuid = hostUuid;
               entity.deviceName = pnic;
               pnics.add(entity);
            }

            HostPnicsData pnicData = new HostPnicsData();
            pnicData.hostName = (String)response.getProperty(hostRef, "name");
            pnicData.pnics = pnics;
            hostPnics.add(pnicData);
         }
      }
   }

   private DataServiceResponse getPnicQueryData(ManagedObjectReference serverObjRef) throws Exception {
      DataServiceResponse response = null;
      if (ClusterComputeResource.class.getSimpleName().equals(serverObjRef.getType())) {
         response = QueryUtil.getPropertiesForRelatedObjects(serverObjRef, "host", HostSystem.class.getSimpleName(), HOST_NETWORK_PROPS);
      } else if (HostSystem.class.getSimpleName().equals(serverObjRef.getType())) {
         response = QueryUtil.getProperties(serverObjRef, HOST_NETWORK_PROPS);
      }

      return response;
   }

   @TsService
   public List getHostPhysicalAdapters(ManagedObjectReference hostRef) throws Exception {
      List result = this.getHostPhysicalAdapterMapping(hostRef);
      return CollectionUtils.isEmpty(result) ? null : ((HostPnicsData)result.get(0)).pnics;
   }

   private String[] getActiveUplinkNamesOnHost(ActiveVmnicDataSpec vmnicSpec, VirtualNic vn) throws Exception {
      String switchUuid = vn.spec.distributedVirtualPort == null ? null : vn.spec.distributedVirtualPort.switchUuid;
      if (vmnicSpec != null && !StringUtils.isEmpty(switchUuid) && !CollectionUtils.isEmpty(vmnicSpec.switches)) {
         String portgroupKey = vn.spec.distributedVirtualPort.portgroupKey;
         Set uplinks = vmnicSpec.getUplinksBySwitchUuid(switchUuid, portgroupKey);
         String[] uplinkArr = new String[uplinks.size()];
         uplinkArr = (String[])uplinks.toArray(uplinkArr);
         return uplinkArr;
      } else {
         return new String[0];
      }
   }

   @TsService
   public List getHostVnicsMapping(ManagedObjectReference serverObj) throws Exception {
      List hostVnics = new ArrayList();
      DataServiceResponse response = this.getVnicQueryData(serverObj);
      if (response == null) {
         return hostVnics;
      } else {
         Iterator var4 = response.getResourceObjects().iterator();

         while(var4.hasNext()) {
            Object resourceObject = var4.next();
            ManagedObjectReference hostRef = (ManagedObjectReference)resourceObject;
            PortConfig[] portConfigs = (PortConfig[])((PortConfig[])response.getProperty(hostRef, "config.vsanHostConfig.networkInfo.port"));
            VirtualNic[] vnics = (VirtualNic[])((VirtualNic[])response.getProperty(hostRef, "config.network.vnic"));
            if (!ArrayUtils.isEmpty(portConfigs) && !ArrayUtils.isEmpty(vnics)) {
               String hostUuid = (String)response.getProperty(hostRef, "config.vsanHostConfig.clusterInfo.nodeUuid");
               List vnicEntities = NetworkUtils.getVsanUsedVnicEntities(portConfigs, vnics, hostUuid);
               HostVnicsData vnicsData = new HostVnicsData();
               vnicsData.hostName = (String)response.getProperty(hostRef, "name");
               vnicsData.vnics = vnicEntities;
               hostVnics.add(vnicsData);
            }
         }

         return hostVnics;
      }
   }

   private DataServiceResponse getVnicQueryData(ManagedObjectReference serverObj) throws Exception {
      DataServiceResponse response = null;
      if (ClusterComputeResource.class.getSimpleName().equals(serverObj.getType())) {
         response = QueryUtil.getPropertiesForRelatedObjects(serverObj, "host", HostSystem.class.getSimpleName(), new String[]{"name", "config.vsanHostConfig.clusterInfo.nodeUuid", "config.vsanHostConfig.networkInfo.port", "config.network.vnic"});
      } else if (HostSystem.class.getSimpleName().equals(serverObj.getType())) {
         response = QueryUtil.getProperties(serverObj, new String[]{"name", "config.vsanHostConfig.clusterInfo.nodeUuid", "config.vsanHostConfig.networkInfo.port", "config.network.vnic"});
      }

      return response;
   }

   @TsService
   public boolean isPerformanceServiceHealthy(ManagedObjectReference moRef) throws VsanUiLocalizableException {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(moRef);
      PerfStatsObjectInfo perfStatsObjectInfo = this.getPerfStatsInfo(clusterRef);
      if (perfStatsObjectInfo != null && perfStatsObjectInfo.serviceEnabled && perfStatsObjectInfo.vsanHealth != null && perfStatsObjectInfo.spbmComplianceResult != null) {
         VsanObjectHealthState perfObjectHealth = VsanObjectHealthState.fromString(perfStatsObjectInfo.vsanHealth.toLowerCase());
         StorageCompliance storageCompliance = StorageCompliance.fromName(perfStatsObjectInfo.spbmComplianceResult.complianceStatus);
         return perfObjectHealth == VsanObjectHealthState.HEALTHY && storageCompliance == StorageCompliance.compliant;
      } else {
         return false;
      }
   }

   @TsService
   public List getHostVirtualAdapters(ManagedObjectReference hostRef) throws Exception {
      List result = this.getHostVnicsMapping(hostRef);
      return CollectionUtils.isEmpty(result) ? null : ((HostVnicsData)result.get(0)).vnics;
   }

   @TsService
   public PerfVirtualMachineDiskData getVirtualMachineDiskData(ManagedObjectReference vmRef, boolean includeVirtualDiskData) throws Exception {
      PerfVirtualMachineDiskData data = new PerfVirtualMachineDiskData();
      DataServiceResponse response = QueryUtil.getProperties(vmRef, new String[]{"config.instanceUuid", "config.hardware.device"});
      if (response == null) {
         logger.warn("No disk data found for virtual machine:" + vmRef.getValue());
         return data;
      } else {
         Iterator var5 = response.getResourceObjects().iterator();

         while(var5.hasNext()) {
            Object resourceObject = var5.next();
            data.vmUuid = (String)response.getProperty(resourceObject, "config.instanceUuid");
            VirtualDevice[] vDevs = (VirtualDevice[])response.getProperty(resourceObject, "config.hardware.device");
            data.vscsiEntities = this.getVscsiEntities(vDevs);
            if (includeVirtualDiskData) {
               data.virtualDisks = this.getVirtualDiskEntities(vDevs);
            }
         }

         return data;
      }
   }

   private List getVscsiEntities(VirtualDevice[] vDevs) throws Exception {
      List vscsiEntities = new ArrayList();
      if (vDevs.length > 0) {
         List vDisks = new ArrayList();
         Map vConts = new HashMap();
         VirtualDevice[] var5 = vDevs;
         int var6 = vDevs.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            VirtualDevice vDev = var5[var7];
            if (vDev instanceof VirtualDisk) {
               vDisks.add((VirtualDisk)vDev);
            } else {
               try {
                  if (vDev.getClass().getField("scsiCtlrUnitNumber") != null) {
                     vConts.put(vDev.key, (VirtualController)vDev);
                  }
               } catch (Exception var17) {
               }
            }
         }

         Map datastoreMap = new HashMap();

         Object disksInSameStore;
         VirtualDisk vdisk;
         for(Iterator var19 = vDisks.iterator(); var19.hasNext(); ((List)disksInSameStore).add(vdisk)) {
            vdisk = (VirtualDisk)var19.next();
            FileBackingInfo backing = (FileBackingInfo)vdisk.getBacking();
            disksInSameStore = (List)datastoreMap.get(backing.datastore);
            if (disksInSameStore == null) {
               disksInSameStore = new ArrayList();
               datastoreMap.put(backing.datastore, disksInSameStore);
            }
         }

         ManagedObjectReference[] keyArr = (ManagedObjectReference[])datastoreMap.keySet().toArray(new ManagedObjectReference[0]);
         if (keyArr != null && keyArr.length > 0) {
            PropertyValue[] dsTypeValues = QueryUtil.getProperties(keyArr, new String[]{"summary.type"}).getPropertyValues();
            List vsanDsRefs = new ArrayList();
            PropertyValue[] var25 = dsTypeValues;
            int var10 = dsTypeValues.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               PropertyValue dsType = var25[var11];
               if (dsType.value.equals("vsan")) {
                  vsanDsRefs.add((ManagedObjectReference)dsType.resourceObject);
               }
            }

            List disksOnVsan = new ArrayList();
            Iterator var27 = vsanDsRefs.iterator();

            while(var27.hasNext()) {
               ManagedObjectReference dsRef = (ManagedObjectReference)var27.next();
               List disks = (List)datastoreMap.get(dsRef);
               if (disks != null) {
                  disksOnVsan.addAll(disks);
               }
            }

            Map controllerMap = new HashMap();

            Object disks;
            Iterator var30;
            VirtualDisk disk;
            for(var30 = disksOnVsan.iterator(); var30.hasNext(); ((List)disks).add(disk)) {
               disk = (VirtualDisk)var30.next();
               disks = (List)controllerMap.get(disk.controllerKey);
               if (disks == null) {
                  disks = new ArrayList();
                  controllerMap.put(disk.controllerKey, disks);
               }
            }

            var30 = controllerMap.entrySet().iterator();

            while(true) {
               Entry entry;
               VirtualController controller;
               do {
                  do {
                     if (!var30.hasNext()) {
                        return vscsiEntities;
                     }

                     entry = (Entry)var30.next();
                     controller = (VirtualController)vConts.get(entry.getKey());
                  } while(controller == null);
               } while(((List)entry.getValue()).isEmpty());

               Iterator var14 = ((List)entry.getValue()).iterator();

               while(var14.hasNext()) {
                  VirtualDisk disk = (VirtualDisk)var14.next();
                  PerfVscsiEntity entity = new PerfVscsiEntity();
                  entity.busId = controller.busNumber;
                  entity.controllerKey = controller.key;
                  entity.deviceName = disk.deviceInfo.label;
                  entity.vmdkName = ((FileBackingInfo)disk.backing).fileName;
                  entity.position = disk.unitNumber;
                  vscsiEntities.add(entity);
               }
            }
         }
      }

      return vscsiEntities;
   }

   private List getVirtualDiskEntities(VirtualDevice[] vDevs) throws Exception {
      List virtualDiskEntities = new ArrayList();
      Map dsMap = new HashMap();
      VirtualDevice[] var4 = vDevs;
      int var5 = vDevs.length;

      ManagedObjectReference dsRef;
      for(int var6 = 0; var6 < var5; ++var6) {
         VirtualDevice dev = var4[var6];
         if (dev instanceof VirtualDisk) {
            PerfVirtualDiskEntity entity = new PerfVirtualDiskEntity();
            entity.diskName = dev.deviceInfo.label;
            entity.controllerKey = dev.controllerKey;
            FileBackingInfo backing = (FileBackingInfo)dev.getBacking();
            dsRef = backing.datastore;
            Object type = QueryUtil.getProperty(dsRef, "summary.type", (Object)null);
            if (type != null && "vsan".equalsIgnoreCase(type.toString())) {
               virtualDiskEntities.add(entity);
               entity.vmdkPath = backing.getFileName();
               List disds = (List)dsMap.get(dsRef);
               if (disds == null) {
                  disds = new ArrayList();
                  dsMap.put(dsRef, disds);
               }

               ((List)disds).add(entity);
            }
         }
      }

      ManagedObjectReference[] keyArr = (ManagedObjectReference[])dsMap.keySet().toArray(new ManagedObjectReference[0]);
      int mark;
      if (keyArr != null && keyArr.length > 0) {
         PropertyValue[] dsPropsVals = QueryUtil.getProperties(keyArr, new String[]{"name", "summary.url"}).getPropertyValues();
         PropertyValue[] var19 = dsPropsVals;
         int var21 = dsPropsVals.length;

         label72:
         for(mark = 0; mark < var21; ++mark) {
            PropertyValue propVal = var19[mark];
            dsRef = (ManagedObjectReference)propVal.resourceObject;
            List pvdes = (List)dsMap.get(dsRef);
            String var26 = propVal.propertyName;
            byte var13 = -1;
            switch(var26.hashCode()) {
            case -1193995481:
               if (var26.equals("summary.url")) {
                  var13 = 1;
               }
               break;
            case 3373707:
               if (var26.equals("name")) {
                  var13 = 0;
               }
            }

            Iterator var14;
            PerfVirtualDiskEntity pvde;
            switch(var13) {
            case 0:
               var14 = pvdes.iterator();

               while(true) {
                  if (!var14.hasNext()) {
                     continue label72;
                  }

                  pvde = (PerfVirtualDiskEntity)var14.next();
                  pvde.datastoreName = propVal.value.toString();
               }
            case 1:
               for(var14 = pvdes.iterator(); var14.hasNext(); pvde.datastorePath = propVal.value.toString()) {
                  pvde = (PerfVirtualDiskEntity)var14.next();
               }
            }
         }
      }

      PerfVirtualDiskEntity pvde;
      for(Iterator var18 = virtualDiskEntities.iterator(); var18.hasNext(); pvde.datastorePath = pvde.datastorePath.substring(mark)) {
         pvde = (PerfVirtualDiskEntity)var18.next();
         String dsNamePair = "[" + pvde.datastoreName + "] ";
         mark = pvde.vmdkPath.indexOf(dsNamePair);
         pvde.vmdkPath = mark != -1 ? pvde.vmdkPath.substring(mark + dsNamePair.length()) : pvde.vmdkPath;
         mark = "ds://".length();
      }

      return virtualDiskEntities;
   }
}
