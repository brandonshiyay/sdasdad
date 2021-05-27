package com.vmware.vsan.client.services.capacity;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.vm.DefinedProfileSpec;
import com.vmware.vim.binding.vim.vm.ProfileSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterLimitHealthResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceReportSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsage;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsageDetailResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsageWithDatastoreType;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanLimitHealthResult;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.DataEfficiencyCapacityState;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.advancedoptions.AdvancedOptionsInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.capacity.model.CapacityData;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.capacity.model.HostStorageConsumptionData;
import com.vmware.vsan.client.services.capacity.model.ReducedCapacityData;
import com.vmware.vsan.client.services.capacity.model.WhatifCapacityData;
import com.vmware.vsan.client.services.config.CapacityReservationConfig;
import com.vmware.vsan.client.services.config.SpaceEfficiencyConfig;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.services.csd.model.MountedRemoteDatastore;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CapacityDataService {
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private ReducedCapacityMessagesService capacityMessagesService;
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private CsdService csdService;
   private static final Log logger = LogFactory.getLog(CapacityDataService.class);

   @TsService
   public Map getSpaceUsage(ManagedObjectReference objectRef, boolean summaryDataOnly) {
      return this.getSpaceUsage(objectRef, false, summaryDataOnly, false);
   }

   public Map getVsanSpaceUsage(ManagedObjectReference objectRef, boolean vSanDataOnly) {
      return this.getSpaceUsage(objectRef, vSanDataOnly, false, true);
   }

   private Map getSpaceUsage(ManagedObjectReference objectRef, boolean vSanDataOnly, boolean summaryDataOnly, boolean simulateCapacityEnforce) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objectRef);
      Validate.notNull(clusterRef);

      VsanSpaceUsageWithDatastoreType[] datastoresSpaceUsage;
      DataEfficiencyCapacityState dataEfficiencyCapacity;
      ConfigInfoEx vsanConfig;
      try {
         Measure measure = new Measure("getSpaceUsage");
         Throwable var10 = null;

         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadConfigInfoEx().loadDataEfficiencyCapacitySpace();
            if (!vSanDataOnly && !this.vmodlHelper.isOfType(objectRef, Datastore.class)) {
               dataRetriever.loadSpaceUsageData((DatastoreType)null);
            } else {
               dataRetriever.loadSpaceUsageData(DatastoreType.VSAN);
            }

            vsanConfig = dataRetriever.getConfigInfoEx();
            dataEfficiencyCapacity = dataRetriever.getDataEfficiencyCapacitySpace();
            datastoresSpaceUsage = dataRetriever.getSpaceUsageData();
         } catch (Throwable var20) {
            var10 = var20;
            throw var20;
         } finally {
            if (measure != null) {
               if (var10 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var19) {
                     var10.addSuppressed(var19);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Exception var22) {
         logger.error("Unable to get vSAN capacity data: ", var22);
         throw new VsanUiLocalizableException("vsan.common.generic.error");
      }

      CapacityReservationConfig capacityReservationConfig = AdvancedOptionsInfo.fromVmodl(vsanConfig, clusterRef).capacityReservationConfig;
      return (Map)((Map)Arrays.stream(datastoresSpaceUsage).collect(Collectors.toMap((datastoreSpaceUsage) -> {
         return DatastoreType.fromString(datastoreSpaceUsage.datastoreType);
      }, (datastoreSpaceUsage) -> {
         switch(DatastoreType.fromString(datastoreSpaceUsage.datastoreType)) {
         case VSAN:
            return this.calculateVsanSpaceUsage(clusterRef, datastoreSpaceUsage.spaceUsage, dataEfficiencyCapacity, SpaceEfficiencyConfig.fromVmodl(vsanConfig.dataEfficiencyConfig), capacityReservationConfig, summaryDataOnly, simulateCapacityEnforce);
         case VMFS:
            return this.calculateVsanDirectUsage(datastoreSpaceUsage.spaceUsage, summaryDataOnly);
         case PMEM:
            return this.calculatePmemUsage(datastoreSpaceUsage.spaceUsage, summaryDataOnly);
         default:
            logger.warn("Unknown datastore type when calculating capacity: " + datastoreSpaceUsage.datastoreType);
            return null;
         }
      }))).entrySet().stream().filter((spaceUsage) -> {
         return spaceUsage.getKey() == DatastoreType.VSAN || ((CapacityData)spaceUsage.getValue()).usedSpace > 0L;
      }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
   }

   private CapacityData calculateVsanSpaceUsage(ManagedObjectReference clusterRef, VsanSpaceUsage spaceUsage, DataEfficiencyCapacityState dataEfficiencyCapacity, SpaceEfficiencyConfig spaceEfficiencyConfig, CapacityReservationConfig capacityReservationConfig, boolean summaryDataOnly, boolean simulateCapacityEnforce) {
      CapacityOverviewCalculator capacityOverviewCalculator = new CapacityOverviewCalculator(spaceUsage, dataEfficiencyCapacity, spaceEfficiencyConfig);
      CapacityData capacityData = capacityOverviewCalculator.calculate();
      capacityData.reducedCapacityMessages = this.capacityMessagesService.getReducedCapacityMessages(clusterRef, spaceUsage != null ? spaceUsage.spaceDetail : null, true).reducedCapacityMessages;
      if (spaceUsage.spaceDetail == null) {
         return capacityData;
      } else {
         VsanCapacityBreakdownCalculator breakdownCapacityCalculator = new VsanCapacityBreakdownCalculator(spaceUsage, spaceEfficiencyConfig, capacityReservationConfig, capacityData, simulateCapacityEnforce);
         this.populateSlackSpaceUsage(breakdownCapacityCalculator, capacityData);
         this.populateOperationsUsage(breakdownCapacityCalculator, capacityData);
         if (summaryDataOnly) {
            return capacityData;
         } else {
            this.populateBreakdownUsage(breakdownCapacityCalculator, capacityData);
            return capacityData;
         }
      }
   }

   private void populateSlackSpaceUsage(VsanCapacityBreakdownCalculator breakdownCapacityCalculator, CapacityData capacityData) {
      capacityData.slackSpaceCapacityData = breakdownCapacityCalculator.getSlackSpaceCapacity();
      capacityData.thresholds = breakdownCapacityCalculator.getCapacityThresholds();
      if (capacityData.slackSpaceCapacityData.rebuildToleranceReservationAdjusted > 0L) {
         capacityData.freeSpace -= capacityData.slackSpaceCapacityData.rebuildToleranceReservationAdjusted;
      }

      if (capacityData.slackSpaceCapacityData.operationSpaceReservationAdjusted > 0L) {
         capacityData.freeSpace -= capacityData.slackSpaceCapacityData.operationSpaceReservationAdjusted;
      }

      if (capacityData.freeSpace < 0L) {
         capacityData.freeSpace = 0L;
      }

   }

   private void populateOperationsUsage(VsanCapacityBreakdownCalculator breakdownCapacityCalculator, CapacityData capacityData) {
      capacityData.transientSpace = breakdownCapacityCalculator.getTransientCapacityUsage();
      if (capacityData.transientSpace > 0L) {
         capacityData.actuallyWrittenSpace -= capacityData.transientSpace;
         if (capacityData.actuallyWrittenSpace < 0L) {
            capacityData.actuallyWrittenSpace = 0L;
         }
      }

   }

   private CapacityData calculateVsanDirectUsage(VsanSpaceUsage spaceUsage, boolean summaryDataOnly) {
      CapacityOverviewCalculator capacityOverviewCalculator = new CapacityOverviewCalculator(spaceUsage);
      CapacityData capacityData = capacityOverviewCalculator.calculate();
      if (spaceUsage.spaceDetail == null) {
         return capacityData;
      } else {
         VmfsCapacityBreakdownCalculator breakdownCapacityCalculator = new VmfsCapacityBreakdownCalculator(spaceUsage);
         capacityData.thresholds = breakdownCapacityCalculator.getCapacityThresholds();
         if (summaryDataOnly) {
            return capacityData;
         } else {
            this.populateBreakdownUsage(breakdownCapacityCalculator, capacityData);
            return capacityData;
         }
      }
   }

   private CapacityData calculatePmemUsage(VsanSpaceUsage spaceUsage, boolean summaryDataOnly) {
      CapacityOverviewCalculator capacityOverviewCalculator = new CapacityOverviewCalculator(spaceUsage);
      CapacityData capacityData = capacityOverviewCalculator.calculate();
      if (spaceUsage.spaceDetail == null) {
         return capacityData;
      } else {
         PMemCapacityBreakdownCalculator breakdownCapacityCalculator = new PMemCapacityBreakdownCalculator(spaceUsage);
         capacityData.thresholds = breakdownCapacityCalculator.getCapacityThresholds();
         if (summaryDataOnly) {
            return capacityData;
         } else {
            this.populateBreakdownUsage(breakdownCapacityCalculator, capacityData);
            return capacityData;
         }
      }
   }

   private void populateBreakdownUsage(CapacityBreakdownCalculator breakdownCapacityCalculator, CapacityData capacityData) {
      capacityData.vmCapacity = breakdownCapacityCalculator.calculateVmCapacityData();
      capacityData.userObjectsCapacity = breakdownCapacityCalculator.calculateUserObjectsCapacityData();
      capacityData.systemUsageCapacity = breakdownCapacityCalculator.calculateSystemUsageCapacityData();
   }

   @TsService
   public List getMountedDatastoresSpaceUsage(ManagedObjectReference clusterRef) {
      List mountedRemoteDatastores = (List)this.csdService.getMountedDatastores(clusterRef).stream().filter((ds) -> {
         return ds.shareableDatastore.serverCluster != null;
      }).collect(Collectors.toList());
      Iterator var3 = mountedRemoteDatastores.iterator();

      while(var3.hasNext()) {
         MountedRemoteDatastore datastore = (MountedRemoteDatastore)var3.next();

         try {
            ReducedCapacityData reducedCapacityData = this.capacityMessagesService.getReducedCapacityMessages(datastore.shareableDatastore.serverCluster.moRef, (VsanSpaceUsageDetailResult)null, false);
            CapacityData datastoreCapacity;
            if (reducedCapacityData.hasHealthyHosts) {
               datastoreCapacity = (CapacityData)this.getSpaceUsage(datastore.shareableDatastore.datastore.moRef, true).get(DatastoreType.VSAN);
            } else {
               datastoreCapacity = new CapacityData();
               datastoreCapacity.reducedCapacityMessages = reducedCapacityData.reducedCapacityMessages;
            }

            datastore.shareableDatastore.setCapacityData(datastoreCapacity);
         } catch (Exception var7) {
            logger.warn("Unable to get capacity data for datastore: " + datastore, var7);
         }
      }

      return mountedRemoteDatastores;
   }

   @TsService
   public WhatifCapacityData getWhatIfCapacity(ManagedObjectReference objectRef, String profileId) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objectRef);
      Validate.notNull(clusterRef);
      if (!VsanCapabilityUtils.isWhatIfCapacitySupported(clusterRef)) {
         logger.error("Unable to get what-if capacity for cluster that doesn't support it!");
         throw new VsanUiLocalizableException("vsan.common.generic.error");
      } else {
         DefinedProfileSpec profileSpec = new DefinedProfileSpec();
         profileSpec.profileId = profileId;
         ProfileSpec[] profiles = new ProfileSpec[]{profileSpec};
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var8 = null;

         VsanSpaceUsage spaceUsage;
         try {
            VsanSpaceReportSystem capacitySystem = conn.getVsanSpaceReportSystem();

            try {
               Measure m = new Measure("capacitySystem.querySpaceUsage");
               Throwable var11 = null;

               try {
                  spaceUsage = capacitySystem.querySpaceUsage(clusterRef, profiles, false);
               } catch (Throwable var36) {
                  var11 = var36;
                  throw var36;
               } finally {
                  if (m != null) {
                     if (var11 != null) {
                        try {
                           m.close();
                        } catch (Throwable var35) {
                           var11.addSuppressed(var35);
                        }
                     } else {
                        m.close();
                     }
                  }

               }
            } catch (Exception var38) {
               logger.error("Unable to get what-if capacity: " + var38);
               throw new VsanUiLocalizableException("vsan.common.generic.error");
            }
         } catch (Throwable var39) {
            var8 = var39;
            throw var39;
         } finally {
            if (conn != null) {
               if (var8 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var34) {
                     var8.addSuppressed(var34);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return WhatifCapacityData.fromSpaceUsage(spaceUsage);
      }
   }

   @TsService
   public List getStorageConsumptionByHost(ManagedObjectReference clusterRef) {
      boolean isHostReservedCapacitySupported = VsanCapabilityUtils.isHostReservedCapacitySupportedOnVc(clusterRef);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      Future clusterLimitHealthFuture;
      try {
         VsanVcClusterHealthSystem clusterHealthSystem = conn.getVsanVcClusterHealthSystem();
         Measure measure = new Measure("Calculating storage consumption by host");
         Throwable var8 = null;

         try {
            clusterLimitHealthFuture = measure.newFuture("VsanVcClusterHealthSystem.queryCheckLimits");
            clusterHealthSystem.queryCheckLimits(clusterRef, clusterLimitHealthFuture);
         } catch (Throwable var34) {
            var8 = var34;
            throw var34;
         } finally {
            if (measure != null) {
               if (var8 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var32) {
                     var8.addSuppressed(var32);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Throwable var36) {
         var5 = var36;
         throw var36;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var31) {
                  var5.addSuppressed(var31);
               }
            } else {
               conn.close();
            }
         }

      }

      VsanClusterLimitHealthResult clusterLimitHealthResult;
      try {
         clusterLimitHealthResult = (VsanClusterLimitHealthResult)clusterLimitHealthFuture.get();
      } catch (Exception var33) {
         logger.error("Unable to get cluster limit health result for cluster ref " + clusterRef);
         throw new VsanUiLocalizableException(var33);
      }

      List hostStorageConsumptionData = new ArrayList();
      if (ArrayUtils.isEmpty(clusterLimitHealthResult.hostResults)) {
         return hostStorageConsumptionData;
      } else {
         Map hostNameAndMoRefMap = this.getHostNameAndMoRefMap(clusterRef);
         VsanLimitHealthResult[] var41 = clusterLimitHealthResult.hostResults;
         int var42 = var41.length;

         for(int var9 = 0; var9 < var42; ++var9) {
            VsanLimitHealthResult hostResult = var41[var9];
            HostStorageConsumptionData hostData = new HostStorageConsumptionData();
            hostData.totalCapacity = hostResult.totalDiskSpaceB;
            hostData.userCapacity = hostResult.usedDiskSpaceB;
            if (isHostReservedCapacitySupported && hostResult.cdReservedSizeB != null) {
               hostData.reservedCapacity = hostResult.cdReservedSizeB;
            }

            hostData.hostRef = (ManagedObjectReference)hostNameAndMoRefMap.get(hostResult.hostname);
            hostStorageConsumptionData.add(hostData);
         }

         return hostStorageConsumptionData;
      }
   }

   private Map getHostNameAndMoRefMap(ManagedObjectReference clusterRef) {
      HashMap result = new HashMap();

      try {
         DataServiceResponse response = QueryUtil.getPropertyForRelatedObjects(clusterRef, "host", ClusterComputeResource.class.getSimpleName(), "name");
         PropertyValue[] var4 = response.getPropertyValues();
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            PropertyValue property = var4[var6];
            result.put((String)property.value, (ManagedObjectReference)property.resourceObject);
         }
      } catch (Exception var8) {
         logger.error("Unable to get host data for cluster " + clusterRef, var8);
      }

      return result;
   }
}
