package com.vmware.vsan.client.services.evacuationstatus;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.Datastore.HostMount;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.host.MaintenanceSpec;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DecommissionMode;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vim.vsan.host.DecommissionMode.ObjectAction;
import com.vmware.vim.binding.vmodl.LocalizableMessage;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.InvalidArgument;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.DiskDataEvacuationResourceCheckTaskDetails;
import com.vmware.vim.vsan.binding.vim.vsan.DiskGroupResourceCheckResult;
import com.vmware.vim.vsan.binding.vim.vsan.DiskResourceCheckResult;
import com.vmware.vim.vsan.binding.vim.vsan.DurabilityResult;
import com.vmware.vim.vsan.binding.vim.vsan.FaultDomainResourceCheckResult;
import com.vmware.vim.vsan.binding.vim.vsan.HostResourceCheckResult;
import com.vmware.vim.vsan.binding.vim.vsan.ResourceCheckDataPersistenceResult;
import com.vmware.vim.vsan.binding.vim.vsan.ResourceCheckResult;
import com.vmware.vim.vsan.binding.vim.vsan.ResourceCheckSpec;
import com.vmware.vim.vsan.binding.vim.vsan.ResourceCheckStatus;
import com.vmware.vim.vsan.binding.vim.vsan.ResourceCheckStatusType;
import com.vmware.vim.vsan.binding.vim.vsan.ResourceCheckTaskDetails;
import com.vmware.vim.vsan.binding.vim.vsan.ResourceCheckVsanResult;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthThreshold;
import com.vmware.vim.vsan.binding.vim.vsan.VsanResourceCheckSystem;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.TaskService;
import com.vmware.vsan.client.services.common.data.TaskInfoData;
import com.vmware.vsan.client.services.config.SpaceEfficiencyConfig;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.services.diskGroups.data.VsanDiskMapping;
import com.vmware.vsan.client.services.diskmanagement.DiskData;
import com.vmware.vsan.client.services.diskmanagement.DiskGroupData;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementUtil;
import com.vmware.vsan.client.services.evacuationstatus.model.ClusterEvacuationCapacityData;
import com.vmware.vsan.client.services.evacuationstatus.model.DataMigrationDiskGroupData;
import com.vmware.vsan.client.services.evacuationstatus.model.EvacuationEntity;
import com.vmware.vsan.client.services.evacuationstatus.model.EvacuationEntityType;
import com.vmware.vsan.client.services.evacuationstatus.model.EvacuationReport;
import com.vmware.vsan.client.services.evacuationstatus.model.EvacuationStatusData;
import com.vmware.vsan.client.services.evacuationstatus.model.EvacuationTaskData;
import com.vmware.vsan.client.services.evacuationstatus.model.FaultDomainEvacuationCapacityData;
import com.vmware.vsan.client.services.evacuationstatus.model.HostEvacuationCapacityData;
import com.vmware.vsan.client.services.evacuationstatus.model.PrecheckPersistenceData;
import com.vmware.vsan.client.services.evacuationstatus.model.PrecheckPersistentInstanceData;
import com.vmware.vsan.client.services.evacuationstatus.model.PrecheckPersistentInstanceState;
import com.vmware.vsan.client.services.evacuationstatus.model.PrecheckResultStatusType;
import com.vmware.vsan.client.services.evacuationstatus.model.PrecheckTaskType;
import com.vmware.vsan.client.services.evacuationstatus.model.ResourceCheckOperation;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsService;
import com.vmware.vsan.client.services.virtualobjects.data.DurabilityState;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.NumberUtils;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutorResult;
import com.vmware.vsan.client.util.dataservice.query.QueryModel;
import com.vmware.vsan.client.util.dataservice.query.QueryResult;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.data.VsanDiskAndGroupData;
import com.vmware.vsphere.client.vsan.data.VsanDiskData;
import com.vmware.vsphere.client.vsan.data.VsanDiskGroupData;
import com.vmware.vsphere.client.vsan.health.util.VsanHealthUtil;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import com.vmware.vsphere.client.vsan.whatif.VsanWhatIfComplianceStatus;
import com.vmware.vsphere.client.vsan.whatif.WhatIfPropertyProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class EvacuationStatusService {
   private static final Log logger = LogFactory.getLog(EvacuationStatusService.class);
   private static final List WARNING_MASSAGES_FILTER_KEYS = new ArrayList() {
      {
         this.add("com.vmware.vsan.whatif.compliance.hostinmaintenancemode");
      }
   };
   private static final String REMOVE_DISK_TASK_ID = "com.vmware.vsan.diskmgmt.tasks.removediskex";
   private static final String REMOVE_CAPACITY_DISK_TASK_ID = "com.vmware.vsan.diskmgmt.tasks.removecapacitydiskex";
   private static final String REMOVE_DISKGROUP_TASK_ID = "com.vmware.vsan.diskmgmt.tasks.removediskmappingex";
   private static final String RECREATE_DISKGROUP_TASK_ID = "com.vmware.vsan.diskmgmt.tasks.rebuilddiskmapping";
   private static final String UNMOUNT_DISKGROUP_TASK_ID = "com.vmware.vsan.diskmgmt.tasks.unmountdiskmappingex";
   private static final String VSAN_DIRECT_DATASTORE_REQUEST_NAME = "vsan-direct-datastore-properties";
   private static final String VSAN_DIRECT_HOST_REQUEST_NAME = "host-properties";
   @Autowired
   private VsanConfigService vsanConfigSystem;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VcClient vcClient;
   @Autowired
   private TaskService taskService;
   @Autowired
   private VirtualObjectsService virtualObjectsService;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private WhatIfPropertyProvider whatIfPropertyProvider;
   @Autowired
   private QueryExecutor queryExecutor;

   @TsService
   public EvacuationStatusData getEvacuationStatus(ManagedObjectReference clusterRef) {
      EvacuationStatusData evacuationStatusData = new EvacuationStatusData();
      evacuationStatusData.isHostResourcePrecheckSupported = VsanCapabilityUtils.isHostResourcePrecheckSupported(clusterRef);
      evacuationStatusData.isDiskResourcePrecheckSupported = VsanCapabilityUtils.isDiskResourcePrecheckSupported(clusterRef);
      if (evacuationStatusData.isDiskResourcePrecheckSupported) {
         ConfigInfoEx config = this.vsanConfigSystem.getConfigInfoEx(clusterRef);
         evacuationStatusData.spaceEfficiencyConfig = SpaceEfficiencyConfig.fromVmodl(config.dataEfficiencyConfig);
      }

      if (!evacuationStatusData.isHostResourcePrecheckSupported) {
         return evacuationStatusData;
      } else {
         evacuationStatusData.evacuationEntities = this.getHostEvacuationEntities(clusterRef);
         if (ArrayUtils.isEmpty(evacuationStatusData.evacuationEntities)) {
            logger.warn("No evacuation entities found for cluster: " + clusterRef);
            return evacuationStatusData;
         } else {
            if (evacuationStatusData.isDiskResourcePrecheckSupported) {
               this.appendDiskAndGroupEntitiesToHost(evacuationStatusData);
            }

            return evacuationStatusData;
         }
      }
   }

   @TsService
   public EvacuationReport getEvacuationReport(ManagedObjectReference clusterRef, EvacuationEntity evacuationEntity, String decommissionMode, ResourceCheckOperation operation) {
      ResourceCheckSpec resourceCheckSpec = evacuationEntity == null ? null : this.getResourceCheckSpec(decommissionMode, evacuationEntity.uuid, operation, clusterRef);

      try {
         VsanConnection vsanConnection = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var8 = null;

         EvacuationReport var44;
         try {
            VsanResourceCheckSystem vsanResourceCheckSystem = vsanConnection.getVsanResourceCheckSystem();
            Measure measure = new Measure("vsanResourceCheckSystem.getResourceCheckStatus");
            Throwable var11 = null;

            ResourceCheckStatus resourceCheckStatus;
            try {
               resourceCheckStatus = vsanResourceCheckSystem.getResourceCheckStatus(resourceCheckSpec, clusterRef);
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

            var44 = this.parseResourceCheckStatusToEvacuationReport(resourceCheckStatus, evacuationEntity, clusterRef, decommissionMode, operation);
         } catch (Throwable var40) {
            var8 = var40;
            throw var40;
         } finally {
            if (vsanConnection != null) {
               if (var8 != null) {
                  try {
                     vsanConnection.close();
                  } catch (Throwable var36) {
                     var8.addSuppressed(var36);
                  }
               } else {
                  vsanConnection.close();
               }
            }

         }

         return var44;
      } catch (InvalidArgument var42) {
         throw new VsanUiLocalizableException("vsan.evacuationStatus.getResourceCheckStatusFailed.invalidArgument", "Gathering evacuation status report failed for cluster " + clusterRef, var42, new Object[0]);
      } catch (Exception var43) {
         throw new VsanUiLocalizableException("vsan.evacuationStatus.getResourceCheckStatusFailed", "Gathering evacuation status report failed for cluster " + clusterRef, var43, new Object[0]);
      }
   }

   @TsService
   public ManagedObjectReference runDataMigrationPrecheck(ManagedObjectReference clusterRef, String uuid, String decommissionMode, ResourceCheckOperation operation) {
      ResourceCheckSpec resourceCheckSpec = this.getResourceCheckSpec(decommissionMode, uuid, operation, clusterRef);

      try {
         VsanConnection vsanConnection = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var7 = null;

         ManagedObjectReference var12;
         try {
            VsanResourceCheckSystem vsanResourceCheckSystem = vsanConnection.getVsanResourceCheckSystem();
            Measure measure = new Measure("vsanResourceCheckSystem.performResourceCheck");
            Throwable var10 = null;

            try {
               ManagedObjectReference task = vsanResourceCheckSystem.performResourceCheck(resourceCheckSpec, clusterRef);
               var12 = VmodlHelper.assignServerGuid(task, clusterRef.getServerGuid());
            } catch (Throwable var37) {
               var10 = var37;
               throw var37;
            } finally {
               if (measure != null) {
                  if (var10 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var36) {
                        var10.addSuppressed(var36);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var39) {
            var7 = var39;
            throw var39;
         } finally {
            if (vsanConnection != null) {
               if (var7 != null) {
                  try {
                     vsanConnection.close();
                  } catch (Throwable var35) {
                     var7.addSuppressed(var35);
                  }
               } else {
                  vsanConnection.close();
               }
            }

         }

         return var12;
      } catch (Exception var41) {
         throw new VsanUiLocalizableException("vsan.evacuationStatus.resourceCheckFailed", "Running evacuation status report failed for cluster " + clusterRef, var41, new Object[0]);
      }
   }

   public List getVirtualObjectsList(ManagedObjectReference clusterRef, String[] inaccessibleObjects, String[] nonCompliantObjects, boolean isDurabilityPossible, ResourceCheckResult resourceCheckResult) {
      ArrayList virtualObjects = new ArrayList();

      try {
         List vsanObjects = this.virtualObjectsService.listVirtualObjects(clusterRef);
         if (ArrayUtils.isNotEmpty(inaccessibleObjects)) {
            virtualObjects.addAll(this.whatIfPropertyProvider.getVsanObjects(inaccessibleObjects, vsanObjects, VsanWhatIfComplianceStatus.INACCESSIBLE));
         }

         if (ArrayUtils.isNotEmpty(nonCompliantObjects)) {
            virtualObjects.addAll(this.whatIfPropertyProvider.getVsanObjects(nonCompliantObjects, vsanObjects, VsanWhatIfComplianceStatus.NOT_COMPLIANT));
         }

         if (isDurabilityPossible) {
            this.initializeComponentsDurability(virtualObjects, resourceCheckResult.durabilityResult);
         }

         return virtualObjects;
      } catch (Exception var8) {
         throw new VsanUiLocalizableException("vsan.evacuationStatus.listVirtualObjectsFailed", "Failed to list virtual objects for cluster " + clusterRef, var8, new Object[0]);
      }
   }

   private void initializeComponentsDurability(List virtualObjects, DurabilityResult durabilityResult) {
      if (!CollectionUtils.isEmpty(virtualObjects)) {
         if (durabilityResult == null) {
            logger.error("Missing durability result. All components durability will be set to " + DurabilityState.UNKNOWN);
            this.setComponentsDurabilityState(virtualObjects, DurabilityState.UNKNOWN);
         } else if (durabilityResult.isDurable) {
            this.setComponentsDurabilityState(virtualObjects, DurabilityState.GUARANTEED);
         } else {
            this.setComponentsDurability(virtualObjects, durabilityResult);
         }
      }
   }

   private void setComponentsDurability(List virtualObjects, DurabilityResult durabilityResult) {
      virtualObjects.forEach((obj) -> {
         if (ArrayUtils.isNotEmpty(obj.children)) {
            this.setComponentsDurability(Arrays.asList(obj.children), durabilityResult);
         }

         if (StringUtils.isNotBlank(obj.uid)) {
            obj.durabilityState = this.extractDurabilityState(durabilityResult, obj.uid);
         }

      });
   }

   private DurabilityState extractDurabilityState(DurabilityResult durabilityResult, String objUid) {
      if (ArrayUtils.isNotEmpty(durabilityResult.exceedCompLimit) && ArrayUtils.indexOf(durabilityResult.exceedCompLimit, objUid) > -1) {
         return DurabilityState.EXCEEDED_COMP_LIMIT;
      } else if (ArrayUtils.isNotEmpty(durabilityResult.noSpace) && ArrayUtils.indexOf(durabilityResult.noSpace, objUid) > -1) {
         return DurabilityState.NO_SPACE;
      } else if (ArrayUtils.isNotEmpty(durabilityResult.noResource) && ArrayUtils.indexOf(durabilityResult.noResource, objUid) > -1) {
         return DurabilityState.NO_RESOURCE;
      } else {
         return ArrayUtils.isNotEmpty(durabilityResult.staleDurabilityComp) && ArrayUtils.indexOf(durabilityResult.staleDurabilityComp, objUid) > -1 ? DurabilityState.STALE_DURABILITY_COMP : DurabilityState.GUARANTEED;
      }
   }

   private void setComponentsDurabilityState(List virtualObjects, DurabilityState state) {
      virtualObjects.forEach((obj) -> {
         if (StringUtils.isNotBlank(obj.uid)) {
            obj.durabilityState = state;
         }

         if (ArrayUtils.isNotEmpty(obj.children)) {
            this.setComponentsDurabilityState(Arrays.asList(obj.children), state);
         }

      });
   }

   @TsService
   public ManagedObjectReference runEnterMaintenanceMode(ManagedObjectReference hostRef, String decommissionModeValue, boolean movePoweredOffVms) {
      try {
         VcConnection vcConnection = this.vcClient.getVsanVmodlVersionConnection(hostRef.getServerGuid());
         Throwable var5 = null;

         ManagedObjectReference var12;
         try {
            DecommissionMode decommissionMode = new DecommissionMode(decommissionModeValue);
            MaintenanceSpec maintenanceSpec = new MaintenanceSpec();
            maintenanceSpec.setVsanMode(decommissionMode);
            HostSystem hostSystem = (HostSystem)vcConnection.createStub(HostSystem.class, hostRef);
            Measure measure = new Measure("hostSystem.enterMaintenanceMode");
            Throwable var10 = null;

            try {
               ManagedObjectReference task = hostSystem.enterMaintenanceMode(0, movePoweredOffVms, maintenanceSpec);
               var12 = VmodlHelper.assignServerGuid(task, hostRef.getServerGuid());
            } catch (Throwable var37) {
               var10 = var37;
               throw var37;
            } finally {
               if (measure != null) {
                  if (var10 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var36) {
                        var10.addSuppressed(var36);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var39) {
            var5 = var39;
            throw var39;
         } finally {
            if (vcConnection != null) {
               if (var5 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var35) {
                     var5.addSuppressed(var35);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return var12;
      } catch (Exception var41) {
         throw new VsanUiLocalizableException("vsan.evacuationStatus.eMMFailed", "Entering maintenance mode failed for host " + hostRef, var41, new Object[0]);
      }
   }

   private EvacuationEntity[] getHostEvacuationEntities(ManagedObjectReference clusterRef) {
      ArrayList evacuationEntities = new ArrayList();

      try {
         Map hostResponse = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), new String[]{"name", "config.vsanHostConfig.enabled", "primaryIconId", "config.vsanHostConfig.clusterInfo.nodeUuid", "runtime.connectionState", "runtime.inMaintenanceMode"}).getMap();
         if (CollectionUtils.isEmpty(hostResponse)) {
            return (EvacuationEntity[])evacuationEntities.toArray(new EvacuationEntity[0]);
         }

         Iterator var4 = hostResponse.entrySet().iterator();

         while(var4.hasNext()) {
            Entry mapEntry = (Entry)var4.next();
            Map hostProperties = (Map)mapEntry.getValue();
            Boolean vsanEnabledOnHost = (Boolean)hostProperties.get("config.vsanHostConfig.enabled");
            if (!BooleanUtils.isFalse(vsanEnabledOnHost)) {
               ManagedObjectReference hostRef = (ManagedObjectReference)mapEntry.getKey();
               evacuationEntities.add(this.createHostEvacuationEntity(hostRef, hostProperties));
            }
         }
      } catch (IllegalStateException var9) {
         logger.warn("Cannot retrieve evacuation entities for cluster: " + clusterRef, var9);
      } catch (Exception var10) {
         throw new VsanUiLocalizableException("vsan.evacuationStatus.getEvacuationStatusFailed", "Error encountered while retrieving evacuation entities for cluster: " + clusterRef, var10, new Object[0]);
      }

      return (EvacuationEntity[])evacuationEntities.toArray(new EvacuationEntity[0]);
   }

   private EvacuationEntity createHostEvacuationEntity(ManagedObjectReference hostRef, Map hostProperties) {
      EvacuationEntity evacuationEntity = new EvacuationEntity();
      evacuationEntity.type = EvacuationEntityType.HOST;
      evacuationEntity.hostRef = hostRef;
      evacuationEntity.name = (String)hostProperties.get("name");
      evacuationEntity.primaryIconId = (String)hostProperties.get("primaryIconId");
      evacuationEntity.uuid = (String)hostProperties.get("config.vsanHostConfig.clusterInfo.nodeUuid");
      ConnectionState connectionState = (ConnectionState)hostProperties.get("runtime.connectionState");
      evacuationEntity.isHostConnected = ConnectionState.connected.equals(connectionState);
      evacuationEntity.isInMaintenanceMode = evacuationEntity.isHostConnected && (Boolean)hostProperties.get("runtime.inMaintenanceMode");
      return evacuationEntity;
   }

   private void appendDiskAndGroupEntitiesToHost(EvacuationStatusData evacuationStatusData) {
      Map hostEntities = new HashMap();
      EvacuationEntity[] var3 = evacuationStatusData.evacuationEntities;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         EvacuationEntity entity = var3[var5];
         if (entity.isHostConnected) {
            hostEntities.put(entity.hostRef, entity);
         }
      }

      try {
         Measure measure = new Measure("Collect disks health information.");
         Throwable var30 = null;

         try {
            DataServiceResponse response = QueryUtil.getProperties((ManagedObjectReference[])hostEntities.keySet().toArray(new ManagedObjectReference[0]), new String[]{"vsanDisksAndGroupsData"});
            PropertyValue[] propertyValues = response.getPropertyValues();
            PropertyValue[] var7 = propertyValues;
            int var8 = propertyValues.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               PropertyValue prop = var7[var9];
               VsanDiskAndGroupData diskAndGroupData = (VsanDiskAndGroupData)prop.value;
               if (diskAndGroupData != null && !ArrayUtils.isEmpty(diskAndGroupData.vsanGroups)) {
                  ManagedObjectReference hostRef = (ManagedObjectReference)prop.resourceObject;
                  EvacuationEntity[] diskGroupEntities = this.getDiskGroupEvacuationEntities((EvacuationEntity)hostEntities.get(hostRef), diskAndGroupData);
                  EvacuationEntity[] var14 = evacuationStatusData.evacuationEntities;
                  int var15 = var14.length;

                  for(int var16 = 0; var16 < var15; ++var16) {
                     EvacuationEntity hostEntity = var14[var16];
                     if (hostEntity.hostRef.getValue().equalsIgnoreCase(hostRef.getValue())) {
                        hostEntity.children = diskGroupEntities;
                     }
                  }
               }
            }
         } catch (Throwable var26) {
            var30 = var26;
            throw var26;
         } finally {
            if (measure != null) {
               if (var30 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var25) {
                     var30.addSuppressed(var25);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Exception var28) {
         evacuationStatusData.errorMessage = Utils.getLocalizedString("vsan.evacuationStatus.getHostsEvacuationStatusFailed");
         logger.error("Error encountered while retrieving disk groups and disks for hosts. ", var28);
      }

   }

   private EvacuationEntity[] getDiskGroupEvacuationEntities(EvacuationEntity hostEntity, VsanDiskAndGroupData diskAndGroupData) {
      List groups = new ArrayList();
      VsanDiskGroupData[] var4 = diskAndGroupData.vsanGroups;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         VsanDiskGroupData group = var4[var6];
         List disks = new ArrayList();
         VsanDiskData[] var9 = group.disks;
         int var10 = var9.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            VsanDiskData disk = var9[var11];
            EvacuationEntity capacityDisk = this.createDiskEvacuationEntity(hostEntity, disk, group.ssd.vsanUuid);
            disks.add(capacityDisk);
         }

         EvacuationEntity diskGroup = this.createDiskgroupEvacuationEntity(hostEntity, group, disks);
         groups.add(diskGroup);
      }

      return (EvacuationEntity[])groups.toArray(new EvacuationEntity[0]);
   }

   private EvacuationEntity createDiskgroupEvacuationEntity(EvacuationEntity hostEntity, VsanDiskGroupData group, List disks) {
      EvacuationEntity diskGroupEntity = new EvacuationEntity();
      diskGroupEntity.hostRef = hostEntity.hostRef;
      diskGroupEntity.uuid = group.ssd.vsanUuid;
      diskGroupEntity.type = EvacuationEntityType.DISK_GROUP;
      diskGroupEntity.children = (EvacuationEntity[])disks.toArray(new EvacuationEntity[0]);
      diskGroupEntity.name = Utils.getLocalizedString("vsan.evacuationStatus.formattedDiskGroupName", group.ssd.vsanUuid);
      diskGroupEntity.isHostConnected = hostEntity.isHostConnected;
      diskGroupEntity.isInMaintenanceMode = hostEntity.isInMaintenanceMode;
      List allDisks = new ArrayList();
      DataMigrationDiskGroupData diskGroupData = new DataMigrationDiskGroupData();
      diskGroupData.isLocked = group.encrypted && !group.unlockedEncrypted;
      diskGroupData.isMounted = group.mounted;
      diskGroupData.disksStatuses = new HashMap();
      diskGroupData.disksStatuses.put(group.ssd.vsanUuid, group.ssd.diskStatus);
      VsanDiskData[] var7 = group.disks;
      int var8 = var7.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         VsanDiskData capacityDisk = var7[var9];
         diskGroupData.disksStatuses.put(capacityDisk.vsanUuid, capacityDisk.diskStatus);
      }

      diskGroupData.diskMapping = new VsanDiskMapping();
      String groupVsanUuid = group.ssd.vsanUuid;
      diskGroupData.diskMapping.ssd = group.ssd.disk;
      allDisks.add(DiskData.fromVsanDiskData(group.ssd, groupVsanUuid));
      List capacityDisks = new ArrayList();
      VsanDiskData[] var15 = group.disks;
      int var16 = var15.length;

      for(int var11 = 0; var11 < var16; ++var11) {
         VsanDiskData disk = var15[var11];
         capacityDisks.add(disk.disk);
         allDisks.add(DiskData.fromVsanDiskData(disk, groupVsanUuid));
      }

      diskGroupData.diskMapping.nonSsd = (ScsiDisk[])capacityDisks.toArray(new ScsiDisk[0]);
      diskGroupEntity.diskGroupData = diskGroupData;
      diskGroupEntity.primaryIconId = DiskGroupData.getDiskGroupIcon(diskGroupData.isLocked, diskGroupData.isMounted, (DiskData[])allDisks.toArray(new DiskData[0]));
      return diskGroupEntity;
   }

   private EvacuationEntity createDiskEvacuationEntity(EvacuationEntity hostEntity, VsanDiskData disk, String groupVsanUuid) {
      EvacuationEntity diskEntity = new EvacuationEntity();
      diskEntity.hostRef = hostEntity.hostRef;
      diskEntity.uuid = disk.vsanUuid;
      diskEntity.name = DiskManagementUtil.getDiskName(disk.disk);
      diskEntity.type = EvacuationEntityType.CAPACITY_DISK;
      diskEntity.isHostConnected = hostEntity.isHostConnected;
      diskEntity.isInMaintenanceMode = hostEntity.isInMaintenanceMode;
      diskEntity.diskData = DiskData.fromVsanDiskData(disk, groupVsanUuid);
      diskEntity.primaryIconId = DiskData.getDiskIcon(diskEntity.diskData.isHealthy(), diskEntity.diskData.isFlash);
      return diskEntity;
   }

   private ResourceCheckSpec getResourceCheckSpec(String decommissionModeValue, String uuid, ResourceCheckOperation operation, ManagedObjectReference clusterRef) {
      ResourceCheckSpec resourceCheckSpec = new ResourceCheckSpec();
      resourceCheckSpec.entities = new String[]{uuid};
      resourceCheckSpec.operation = operation.value;
      DecommissionMode decommissionMode = new DecommissionMode(decommissionModeValue);
      if (VsanCapabilityUtils.isEnsureDurabilitySupported(clusterRef) && ResourceCheckOperation.ENTER_MAINTENANCE_MODE.equals(operation) && ObjectAction.ensureObjectAccessibility.toString().equals(decommissionModeValue)) {
         decommissionMode = new DecommissionMode(ObjectAction.ensureEnhancedDurability.toString());
      }

      resourceCheckSpec.maintenanceSpec = new MaintenanceSpec();
      resourceCheckSpec.maintenanceSpec.setVsanMode(decommissionMode);
      return resourceCheckSpec;
   }

   private EvacuationReport parseResourceCheckStatusToEvacuationReport(ResourceCheckStatus resourceCheckStatus, EvacuationEntity evacuationEntity, ManagedObjectReference clusterRef, String decommissionMode, ResourceCheckOperation operation) throws Exception {
      EvacuationReport evacuationReport = new EvacuationReport();
      evacuationReport.uuid = evacuationEntity != null ? evacuationEntity.uuid : null;
      evacuationReport.decommissionMode = decommissionMode;
      if (resourceCheckStatus != null && (resourceCheckStatus.result != null || resourceCheckStatus.task != null)) {
         if (resourceCheckStatus.task != null) {
            if (resourceCheckStatus.task instanceof DiskDataEvacuationResourceCheckTaskDetails) {
               evacuationReport.runningTask = this.getRunningTaskData((DiskDataEvacuationResourceCheckTaskDetails)resourceCheckStatus.task, resourceCheckStatus.parentTask, clusterRef);
            } else {
               evacuationReport.runningTask = this.getRunningTaskData(resourceCheckStatus.task, resourceCheckStatus.parentTask, clusterRef);
            }

            return evacuationReport;
         } else {
            boolean isResourceCheckCompleted = ResourceCheckStatusType.resourceCheckCompleted.name().equalsIgnoreCase(resourceCheckStatus.status);
            if (!isResourceCheckCompleted) {
               return evacuationReport;
            } else {
               evacuationReport.hasEvacuationReport = true;
               ResourceCheckResult result = resourceCheckStatus.result;
               this.assignReportStatus(evacuationReport, result.status);
               evacuationReport.dataToMove = result.dataToMove;
               evacuationReport.reportDate = result.timestamp.getTime();
               evacuationReport.messages = this.getReportMessages(result.messages);
               evacuationReport.inaccessibleObjects = result.inaccessibleObjects;
               evacuationReport.nonCompliantObjects = result.nonCompliantObjects;
               evacuationReport.clusterCapacity = this.parseCapacityReport(resourceCheckStatus, evacuationEntity.uuid, clusterRef);
               evacuationReport.healthSummary = VsanHealthUtil.getVsanHealthData(result.health, clusterRef, true, false);
               if (VsanCapabilityUtils.isEnsureDurabilitySupported(clusterRef) && ResourceCheckOperation.ENTER_MAINTENANCE_MODE.equals(operation) && ObjectAction.ensureObjectAccessibility.toString().equals(decommissionMode)) {
                  evacuationReport.isDurabilityPossible = true;
                  evacuationReport.isDurabilityGuaranteed = result.durabilityResult != null && result.durabilityResult.isDurable;
               }

               evacuationReport.virtualObjects = this.getVirtualObjectsList(clusterRef, evacuationReport.inaccessibleObjects, evacuationReport.nonCompliantObjects, evacuationReport.isDurabilityPossible, result);
               if (ArrayUtils.isNotEmpty(evacuationReport.nonCompliantObjects)) {
                  evacuationReport.clusterRepairTime = this.whatIfPropertyProvider.getClusterRepairTime(clusterRef);
               }

               this.assignPersistencePrecheckData(evacuationReport, resourceCheckStatus, clusterRef, evacuationEntity);
               return evacuationReport;
            }
         }
      } else {
         return evacuationReport;
      }
   }

   private void assignReportStatus(EvacuationReport evacuationReport, String resultStatus) {
      PrecheckResultStatusType precheckResultStatus = (PrecheckResultStatusType)EnumUtils.fromStringIgnoreCase(PrecheckResultStatusType.class, resultStatus);
      if (precheckResultStatus != null && precheckResultStatus.isMoreSevereThan(evacuationReport.status)) {
         evacuationReport.status = precheckResultStatus;
      }

   }

   private String[] getReportMessages(LocalizableMessage[] reportMessages) {
      if (ArrayUtils.isEmpty(reportMessages)) {
         return null;
      } else {
         List messages = new ArrayList();
         LocalizableMessage[] var3 = reportMessages;
         int var4 = reportMessages.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            LocalizableMessage localizableMessage = var3[var5];
            boolean addMessage = localizableMessage != null && localizableMessage.getMessage() != null && !WARNING_MASSAGES_FILTER_KEYS.contains(localizableMessage.getKey());
            if (addMessage) {
               messages.add(localizableMessage.getMessage());
            }
         }

         return (String[])messages.toArray(new String[0]);
      }
   }

   private void assignPersistencePrecheckData(EvacuationReport evacuationReport, ResourceCheckStatus resourceCheckStatus, ManagedObjectReference clusterRef, EvacuationEntity evacuationEntity) {
      if (VsanCapabilityUtils.isPersistenceResourceCheckSupportedOnVc(clusterRef) && !ArrayUtils.isEmpty(resourceCheckStatus.componentResults)) {
         ResourceCheckDataPersistenceResult persistenceResult = (ResourceCheckDataPersistenceResult)Arrays.stream(resourceCheckStatus.componentResults).filter((compResult) -> {
            return compResult instanceof ResourceCheckDataPersistenceResult;
         }).map((compResult) -> {
            return (ResourceCheckDataPersistenceResult)compResult;
         }).findFirst().orElse((Object)null);
         if (persistenceResult != null) {
            this.assignReportStatus(evacuationReport, persistenceResult.status);
            evacuationReport.persistenceData = new PrecheckPersistenceData();
            evacuationReport.persistenceData.dataToRebuild = persistenceResult.dataToRebuild;
            evacuationReport.persistenceData.persistentInstances = this.getPersistenceInstances(persistenceResult);
            evacuationReport.vsanDirectClusterCapacity = this.parseVsanDirectCapacity(clusterRef, evacuationEntity, persistenceResult.capacityThreshold, persistenceResult.status);
         }
      }
   }

   private List getPersistenceInstances(ResourceCheckDataPersistenceResult persistenceResult) {
      List persistentInstances = new ArrayList(this.getPersistentInstanceData(persistenceResult.inaccessibleInstances, PrecheckPersistentInstanceState.INACCESSIBLE));
      persistentInstances.addAll(this.getPersistentInstanceData(persistenceResult.reducedAvailabilityInstances, PrecheckPersistentInstanceState.REDUCED_AVAILABILITY));
      persistentInstances.addAll(this.getPersistentInstanceData(persistenceResult.rebuildInstances, PrecheckPersistentInstanceState.REBUILD));
      return persistentInstances;
   }

   private List getPersistentInstanceData(String[] instances, PrecheckPersistentInstanceState predictedState) {
      return (List)(ArrayUtils.isEmpty(instances) ? new ArrayList() : (List)Stream.of(instances).map((instance) -> {
         return PrecheckPersistentInstanceData.createPrecheckPersistentInstance(instance, predictedState);
      }).filter(Objects::nonNull).collect(Collectors.toList()));
   }

   private EvacuationTaskData getRunningTaskData(ResourceCheckTaskDetails task, ResourceCheckTaskDetails parentTask, ManagedObjectReference clusterRef) throws Exception {
      EvacuationTaskData runningTask = new EvacuationTaskData();
      runningTask.uuid = task.hostUuid;
      if (task.host != null) {
         VmodlHelper.assignServerGuid(task.host, clusterRef.getServerGuid());
         runningTask.name = runningTask.hostName = (String)QueryUtil.getProperty(task.host, "name");
      }

      if (parentTask == null) {
         runningTask.taskMoRef = task.task;
         runningTask.taskType = PrecheckTaskType.HOST_PRECHECK;
      } else {
         runningTask.taskMoRef = parentTask.task;
         runningTask.taskType = PrecheckTaskType.HOST_ENTER_MAINTENANCE_MODE;
      }

      VmodlHelper.assignServerGuid(runningTask.taskMoRef, clusterRef.getServerGuid());
      runningTask.decommissionMode = this.getDecommissionMode(task.maintenanceSpec);
      return runningTask;
   }

   private EvacuationTaskData getRunningTaskData(DiskDataEvacuationResourceCheckTaskDetails task, ResourceCheckTaskDetails parentTask, ManagedObjectReference clusterRef) throws Exception {
      EvacuationTaskData runningTask = new EvacuationTaskData();
      runningTask.uuid = task.diskUuid;
      ManagedObjectReference hostRef = task.host;
      if (hostRef != null) {
         VmodlHelper.assignServerGuid(hostRef, clusterRef.getServerGuid());
         Measure measure = new Measure("Get disk group or disk name.");
         Throwable var7 = null;

         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadDiskMappings(Arrays.asList(hostRef));
            runningTask.hostName = (String)QueryUtil.getProperty(hostRef, "name");
            Map hostToDiskMappings = dataRetriever.getDiskMappings();
            runningTask.name = this.getDiskOrGroupName((DiskMapInfoEx[])hostToDiskMappings.get(hostRef), task.diskUuid);
         } catch (Throwable var17) {
            var7 = var17;
            throw var17;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var16) {
                     var7.addSuppressed(var16);
                  }
               } else {
                  measure.close();
               }
            }

         }
      }

      runningTask.taskMoRef = parentTask == null ? task.task : parentTask.task;
      VmodlHelper.assignServerGuid(runningTask.taskMoRef, clusterRef.getServerGuid());
      if (parentTask == null) {
         runningTask.taskType = this.getTaskType(task);
      } else {
         runningTask.taskType = this.getParentTaskType(runningTask.taskMoRef);
      }

      runningTask.decommissionMode = this.getDecommissionMode(task.maintenanceSpec);
      return runningTask;
   }

   private PrecheckTaskType getTaskType(DiskDataEvacuationResourceCheckTaskDetails task) {
      if (task.isCapacityTier == null) {
         return null;
      } else {
         return BooleanUtils.isTrue(task.isCapacityTier) ? PrecheckTaskType.DISK_PRECHECK : PrecheckTaskType.DISKGROUP_PRECHECK;
      }
   }

   private PrecheckTaskType getParentTaskType(ManagedObjectReference taskRef) {
      TaskInfoData taskInfo = this.taskService.getInfo(taskRef);
      String var3 = taskInfo.descriptionId;
      byte var4 = -1;
      switch(var3.hashCode()) {
      case -2000523129:
         if (var3.equals("com.vmware.vsan.diskmgmt.tasks.removecapacitydiskex")) {
            var4 = 4;
         }
         break;
      case -1374846321:
         if (var3.equals("com.vmware.vsan.diskmgmt.tasks.rebuilddiskmapping")) {
            var4 = 2;
         }
         break;
      case 1054596173:
         if (var3.equals("com.vmware.vsan.diskmgmt.tasks.removediskex")) {
            var4 = 3;
         }
         break;
      case 1162703517:
         if (var3.equals("com.vmware.vsan.diskmgmt.tasks.unmountdiskmappingex")) {
            var4 = 1;
         }
         break;
      case 1556838695:
         if (var3.equals("com.vmware.vsan.diskmgmt.tasks.removediskmappingex")) {
            var4 = 0;
         }
      }

      switch(var4) {
      case 0:
         return PrecheckTaskType.DISKGROUP_REMOVAL;
      case 1:
         return PrecheckTaskType.DISKGROUP_UNMOUNT;
      case 2:
         return PrecheckTaskType.DISKGROUP_RECREATE;
      case 3:
      case 4:
         return PrecheckTaskType.DISK_REMOVAL;
      default:
         return null;
      }
   }

   private String getDiskOrGroupName(DiskMapInfoEx[] mappings, String diskUuid) {
      if (ArrayUtils.isEmpty(mappings)) {
         logger.warn("No disk groups.");
         return diskUuid;
      } else {
         DiskMapInfoEx[] var3 = mappings;
         int var4 = mappings.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            DiskMapInfoEx mapping = var3[var5];
            DiskMapping diskGroup = mapping.mapping;
            if (diskUuid.equalsIgnoreCase(diskGroup.ssd.vsanDiskInfo.vsanUuid)) {
               return Utils.getLocalizedString("vsan.evacuationStatus.formattedDiskGroupName", diskUuid);
            }

            if (ArrayUtils.isNotEmpty(diskGroup.nonSsd)) {
               ScsiDisk[] var8 = diskGroup.nonSsd;
               int var9 = var8.length;

               for(int var10 = 0; var10 < var9; ++var10) {
                  ScsiDisk capacityDisk = var8[var10];
                  if (diskUuid.equalsIgnoreCase(capacityDisk.vsanDiskInfo.vsanUuid)) {
                     return DiskManagementUtil.getDiskName(capacityDisk);
                  }
               }
            }
         }

         return diskUuid;
      }
   }

   private com.vmware.vsan.client.services.diskGroups.data.DecommissionMode getDecommissionMode(MaintenanceSpec maintenanceSpec) {
      com.vmware.vsan.client.services.diskGroups.data.DecommissionMode mode = null;
      if (maintenanceSpec != null && maintenanceSpec.vsanMode != null) {
         String decommissionMode = maintenanceSpec.vsanMode.objectAction;

         try {
            mode = com.vmware.vsan.client.services.diskGroups.data.DecommissionMode.valueOf(decommissionMode);
         } catch (IllegalArgumentException var5) {
            logger.error("Unsupported decommission mode " + decommissionMode, var5);
         }
      }

      return mode;
   }

   private ClusterEvacuationCapacityData parseCapacityReport(ResourceCheckStatus resourceCheckStatus, String uuid, ManagedObjectReference clusterRef) {
      ResourceCheckResult result = null;
      if (VsanCapabilityUtils.isPersistenceResourceCheckSupportedOnVc(clusterRef) && ArrayUtils.isNotEmpty(resourceCheckStatus.componentResults)) {
         result = (ResourceCheckResult)Arrays.stream(resourceCheckStatus.componentResults).filter((compResult) -> {
            return compResult instanceof ResourceCheckVsanResult;
         }).map((compResult) -> {
            return (ResourceCheckVsanResult)compResult;
         }).findFirst().orElse((Object)null);
      }

      if (result == null) {
         result = resourceCheckStatus.result;
      }

      ClusterEvacuationCapacityData clusterCapacityData = new ClusterEvacuationCapacityData();
      clusterCapacityData.status = (PrecheckResultStatusType)EnumUtils.fromStringIgnoreCase(PrecheckResultStatusType.class, result.status);
      if (result.capacityThreshold != null) {
         clusterCapacityData.warningThreshold = (int)result.capacityThreshold.yellowValue;
         clusterCapacityData.errorThreshold = (int)result.capacityThreshold.redValue;
      }

      clusterCapacityData.preOperationCapacity.totalCapacity = result.capacity;
      clusterCapacityData.preOperationCapacity.usedCapacity = result.usedCapacity;
      clusterCapacityData.postOperationCapacity.totalCapacity = result.postOperationCapacity;
      clusterCapacityData.postOperationCapacity.usedCapacity = result.postOperationUsedCapacity;
      if (ArrayUtils.isEmpty(result.faultDomains)) {
         return clusterCapacityData;
      } else {
         Map hostIdToIconIdMap = this.getHostIdToIconIdMap(result.faultDomains, clusterRef.getServerGuid());
         FaultDomainResourceCheckResult[] var7 = result.faultDomains;
         int var8 = var7.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            FaultDomainResourceCheckResult resourceCheckResult = var7[var9];
            this.assignCapacityDataFromFaultDomainsResource(clusterCapacityData, resourceCheckResult, uuid, hostIdToIconIdMap);
         }

         this.sortCapacityEntities(clusterCapacityData);
         return clusterCapacityData;
      }
   }

   private void sortCapacityEntities(ClusterEvacuationCapacityData clusterCapacityData) {
      if (!CollectionUtils.isEmpty(clusterCapacityData.faultDomains)) {
         clusterCapacityData.faultDomains.sort(Comparator.comparing((f) -> {
            return f.faultDomainName;
         }));
         clusterCapacityData.faultDomains.forEach((fd) -> {
            fd.hostsCapacityData.sort(Comparator.comparing((h) -> {
               return h.hostName;
            }));
         });
      }

      if (!CollectionUtils.isEmpty(clusterCapacityData.standaloneHosts)) {
         clusterCapacityData.standaloneHosts.sort(Comparator.comparing((s) -> {
            return s.hostName;
         }));
      }

   }

   private ClusterEvacuationCapacityData parseVsanDirectCapacity(ManagedObjectReference clusterRef, EvacuationEntity selectedEvacuationEntity, VsanHealthThreshold capacityThreshold, String persistenceStatus) {
      if (selectedEvacuationEntity.type != EvacuationEntityType.HOST) {
         return null;
      } else {
         ClusterEvacuationCapacityData vsanDirectClusterCapacityData = new ClusterEvacuationCapacityData();
         vsanDirectClusterCapacityData.status = (PrecheckResultStatusType)EnumUtils.fromStringIgnoreCase(PrecheckResultStatusType.class, persistenceStatus);
         if (vsanDirectClusterCapacityData.status == PrecheckResultStatusType.RED) {
            return vsanDirectClusterCapacityData;
         } else {
            QueryExecutorResult queryResult = this.queryVsanDirectDatastoresAndHosts(clusterRef);
            QueryResult datastoreResults = queryResult.getQueryResult("vsan-direct-datastore-properties");
            QueryResult hostResults = queryResult.getQueryResult("host-properties");
            if (hostResults.exception == null && datastoreResults.exception == null) {
               if (!CollectionUtils.isEmpty(datastoreResults.items) && !CollectionUtils.isEmpty(hostResults.items)) {
                  if (capacityThreshold != null) {
                     vsanDirectClusterCapacityData.warningThreshold = (int)capacityThreshold.yellowValue;
                     vsanDirectClusterCapacityData.errorThreshold = (int)capacityThreshold.redValue;
                  } else {
                     vsanDirectClusterCapacityData.warningThreshold = 70;
                     vsanDirectClusterCapacityData.errorThreshold = 90;
                  }

                  this.assignVsanDirectCapacity(vsanDirectClusterCapacityData, selectedEvacuationEntity.hostRef, datastoreResults.items, hostResults.items);
                  this.sortCapacityEntities(vsanDirectClusterCapacityData);
                  return vsanDirectClusterCapacityData;
               } else {
                  return null;
               }
            } else {
               vsanDirectClusterCapacityData.errorMessages = new String[]{Utils.getLocalizedString("vsan.evacuationStatus.vSanDirectInfoRetrievalFailed")};
               return vsanDirectClusterCapacityData;
            }
         }
      }
   }

   private QueryExecutorResult queryVsanDirectDatastoresAndHosts(ManagedObjectReference clusterRef) {
      RequestSpec requestSpec = (new QueryBuilder()).newQuery("vsan-direct-datastore-properties").select("summary.capacity", "summary.freeSpace", "host").from(clusterRef).join(HostSystem.class).on("host").join(Datastore.class).on("datastore").where().propertyEquals("summary.type", "vsanD").end().newQuery("host-properties").select("name", "primaryIconId", "config.vsanHostConfig.faultDomainInfo.name").from(clusterRef).join(HostSystem.class).on("host").where().propertyEquals("runtime.inMaintenanceMode", false).end().build();
      return this.queryExecutor.execute(requestSpec);
   }

   private void assignVsanDirectCapacity(ClusterEvacuationCapacityData vsanDirectClusterCapacityData, ManagedObjectReference selectedHost, List datastoreQueryModels, List hostQueryModels) {
      Iterator var5 = hostQueryModels.iterator();

      while(var5.hasNext()) {
         QueryModel hostQueryModel = (QueryModel)var5.next();
         ManagedObjectReference hostRef = (ManagedObjectReference)hostQueryModel.id;
         boolean isHostSelected = selectedHost.equals(hostRef);
         datastoreQueryModels.forEach((datastore) -> {
            this.assignClusterVsanDirectCapacity(vsanDirectClusterCapacityData, hostQueryModel.properties, datastore.properties, hostRef, isHostSelected);
         });
      }

   }

   private boolean isDatastoreMountedForHost(Map datastoreProperties, ManagedObjectReference hostRef) {
      HostMount[] hostMounts = (HostMount[])((HostMount[])datastoreProperties.get("host"));
      return !ArrayUtils.isEmpty(hostMounts) && hostMounts[0] != null ? hostMounts[0].key.equals(hostRef) : false;
   }

   private void assignClusterVsanDirectCapacity(ClusterEvacuationCapacityData vsanDirectClusterCapacityData, Map hostProperties, Map datastoreProperties, ManagedObjectReference hostRef, boolean isHostSelected) {
      String hostName = (String)hostProperties.get("name");
      String iconId = (String)hostProperties.get("primaryIconId");
      String fdName = (String)hostProperties.get("config.vsanHostConfig.faultDomainInfo.name");
      long totalCapacity = 0L;
      long usedCapacity = 0L;
      if (this.isDatastoreMountedForHost(datastoreProperties, hostRef)) {
         Long totalCapacityProperty = (Long)datastoreProperties.get("summary.capacity");
         Long freeSpaceProperty = (Long)datastoreProperties.get("summary.freeSpace");
         long freeSpace = NumberUtils.toLong(freeSpaceProperty);
         totalCapacity = NumberUtils.toLong(totalCapacityProperty);
         usedCapacity = totalCapacity - freeSpace;
      }

      vsanDirectClusterCapacityData.addVsanDirectClusterCapacityData(usedCapacity, totalCapacity, isHostSelected);
      if (StringUtils.isNotEmpty(fdName)) {
         this.assignFaultDomainVsanDirectCapacity(vsanDirectClusterCapacityData.faultDomains, hostName, iconId, fdName, usedCapacity, totalCapacity, isHostSelected);
      } else {
         this.assignVsanDirectHostCapacityData(vsanDirectClusterCapacityData.standaloneHosts, hostName, iconId, usedCapacity, totalCapacity, isHostSelected);
      }

   }

   private void assignFaultDomainVsanDirectCapacity(List faultDomains, String hostName, String iconId, String fdName, long usedCapacity, long totalCapacity, boolean isHostSelected) {
      FaultDomainEvacuationCapacityData faultDomain = (FaultDomainEvacuationCapacityData)faultDomains.stream().filter((fd) -> {
         return StringUtils.equals(fd.faultDomainName, fdName);
      }).findFirst().orElse((Object)null);
      if (faultDomain == null) {
         faultDomain = new FaultDomainEvacuationCapacityData(fdName);
         faultDomains.add(faultDomain);
      }

      faultDomain.addVsanDirectFaultDomainCapacityData(usedCapacity, totalCapacity, isHostSelected);
      this.assignVsanDirectHostCapacityData(faultDomain.hostsCapacityData, hostName, iconId, usedCapacity, totalCapacity, isHostSelected);
   }

   public void assignVsanDirectHostCapacityData(List hosts, String hostName, String iconId, long usedCapacity, long totalCapacity, boolean isHostSelected) {
      HostEvacuationCapacityData host = (HostEvacuationCapacityData)hosts.stream().filter((s) -> {
         return StringUtils.equals(s.hostName, hostName);
      }).findFirst().orElse((Object)null);
      if (host == null) {
         host = new HostEvacuationCapacityData(hostName, iconId, isHostSelected);
         hosts.add(host);
      }

      host.addVsanDirectHostCapacityData(usedCapacity, totalCapacity, isHostSelected);
   }

   private Map getHostIdToIconIdMap(FaultDomainResourceCheckResult[] faultDomains, String serverGuid) {
      List hostMoRefs = new ArrayList();
      FaultDomainResourceCheckResult[] var4 = faultDomains;
      int var5 = faultDomains.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         FaultDomainResourceCheckResult resourceCheckResult = var4[var6];
         if (resourceCheckResult != null && !ArrayUtils.isEmpty(resourceCheckResult.hosts)) {
            HostResourceCheckResult[] var8 = resourceCheckResult.hosts;
            int var9 = var8.length;

            for(int var10 = 0; var10 < var9; ++var10) {
               HostResourceCheckResult hostResource = var8[var10];
               if (hostResource != null && hostResource.host != null && !hostResource.isNew) {
                  ManagedObjectReference hostMoRef = VmodlHelper.assignServerGuid(hostResource.host, serverGuid);
                  hostMoRefs.add(hostMoRef);
               }
            }
         }
      }

      Map hostIdToIconIdMap = new HashMap();
      if (CollectionUtils.isEmpty(hostMoRefs)) {
         return hostIdToIconIdMap;
      } else {
         try {
            DataServiceResponse hostIconIdsResponse = QueryUtil.getProperties((ManagedObjectReference[])hostMoRefs.toArray(new ManagedObjectReference[0]), new String[]{"primaryIconId"});
            Iterator var16 = hostIconIdsResponse.getResourceObjects().iterator();

            while(var16.hasNext()) {
               Object hostRef = var16.next();
               String hostIconId = (String)hostIconIdsResponse.getProperty(hostRef, "primaryIconId");
               String hostRefValue = ((ManagedObjectReference)hostRef).getValue();
               hostIdToIconIdMap.put(hostRefValue, hostIconId);
            }
         } catch (Exception var13) {
            logger.error("Cannot retrieve Primary icon ID property for the returned list of hosts.", var13);
         }

         return hostIdToIconIdMap;
      }
   }

   private void assignCapacityDataFromFaultDomainsResource(ClusterEvacuationCapacityData clusterCapacityData, FaultDomainResourceCheckResult resourceCheckResult, String uuid, Map hostIdToIconIdMap) {
      if (resourceCheckResult != null && !ArrayUtils.isEmpty(resourceCheckResult.hosts)) {
         FaultDomainEvacuationCapacityData faultDomainCapacityData = new FaultDomainEvacuationCapacityData(resourceCheckResult.name);
         if (resourceCheckResult.isNew) {
            ++clusterCapacityData.faultDomainsNeeded;
         } else {
            HostResourceCheckResult[] var6 = resourceCheckResult.hosts;
            int var7 = var6.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               HostResourceCheckResult hostResource = var6[var8];
               this.assignCapacityDataFromHostResource(faultDomainCapacityData, hostResource, uuid, hostIdToIconIdMap);
            }

            if (StringUtils.isEmpty(faultDomainCapacityData.faultDomainName)) {
               clusterCapacityData.standaloneHosts.addAll(faultDomainCapacityData.hostsCapacityData);
            } else if (!resourceCheckResult.isNew) {
               faultDomainCapacityData.preOperationCapacity.totalCapacity = resourceCheckResult.capacity;
               faultDomainCapacityData.preOperationCapacity.usedCapacity = resourceCheckResult.usedCapacity;
               faultDomainCapacityData.postOperationCapacity.totalCapacity = resourceCheckResult.postOperationCapacity;
               faultDomainCapacityData.postOperationCapacity.usedCapacity = resourceCheckResult.postOperationUsedCapacity;
               faultDomainCapacityData.hasInsufficientSpace = !faultDomainCapacityData.isAdditionalHostNeeded && resourceCheckResult.additionalRequiredCapacity > 0L;
               clusterCapacityData.faultDomains.add(faultDomainCapacityData);
            }

         }
      }
   }

   private void assignCapacityDataFromHostResource(FaultDomainEvacuationCapacityData faultDomainCapacityData, HostResourceCheckResult hostResource, String uuid, Map hostIdToIconIdMap) {
      if (hostResource != null) {
         if (StringUtils.isNotEmpty(faultDomainCapacityData.faultDomainName) && hostResource.isNew) {
            faultDomainCapacityData.isAdditionalHostNeeded = true;
         } else {
            HostEvacuationCapacityData hostCapacityData = new HostEvacuationCapacityData(hostResource.name);
            String hostIconId = null;
            if (!CollectionUtils.isEmpty(hostIdToIconIdMap) && hostResource.host != null) {
               hostIconId = (String)hostIdToIconIdMap.get(hostResource.host.getValue());
            }

            hostCapacityData.iconId = StringUtils.isNotEmpty(hostIconId) ? hostIconId : "vsphere-icon-host";
            this.assignEntitySelectionFromHostResource(hostCapacityData, hostResource, uuid);
            hostCapacityData.capacityNeeded = hostResource.additionalRequiredCapacity;
            hostCapacityData.preOperationCapacity.totalCapacity = hostResource.capacity;
            hostCapacityData.preOperationCapacity.usedCapacity = hostResource.usedCapacity;
            hostCapacityData.postOperationCapacity.totalCapacity = hostResource.postOperationCapacity;
            hostCapacityData.postOperationCapacity.usedCapacity = hostResource.postOperationUsedCapacity;
            if (hostResource.components != null && hostResource.maxComponents != null && hostResource.components > 0L && hostResource.maxComponents > 0L) {
               boolean isComponentLimitReached = hostResource.components.equals(hostResource.maxComponents);
               hostCapacityData.isComponentLimitReached = isComponentLimitReached;
               faultDomainCapacityData.isComponentLimitReached = isComponentLimitReached;
            }

            faultDomainCapacityData.hostsCapacityData.add(hostCapacityData);
         }
      }
   }

   private void assignEntitySelectionFromHostResource(HostEvacuationCapacityData hostCapacityData, HostResourceCheckResult hostResource, String uuid) {
      if (uuid.equals(hostResource.uuid)) {
         hostCapacityData.selectedEntityType = EvacuationEntityType.HOST;
      } else if (!ArrayUtils.isEmpty(hostResource.diskGroups)) {
         DiskGroupResourceCheckResult[] var4 = hostResource.diskGroups;
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            DiskGroupResourceCheckResult diskGroup = var4[var6];
            if (uuid.equals(diskGroup.uuid) || uuid.equals(diskGroup.cacheTierDisk.uuid)) {
               hostCapacityData.selectedEntityType = EvacuationEntityType.DISK_GROUP;
               return;
            }

            DiskResourceCheckResult[] var8 = diskGroup.capacityTierDisks;
            int var9 = var8.length;

            for(int var10 = 0; var10 < var9; ++var10) {
               DiskResourceCheckResult capacityDisk = var8[var10];
               if (uuid.equals(capacityDisk.uuid)) {
                  hostCapacityData.selectedEntityType = EvacuationEntityType.CAPACITY_DISK;
                  return;
               }
            }
         }

      }
   }
}
