package com.vmware.vsan.client.services.cns;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.pbm.ServerObjectRef;
import com.vmware.vim.binding.pbm.ServerObjectRef.ObjectType;
import com.vmware.vim.binding.pbm.capability.CapabilityInstance;
import com.vmware.vim.binding.pbm.capability.CapabilityMetadata;
import com.vmware.vim.binding.pbm.capability.ConstraintInstance;
import com.vmware.vim.binding.pbm.capability.PropertyInstance;
import com.vmware.vim.binding.pbm.capability.PropertyMetadata;
import com.vmware.vim.binding.pbm.capability.provider.CapabilityObjectMetadataPerCategory;
import com.vmware.vim.binding.pbm.capability.provider.CapabilityObjectSchema;
import com.vmware.vim.binding.pbm.capability.types.DiscreteSet;
import com.vmware.vim.binding.pbm.compliance.ComplianceResult;
import com.vmware.vim.binding.pbm.compliance.PolicyStatus;
import com.vmware.vim.binding.pbm.profile.ProfileManager;
import com.vmware.vim.binding.pbm.profile.ReconfigOutcome;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.ResourcePool;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.Datastore.HostMount;
import com.vmware.vim.binding.vim.vslm.ID;
import com.vmware.vim.binding.vim.vslm.VStorageObject;
import com.vmware.vim.binding.vim.vslm.BaseConfigInfo.BackingInfo;
import com.vmware.vim.binding.vim.vslm.BaseConfigInfo.DiskFileBackingInfo;
import com.vmware.vim.binding.vim.vslm.vcenter.RetrieveVStorageObjSpec;
import com.vmware.vim.binding.vim.vslm.vcenter.VStorageObjectAssociations;
import com.vmware.vim.binding.vim.vslm.vcenter.VStorageObjectManager;
import com.vmware.vim.binding.vim.vslm.vcenter.VStorageObjectAssociations.VmDiskAssociations;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.InvalidArgument;
import com.vmware.vim.vsan.binding.vim.cns.Cursor;
import com.vmware.vim.vsan.binding.vim.cns.KubernetesQueryFilter;
import com.vmware.vim.vsan.binding.vim.cns.QueryFilter;
import com.vmware.vim.vsan.binding.vim.cns.QueryResult;
import com.vmware.vim.vsan.binding.vim.cns.SearchLabelResult;
import com.vmware.vim.vsan.binding.vim.cns.SearchLabelSpec;
import com.vmware.vim.vsan.binding.vim.cns.VolumeId;
import com.vmware.vim.vsan.binding.vim.cns.VolumeManager;
import com.vmware.vim.vsan.binding.vim.fault.CnsFault;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.BackendLocalizedException;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.cns.model.ClusterFlavor;
import com.vmware.vsan.client.services.cns.model.CnsDatastoreAccessibilityStatus;
import com.vmware.vsan.client.services.cns.model.CnsHealthStatus;
import com.vmware.vsan.client.services.cns.model.CnsHostData;
import com.vmware.vsan.client.services.cns.model.DatastoreMetadata;
import com.vmware.vsan.client.services.cns.model.FileShareConfig;
import com.vmware.vsan.client.services.cns.model.GuestClusterData;
import com.vmware.vsan.client.services.cns.model.KubernetesEntity;
import com.vmware.vsan.client.services.cns.model.QueryLabelResult;
import com.vmware.vsan.client.services.cns.model.VirtualObject;
import com.vmware.vsan.client.services.cns.model.Volume;
import com.vmware.vsan.client.services.cns.model.VolumeComplianceFailure;
import com.vmware.vsan.client.services.cns.model.VolumeContainerCluster;
import com.vmware.vsan.client.services.cns.model.VolumeDatastoreData;
import com.vmware.vsan.client.services.cns.model.VolumeDetails;
import com.vmware.vsan.client.services.cns.model.VolumeFilterResult;
import com.vmware.vsan.client.services.cns.model.VolumeFilterSpec;
import com.vmware.vsan.client.services.common.data.BasicVmData;
import com.vmware.vsan.client.services.common.data.LabelData;
import com.vmware.vsan.client.services.fileservice.VsanFileServiceConfigService;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceShare;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.services.virtualobjects.data.DisplayObjectType;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VapiUtils;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.dataservice.query.OnStatement;
import com.vmware.vsan.client.util.dataservice.query.PropertyConditionStatement;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutorResult;
import com.vmware.vsan.client.util.dataservice.query.SelectStatement;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CnsService {
   private static final String COMPLIANCE_FAILURE_TAG_DELIMITER = ",";
   private static final long QUERY_LABELS_MAX_RESULTS = 10L;
   private static final Set VOLUME_FILTER_EXCEPTION_KEYS = (Set)Stream.of("filter.complianceStatus", "filter.datastoreAccessibilityStatus", "filter.podNames", "filter.pvNames", "filter.labels").collect(Collectors.toCollection(HashSet::new));
   private static final String[] datastoreProperties = new String[]{"name", "summary.type", "summary.url"};
   private static final Logger logger = LoggerFactory.getLogger(CnsService.class);
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private PbmClient pbmClient;
   @Autowired
   private VsanFileServiceConfigService fileServiceConfigService;
   @Autowired
   private QueryExecutor queryExecutor;

   @TsService
   public VolumeFilterResult getVolumes(ManagedObjectReference objectRef, VolumeFilterSpec filterSpec) throws Exception {
      VolumeFilterResult result = new VolumeFilterResult();
      Map allDatastoresMetadata = this.queryDatastores(objectRef, filterSpec.datastore);
      if (MapUtils.isEmpty(allDatastoresMetadata)) {
         result.volumes = new Volume[0];
         return result;
      } else {
         boolean isFileVolumesSupported = VsanCapabilityUtils.isFileVolumesSupportedOnVc(objectRef);
         boolean isVc = this.vmodlHelper.isVcRootFolder(objectRef);
         QueryFilter filter = isFileVolumesSupported ? this.createKubernetesQueryFilter(filterSpec, allDatastoresMetadata, isVc) : this.createLegacyQueryFilter(filterSpec, allDatastoresMetadata, isVc);
         VsanConnection connection = this.vsanClient.getConnection(objectRef.getServerGuid());
         Throwable var9 = null;

         try {
            VolumeManager volumeManager = connection.getCnsVolumeManager();

            QueryResult queryResult;
            try {
               Measure measure = new Measure("VolumeManager.query");
               Throwable var13 = null;

               try {
                  queryResult = volumeManager.query(filter);
               } catch (Throwable var41) {
                  var13 = var41;
                  throw var41;
               } finally {
                  if (measure != null) {
                     if (var13 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var40) {
                           var13.addSuppressed(var40);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }
            } catch (InvalidArgument var43) {
               logger.error("VolumeManager query returns error: ", var43);
               throw (Exception)(VOLUME_FILTER_EXCEPTION_KEYS.contains(var43.getInvalidProperty()) ? new BackendLocalizedException(var43) : new VsanUiLocalizableException());
            } catch (Exception var44) {
               logger.error("VolumeManager query returns error: ", var44);
               throw new VsanUiLocalizableException();
            }

            result.total = queryResult.cursor.totalRecords;
            if (ArrayUtils.isEmpty(queryResult.volumes)) {
               result.volumes = new Volume[0];
               VolumeFilterResult var48 = result;
               return var48;
            }

            List volumes = this.createVolumes(objectRef, queryResult, allDatastoresMetadata);
            result.volumes = (Volume[])volumes.toArray(new Volume[0]);
         } catch (Throwable var45) {
            var9 = var45;
            throw var45;
         } finally {
            if (connection != null) {
               if (var9 != null) {
                  try {
                     connection.close();
                  } catch (Throwable var39) {
                     var9.addSuppressed(var39);
                  }
               } else {
                  connection.close();
               }
            }

         }

         return result;
      }
   }

   private QueryFilter createKubernetesQueryFilter(VolumeFilterSpec filterSpec, Map allDatastoresMetadata, boolean isVc) {
      KubernetesQueryFilter filter = new KubernetesQueryFilter();
      if (StringUtils.isNotEmpty(filterSpec.namespace)) {
         filter.namespaces = new String[]{filterSpec.namespace};
      }

      if (StringUtils.isNotEmpty(filterSpec.pvName)) {
         filter.pvNames = new String[]{filterSpec.pvName};
      }

      if (StringUtils.isNotEmpty(filterSpec.pvcName)) {
         filter.pvcNames = new String[]{filterSpec.pvcName};
      }

      if (StringUtils.isNotEmpty(filterSpec.podName)) {
         filter.podNames = new String[]{filterSpec.podName};
      }

      if (StringUtils.isNotEmpty(filterSpec.healthStatus)) {
         filter.healthStatus = filterSpec.healthStatus;
      }

      return this.createQueryFilter(filter, filterSpec, allDatastoresMetadata, isVc);
   }

   private QueryFilter createLegacyQueryFilter(VolumeFilterSpec filterSpec, Map allDatastoresMetadata, boolean isVc) {
      QueryFilter filter = new QueryFilter();
      if (StringUtils.isNotEmpty(filterSpec.healthStatus)) {
         CnsDatastoreAccessibilityStatus status = CnsDatastoreAccessibilityStatus.fromHealthStatus(CnsHealthStatus.fromName(filterSpec.healthStatus));
         if (status != null) {
            filter.datastoreAccessibilityStatus = status.name();
         }
      }

      return this.createQueryFilter(filter, filterSpec, allDatastoresMetadata, isVc);
   }

   private QueryFilter createQueryFilter(QueryFilter filter, VolumeFilterSpec filterSpec, Map allDatastoresMetadata, boolean isVc) {
      if (StringUtils.isNotEmpty(filterSpec.name)) {
         filter.names = new String[]{filterSpec.name};
      }

      if (StringUtils.isNotEmpty(filterSpec.containerCluster)) {
         List clustersMoRefValue = this.getClustersMoRefValuesFromName(filterSpec.containerCluster, (ManagedObjectReference)BaseUtils.getMapNextKey(allDatastoresMetadata));
         List guestClustersIds = this.getGuestClustersIdsByName(filterSpec.containerCluster);
         List clusterIds = new ArrayList();
         clusterIds.addAll(clustersMoRefValue);
         clusterIds.addAll(guestClustersIds);
         clusterIds.add(filterSpec.containerCluster);
         filter.containerClusterIds = (String[])clusterIds.toArray(new String[0]);
      }

      if (!ArrayUtils.isEmpty(filterSpec.labels)) {
         filter.labels = LabelData.toKeyValue(filterSpec.labels);
      }

      if (StringUtils.isNotEmpty(filterSpec.id)) {
         VolumeId volumeId = new VolumeId();
         volumeId.id = filterSpec.id;
         filter.volumeIds = new VolumeId[]{volumeId};
      }

      if (StringUtils.isNotEmpty(filterSpec.complianceStatus)) {
         filter.complianceStatus = filterSpec.complianceStatus;
      }

      filter.storagePolicyId = filterSpec.storagePolicy;
      if (!isVc || !StringUtils.isEmpty(filterSpec.datastore)) {
         filter.datastores = (ManagedObjectReference[])allDatastoresMetadata.keySet().toArray(new ManagedObjectReference[0]);
      }

      filter.cursor = new Cursor();
      filter.cursor.limit = filterSpec.limit;
      filter.cursor.offset = filterSpec.offset;
      return filter;
   }

   private List getClustersMoRefValuesFromName(String clusterName, ManagedObjectReference moRef) {
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select().from(ClusterComputeResource.class).where().propertyIsGreaterOrEquals("host._length", 1).and().propertyEquals("name", clusterName).and().propertyEquals("serverGuid", moRef.getServerGuid()).end().build();
      QueryExecutorResult result = this.queryExecutor.execute(requestSpec);
      com.vmware.vsan.client.util.dataservice.query.QueryResult clustersResult = result.getQueryResult();
      if (CollectionUtils.isEmpty(clustersResult.items)) {
         return new ArrayList();
      } else {
         List clustersMoRefValues = (List)clustersResult.items.stream().filter((queryModel) -> {
            return queryModel != null;
         }).map((queryModel) -> {
            return ((ManagedObjectReference)queryModel.id).getValue();
         }).collect(Collectors.toList());
         return clustersMoRefValues;
      }
   }

   private List getGuestClustersIdsByName(String name) {
      List clustersIds = new ArrayList();
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select().from("com.vmware.wcp.TanzuKubernetesCluster").where().propertyEquals("name", name).end().build();
      QueryExecutorResult result = this.queryExecutor.execute(requestSpec);
      com.vmware.vsan.client.util.dataservice.query.QueryResult clustersResult = result.getQueryResult();
      if (CollectionUtils.isEmpty(clustersResult.items)) {
         return clustersIds;
      } else {
         clustersResult.items.stream().forEach((queryModel) -> {
            String clusterId = VapiUtils.getVapiId((URI)queryModel.id);
            if (StringUtils.isNotEmpty(clusterId)) {
               clustersIds.add(clusterId);
            }

         });
         return clustersIds;
      }
   }

   private Map queryDatastores(ManagedObjectReference contextObjectRef, String datastoreNameFilter) {
      Map result = new HashMap();
      RequestSpec requestSpec = this.getDatastoresSpec(contextObjectRef, datastoreNameFilter);
      QueryExecutorResult queryExecutorResult = this.queryExecutor.execute(requestSpec);
      DataServiceResponse response = queryExecutorResult.getDataServiceResponse();
      if (response != null && !CollectionUtils.isEmpty(response.getResourceObjects())) {
         Object resourceObject;
         DatastoreMetadata metadata;
         for(Iterator var7 = response.getResourceObjects().iterator(); var7.hasNext(); metadata.datastoreUrl = (String)response.getProperty(resourceObject, "summary.url")) {
            resourceObject = var7.next();
            metadata = new DatastoreMetadata();
            result.put((ManagedObjectReference)resourceObject, metadata);
            metadata.name = (String)response.getProperty(resourceObject, "name");
            metadata.type = (String)response.getProperty(resourceObject, "summary.type");
         }

         return result;
      } else {
         return result;
      }
   }

   private RequestSpec getDatastoresSpec(ManagedObjectReference contextObjectRef, String datastoreNameFilter) {
      SelectStatement selectStatement = (new QueryBuilder()).newQuery().select(datastoreProperties);
      if (this.vmodlHelper.isVcRootFolder(contextObjectRef)) {
         return this.buildVcDatastoresSpec(selectStatement, contextObjectRef, datastoreNameFilter);
      } else if (!this.vmodlHelper.isOfType(contextObjectRef, Datacenter.class) && !this.vmodlHelper.isOfType(contextObjectRef, ClusterComputeResource.class)) {
         if (this.vmodlHelper.isOfType(contextObjectRef, Datastore.class)) {
            return this.buildDatastoreSpec(selectStatement, contextObjectRef);
         } else {
            logger.error("Datastores query should be invoked only for VC, DC, Cluster or Datastore.Currently it is invoked for " + contextObjectRef.getType());
            throw new VsanUiLocalizableException();
         }
      } else {
         return this.buildDcClusterDatastoresSpec(selectStatement, contextObjectRef, datastoreNameFilter);
      }
   }

   private RequestSpec buildVcDatastoresSpec(SelectStatement selectStatement, ManagedObjectReference contextObjectRef, String datastoreNameFilter) {
      PropertyConditionStatement propertyConditionStatement = selectStatement.from(Datastore.class.getSimpleName()).where().propertyEquals("serverGuid", contextObjectRef.getServerGuid());
      if (StringUtils.isNotEmpty(datastoreNameFilter)) {
         propertyConditionStatement.and().propertyContains("name", datastoreNameFilter);
      }

      return propertyConditionStatement.end().build();
   }

   private RequestSpec buildDcClusterDatastoresSpec(SelectStatement selectStatement, ManagedObjectReference contextObjectRef, String datastoreNameFilter) {
      OnStatement onStatement = selectStatement.from(contextObjectRef).join(Datastore.class.getSimpleName()).on("datastore");
      if (StringUtils.isNotEmpty(datastoreNameFilter)) {
         onStatement.where().propertyContains("name", datastoreNameFilter);
      }

      return onStatement.end().build();
   }

   private RequestSpec buildDatastoreSpec(SelectStatement selectStatement, ManagedObjectReference contextObjectRef) {
      return selectStatement.from(contextObjectRef).end().build();
   }

   private List createVolumes(ManagedObjectReference objectRef, QueryResult queryResult, Map allDatastoresMetadata) {
      Multimap dsUrlToMoRefMap = this.bindDatastoreUrlToDatastoreRefs(allDatastoresMetadata);
      List volumes = new ArrayList();
      com.vmware.vim.vsan.binding.vim.cns.Volume[] var6 = queryResult.volumes;
      int var7 = var6.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         com.vmware.vim.vsan.binding.vim.cns.Volume cnsVolume = var6[var8];
         Collection volumeDatastoreRefs = dsUrlToMoRefMap.get(cnsVolume.datastoreUrl);
         if (CollectionUtils.isEmpty(volumeDatastoreRefs)) {
            logger.warn("Cannot map data store moRef to the cns volume. Data store MoRef doesn't exists for " + cnsVolume.datastoreUrl + " !");
         }

         ArrayList datastoreDataList = new ArrayList(volumeDatastoreRefs.size());
         String datastoreType = null;
         Iterator var13 = volumeDatastoreRefs.iterator();

         while(var13.hasNext()) {
            ManagedObjectReference datastoreRef = (ManagedObjectReference)var13.next();
            DatastoreMetadata dsMetadata = (DatastoreMetadata)allDatastoresMetadata.get(datastoreRef);
            if (StringUtils.isEmpty(datastoreType)) {
               datastoreType = dsMetadata.type;
            }

            VolumeDatastoreData datastoreData = new VolumeDatastoreData();
            datastoreData.name = dsMetadata.name;
            datastoreData.reference = datastoreRef;
            datastoreDataList.add(datastoreData);
         }

         VolumeFactory factory = BaseVolumeFactory.getInstance(objectRef);
         Volume volume = factory.createVolume(datastoreDataList, datastoreType, cnsVolume);
         volumes.add(volume);
      }

      this.updateContainerClusterWCPData(volumes, objectRef);
      return volumes;
   }

   private Multimap bindDatastoreUrlToDatastoreRefs(Map datastoresMetadata) {
      Multimap result = ArrayListMultimap.create();
      Iterator var3 = datastoresMetadata.entrySet().iterator();

      while(var3.hasNext()) {
         Entry dsEntry = (Entry)var3.next();
         result.put(((DatastoreMetadata)dsEntry.getValue()).datastoreUrl, dsEntry.getKey());
      }

      return result;
   }

   private void updateContainerClusterWCPData(List volumes, ManagedObjectReference objectRef) {
      ManagedObjectReference[] clustersRefs = this.extractSupervisorClustersRefs(volumes, objectRef);
      if (!ArrayUtils.isEmpty(clustersRefs)) {
         String[] guestClustersIds = this.extractGuestClustersIds(volumes, objectRef);
         Map guestClusterIdToData = this.getGuestClustersData(guestClustersIds, objectRef);
         DataServiceResponse clustersProperties = this.getClustersDsProperties(clustersRefs);
         Iterator var7 = volumes.iterator();

         while(var7.hasNext()) {
            Volume volume = (Volume)var7.next();
            Iterator var9 = volume.containerClusters.iterator();

            while(var9.hasNext()) {
               VolumeContainerCluster cluster = (VolumeContainerCluster)var9.next();
               if (cluster.flavor == ClusterFlavor.WORKLOAD) {
                  if (clustersProperties != null) {
                     ManagedObjectReference clusterRef = new ManagedObjectReference(ClusterComputeResource.class.getSimpleName(), cluster.name, objectRef.getServerGuid());
                     String clusterName = (String)clustersProperties.getProperty(clusterRef, "name");
                     String clusterIcon = (String)clustersProperties.getProperty(clusterRef, "primaryIconId");
                     if (StringUtils.isNotEmpty(clusterName) && StringUtils.isNotEmpty(clusterIcon)) {
                        cluster.clusterData = new InventoryNode(clusterRef, clusterName, clusterIcon);
                     }
                  }

                  if (this.hasGuestCluster(cluster)) {
                     VolumeContainerCluster guestCluster = ((KubernetesEntity)cluster.persistentVolumes.get(0)).persistentVolumeClaim.guestCluster;
                     GuestClusterData guestClusterData = (GuestClusterData)guestClusterIdToData.get(guestCluster.name);
                     if (guestClusterData != null) {
                        guestCluster.clusterData = guestClusterData.clusterData;
                     }
                  }
               }
            }
         }

      }
   }

   private ManagedObjectReference[] extractSupervisorClustersRefs(List volumes, ManagedObjectReference objectRef) {
      ManagedObjectReference[] clustersRefs = (ManagedObjectReference[])volumes.stream().filter((volume) -> {
         return CollectionUtils.isNotEmpty(volume.containerClusters);
      }).flatMap((volume) -> {
         return volume.containerClusters.stream();
      }).filter((cluster) -> {
         return cluster.flavor == ClusterFlavor.WORKLOAD;
      }).map((cluster) -> {
         return new ManagedObjectReference(ClusterComputeResource.class.getSimpleName(), cluster.name, objectRef.getServerGuid());
      }).distinct().toArray((x$0) -> {
         return new ManagedObjectReference[x$0];
      });
      return clustersRefs;
   }

   private String[] extractGuestClustersIds(List volumes, ManagedObjectReference objectRef) {
      String[] clustersIds = (String[])volumes.stream().filter((volume) -> {
         return CollectionUtils.isNotEmpty(volume.containerClusters);
      }).flatMap((volume) -> {
         return volume.containerClusters.stream();
      }).filter((cluster) -> {
         return cluster.flavor == ClusterFlavor.WORKLOAD && this.hasGuestCluster(cluster);
      }).map((cluster) -> {
         return ((KubernetesEntity)cluster.persistentVolumes.get(0)).persistentVolumeClaim.guestCluster.name;
      }).distinct().toArray((x$0) -> {
         return new String[x$0];
      });
      return clustersIds;
   }

   private boolean hasGuestCluster(VolumeContainerCluster cluster) {
      return CollectionUtils.isNotEmpty(cluster.persistentVolumes) && ((KubernetesEntity)cluster.persistentVolumes.get(0)).persistentVolumeClaim != null && ((KubernetesEntity)cluster.persistentVolumes.get(0)).persistentVolumeClaim.guestCluster != null;
   }

   private Map getGuestClustersData(String[] guestClustersIds, ManagedObjectReference objectRef) {
      Map clusterIdToData = new HashMap();
      if (ArrayUtils.isEmpty(guestClustersIds)) {
         return clusterIdToData;
      } else {
         QueryBuilder queryBuilder = new QueryBuilder();
         String[] var5 = guestClustersIds;
         int var6 = guestClustersIds.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            String clusterId = var5[var7];
            queryBuilder.newQuery(clusterId).select("name", "workload").from("com.vmware.wcp.TanzuKubernetesCluster").where().idEquals(clusterId, objectRef.getServerGuid()).end();
         }

         RequestSpec requestSpec = queryBuilder.build();
         QueryExecutorResult result = this.queryExecutor.execute(requestSpec);
         String[] var14 = guestClustersIds;
         int var15 = guestClustersIds.length;

         for(int var9 = 0; var9 < var15; ++var9) {
            String clusterId = var14[var9];
            com.vmware.vsan.client.util.dataservice.query.QueryResult clustersResult = result.getQueryResult(clusterId);
            if (!CollectionUtils.isEmpty(clustersResult.items)) {
               clustersResult.items.stream().filter((queryModel) -> {
                  return queryModel != null && queryModel.properties != null;
               }).forEach((queryModel) -> {
                  GuestClusterData guestClusterData = new GuestClusterData();
                  guestClusterData.id = clusterId;
                  Object namespace = queryModel.properties.get("workload");
                  if (namespace instanceof URI) {
                     guestClusterData.namespace = VapiUtils.getVapiId((URI)namespace);
                  } else if (namespace instanceof String) {
                     guestClusterData.namespace = (String)namespace;
                  } else {
                     guestClusterData.namespace = "";
                  }

                  guestClusterData.clusterData.name = (String)queryModel.properties.get("name");
                  clusterIdToData.put(clusterId, guestClusterData);
               });
            }
         }

         this.updateGuestClustersData(clusterIdToData);
         return clusterIdToData;
      }
   }

   private void updateGuestClustersData(Map clusterIdToData) {
      if (!clusterIdToData.isEmpty()) {
         QueryBuilder queryBuilder = new QueryBuilder();
         Iterator var3 = clusterIdToData.values().iterator();

         while(var3.hasNext()) {
            GuestClusterData guestClusterData = (GuestClusterData)var3.next();
            queryBuilder.newQuery(guestClusterData.id).select("primaryIconId").from(ResourcePool.class).where().propertyEquals("name", guestClusterData.namespace).join(ResourcePool.class).on("resourcePool").where().propertyEquals("name", guestClusterData.clusterData.name).end();
         }

         RequestSpec requestSpec = queryBuilder.build();
         QueryExecutorResult result = this.queryExecutor.execute(requestSpec);
         Iterator var5 = clusterIdToData.values().iterator();

         while(var5.hasNext()) {
            GuestClusterData guestClusterData = (GuestClusterData)var5.next();
            com.vmware.vsan.client.util.dataservice.query.QueryResult clustersResult = result.getQueryResult(guestClusterData.id);
            if (!CollectionUtils.isEmpty(clustersResult.items)) {
               clustersResult.items.stream().forEach((queryModel) -> {
                  guestClusterData.clusterData.primaryIconId = (String)queryModel.properties.get("primaryIconId");
                  guestClusterData.clusterData.moRef = (ManagedObjectReference)queryModel.id;
               });
            }
         }

      }
   }

   private DataServiceResponse getClustersDsProperties(ManagedObjectReference[] clustersRefs) {
      try {
         DataServiceResponse clustersProperties = QueryUtil.getProperties(clustersRefs, new String[]{"name", "primaryIconId"});
         return clustersProperties;
      } catch (Exception var3) {
         logger.error("Cannot fetch clusters' properties", var3);
         return null;
      }
   }

   @TsService
   public List getVolumeVmsData(ManagedObjectReference datastoreRef, String volumeID) throws VsanUiLocalizableException {
      try {
         VcConnection connection = this.vcClient.getConnection(datastoreRef.getServerGuid());
         Throwable var4 = null;

         List var7;
         try {
            VStorageObjectManager vStorageObjectManager = connection.getVStorageObjectManager();
            List vmDiskAssociations = this.getVolumeVmDiskAssociations(datastoreRef, volumeID, vStorageObjectManager);
            if (vmDiskAssociations.size() <= 0) {
               return new ArrayList();
            }

            var7 = this.getVmData(datastoreRef, vmDiskAssociations);
         } catch (Throwable var18) {
            var4 = var18;
            throw var18;
         } finally {
            if (connection != null) {
               if (var4 != null) {
                  try {
                     connection.close();
                  } catch (Throwable var17) {
                     var4.addSuppressed(var17);
                  }
               } else {
                  connection.close();
               }
            }

         }

         return var7;
      } catch (Exception var20) {
         logger.error("Unable to query VMs data.", var20);
         throw new VsanUiLocalizableException("vsan.cns.vms.data.error");
      }
   }

   private List getVmData(ManagedObjectReference moRef, List vmDiskAssociations) throws Exception {
      List result = new ArrayList();
      List vmRefs = new ArrayList();
      Iterator var5 = vmDiskAssociations.iterator();

      while(var5.hasNext()) {
         VmDiskAssociations vmDiskAssociation = (VmDiskAssociations)var5.next();
         ManagedObjectReference vmRef = new ManagedObjectReference(VirtualMachine.class.getSimpleName(), vmDiskAssociation.vmId, moRef.getServerGuid());
         vmRefs.add(vmRef);
      }

      String[] vmProperties = new String[]{"name", "isPodVM", "primaryIconId"};
      DataServiceResponse response = QueryUtil.getProperties((ManagedObjectReference[])vmRefs.toArray(new ManagedObjectReference[0]), vmProperties);
      Iterator var12 = response.getResourceObjects().iterator();

      while(var12.hasNext()) {
         Object resourceObject = var12.next();
         BasicVmData vmData = new BasicVmData((ManagedObjectReference)resourceObject);
         vmData.name = (String)response.getProperty(resourceObject, "name");
         vmData.isPodVM = (Boolean)response.getProperty(resourceObject, "isPodVM");
         vmData.primaryIconId = (String)response.getProperty(resourceObject, "primaryIconId");
         result.add(vmData);
      }

      Collections.sort(result, BasicVmData.COMPARATOR);
      return result;
   }

   @TsService
   public VolumeDetails getVolumeDetails(ManagedObjectReference datastoreRef, String volumeID, String fileshareName, boolean hasExtensionId) throws Exception {
      VolumeDetails result = new VolumeDetails();
      result.virtualObject.filter = DisplayObjectType.CNS_VOLUME;
      VcConnection connection = this.vcClient.getConnection(datastoreRef.getServerGuid());
      Throwable var7 = null;

      try {
         Datastore datastore = (Datastore)connection.createStub(Datastore.class, datastoreRef);
         HostMount[] hosts = datastore.getHost();
         if (ArrayUtils.isEmpty(hosts)) {
            logger.error("Datastore " + datastoreRef + " does not have any attached hosts.");
            throw new VsanUiLocalizableException();
         }

         HostSystem host = (HostSystem)connection.createStub(HostSystem.class, hosts[0].key);
         result.cluster = host.getParent();
         VmodlHelper.assignServerGuid(result.cluster, datastoreRef.getServerGuid());
         if (StringUtils.isNotEmpty(fileshareName)) {
            List fileShares = this.fileServiceConfigService.queryShare(result.cluster, fileshareName);
            if (fileShares.size() == 0) {
               logger.error("Cannot find fileshare with name: " + fileshareName);
               throw new VsanUiLocalizableException();
            }

            VsanFileServiceShare fileShare = (VsanFileServiceShare)fileShares.get(0);
            result.virtualObject = new VirtualObject(fileShare.objectUuids, DisplayObjectType.FILE_VOLUME);
            result.fileShare = new FileShareConfig(fileShare.config.domainName, fileShare.config.protocol);
            VolumeDetails var28 = result;
            return var28;
         }

         VStorageObjectManager vStorageObjectManager = connection.getVStorageObjectManager();
         VStorageObject vStorageObject = vStorageObjectManager.retrieveVStorageObject(new ID(volumeID), datastoreRef);
         if (vStorageObject.getConfig() != null && vStorageObject.getConfig().getBacking() != null) {
            BackingInfo backing = vStorageObject.getConfig().getBacking();
            if (backing instanceof DiskFileBackingInfo) {
               result.virtualObject.uuids.add(((DiskFileBackingInfo)backing).getBackingObjectId());
            }
         }

         if (result.virtualObject.uuids.isEmpty()) {
            logger.error("Unable to get vSAN object uuid of volume " + volumeID);
         } else if (!hasExtensionId) {
            List vmDiskAssociations = this.getVolumeVmDiskAssociations(datastoreRef, volumeID, vStorageObjectManager);
            if (vmDiskAssociations.size() > 0) {
               result.virtualObject.filter = DisplayObjectType.VM;
            }
         }
      } catch (Throwable var23) {
         var7 = var23;
         throw var23;
      } finally {
         if (connection != null) {
            if (var7 != null) {
               try {
                  connection.close();
               } catch (Throwable var22) {
                  var7.addSuppressed(var22);
               }
            } else {
               connection.close();
            }
         }

      }

      return result;
   }

   private List getVolumeVmDiskAssociations(ManagedObjectReference datastoreRef, String volumeID, VStorageObjectManager vStorageObjectManager) {
      RetrieveVStorageObjSpec vStorageObjSpec = new RetrieveVStorageObjSpec(new ID(volumeID), datastoreRef);
      Measure measure = new Measure("VStorageObjectManager.retrieveVStorageObjectAssociations");
      Throwable var6 = null;

      ArrayList var8;
      try {
         VStorageObjectAssociations[] vStorageObjectAssociations = vStorageObjectManager.retrieveVStorageObjectAssociations(new RetrieveVStorageObjSpec[]{vStorageObjSpec});
         if (!ArrayUtils.isEmpty(vStorageObjectAssociations)) {
            if (!ArrayUtils.isEmpty(vStorageObjectAssociations[0].vmDiskAssociations)) {
               List var21 = Arrays.asList(vStorageObjectAssociations[0].vmDiskAssociations);
               return var21;
            }

            var8 = new ArrayList();
            return var8;
         }

         var8 = new ArrayList();
      } catch (Throwable var19) {
         var6 = var19;
         throw var19;
      } finally {
         if (measure != null) {
            if (var6 != null) {
               try {
                  measure.close();
               } catch (Throwable var18) {
                  var6.addSuppressed(var18);
               }
            } else {
               measure.close();
            }
         }

      }

      return var8;
   }

   @TsService
   public QueryLabelResult queryLabels(ManagedObjectReference contextObjectRef, String key, String value) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(contextObjectRef.getServerGuid());
         Throwable var5 = null;

         QueryLabelResult var9;
         try {
            VolumeManager volumeManager = conn.getCnsVolumeManager();
            SearchLabelSpec spec = new SearchLabelSpec();
            spec.keyPrefix = StringUtils.isNotBlank(key) ? key : null;
            spec.valuePrefix = StringUtils.isNotBlank(value) ? value : null;
            spec.maxNumberOfResults = 10L;
            SearchLabelResult searchLabelResult = volumeManager.searchLabels(spec);
            var9 = QueryLabelResult.fromVmodl(searchLabelResult);
         } catch (Throwable var19) {
            var5 = var19;
            throw var19;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var18) {
                     var5.addSuppressed(var18);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var9;
      } catch (CnsFault var21) {
         logger.error("Cannot query CNS volumes labels: ", var21);
         throw new VsanUiLocalizableException("vsan.cns.query.labels.error");
      }
   }

   @TsService
   public VolumeComplianceFailure[] loadComplianceFailures(ManagedObjectReference contextObjectRef, Volume volume) throws VsanUiLocalizableException {
      List volumeComplianceFailures = new ArrayList();
      Measure measure = new Measure("Collect Compliance Failures Results");
      Throwable var7 = null;

      ComplianceResult[] complianceResults;
      CapabilityObjectSchema[] capabilityObjectSchemas;
      label171: {
         VolumeComplianceFailure[] var9;
         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, contextObjectRef).loadComplianceResults(volume).loadCapabilityObjectSchema();

            try {
               complianceResults = dataRetriever.getComplianceResults();
            } catch (Exception var25) {
               logger.error("Cannot load compliance failures.", var25);
               throw new VsanUiLocalizableException("vsan.cns.load.compliance.failures.error");
            }

            if (!ArrayUtils.isEmpty(complianceResults)) {
               try {
                  capabilityObjectSchemas = dataRetriever.getCabalityObjectSchema();
                  break label171;
               } catch (Exception var24) {
                  logger.error("Cannot load capability object schemas.", var24);
                  throw new VsanUiLocalizableException("vsan.cns.load.compliance.failures.error");
               }
            }

            var9 = (VolumeComplianceFailure[])volumeComplianceFailures.toArray(new VolumeComplianceFailure[0]);
         } catch (Throwable var26) {
            var7 = var26;
            throw var26;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var23) {
                     var7.addSuppressed(var23);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var9;
      }

      Map propertyMetadataMap = this.getMetadataFromCapabilityObjectSchema(capabilityObjectSchemas);
      if (propertyMetadataMap != null && !propertyMetadataMap.isEmpty()) {
         ComplianceResult[] var29 = complianceResults;
         int var30 = complianceResults.length;

         for(int var31 = 0; var31 < var30; ++var31) {
            ComplianceResult complianceResult = var29[var31];
            if (!ArrayUtils.isEmpty(complianceResult.violatedPolicies)) {
               PolicyStatus[] var11 = complianceResult.violatedPolicies;
               int var12 = var11.length;

               for(int var13 = 0; var13 < var12; ++var13) {
                  PolicyStatus policyStatus = var11[var13];
                  List parsedComplianceFailures = this.parseViolatedPolicyStatuses(policyStatus.getCurrentValue(), policyStatus.getExpectedValue(), propertyMetadataMap);
                  volumeComplianceFailures.addAll(parsedComplianceFailures);
               }
            }
         }

         return (VolumeComplianceFailure[])volumeComplianceFailures.toArray(new VolumeComplianceFailure[0]);
      } else {
         logger.warn("There is no property metadata and there are no labels for the keys of properties");
         return (VolumeComplianceFailure[])volumeComplianceFailures.toArray(new VolumeComplianceFailure[0]);
      }
   }

   private List parseViolatedPolicyStatuses(CapabilityInstance currentCapabilityInstance, CapabilityInstance expectedCapabilityInstance, Map namespaceCapabilityMetadata) {
      List result = new ArrayList();
      if (expectedCapabilityInstance == null) {
         logger.warn("Expected capability instance is null");
         return result;
      } else {
         ConstraintInstance[] expectedConstraints = expectedCapabilityInstance.getConstraint();
         if (expectedConstraints == null) {
            logger.warn("Expected constraints of capability instance are null");
            return result;
         } else {
            ConstraintInstance[] currentConstraints = new ConstraintInstance[0];
            if (currentCapabilityInstance != null) {
               currentConstraints = currentCapabilityInstance.getConstraint();
            }

            Map expectedValuesMap = this.parseConstraints(expectedConstraints);
            Map currentValuesMap = new HashMap();
            if (ArrayUtils.isNotEmpty(currentConstraints)) {
               currentValuesMap = this.parseConstraints(currentConstraints);
            }

            Iterator var9 = expectedValuesMap.entrySet().iterator();

            while(var9.hasNext()) {
               Entry expectedValueEntry = (Entry)var9.next();
               VolumeComplianceFailure volumeComplianceFailure = new VolumeComplianceFailure();
               volumeComplianceFailure.propertyName = (String)namespaceCapabilityMetadata.get(expectedValueEntry.getKey());
               String expectedValue = (String)expectedValueEntry.getValue();
               String currentValue = (String)((Map)currentValuesMap).get(expectedValueEntry.getKey());
               volumeComplianceFailure.currentValue = currentValue != null ? currentValue : Utils.getLocalizedString("vsan.common.na.label");
               volumeComplianceFailure.expectedValue = expectedValue != null ? expectedValue : Utils.getLocalizedString("vsan.common.na.label");
               result.add(volumeComplianceFailure);
            }

            return result;
         }
      }
   }

   private Map parseConstraints(ConstraintInstance[] constraints) {
      Map result = new HashMap();
      ConstraintInstance[] var3 = constraints;
      int var4 = constraints.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ConstraintInstance constraint = var3[var5];
         PropertyInstance[] var7 = constraint.propertyInstance;
         int var8 = var7.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            PropertyInstance propertyInstance = var7[var9];
            if (propertyInstance.value instanceof DiscreteSet) {
               Object[] constraintRawValues = ((DiscreteSet)propertyInstance.value).getValues();
               String constraintValues = "";
               if (ArrayUtils.isNotEmpty(constraintRawValues)) {
                  constraintValues = Utils.getLocalizedString("vsan.cns.compliance.failures.values", Joiner.on(",").join(constraintRawValues));
               }

               result.put(propertyInstance.id, constraintValues);
            } else {
               result.put(propertyInstance.id, propertyInstance.value.toString());
            }
         }
      }

      return result;
   }

   private Map getMetadataFromCapabilityObjectSchema(CapabilityObjectSchema[] capabilityObjectSchemas) {
      Map propertyMetadataInfos = new HashMap();
      if (ArrayUtils.isEmpty(capabilityObjectSchemas)) {
         return propertyMetadataInfos;
      } else {
         CapabilityObjectSchema[] var3 = capabilityObjectSchemas;
         int var4 = capabilityObjectSchemas.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            CapabilityObjectSchema capabilityObjectSchema = var3[var5];
            if (!ArrayUtils.isEmpty(capabilityObjectSchema.capabilityMetadataPerCategory)) {
               CapabilityObjectMetadataPerCategory[] var7 = capabilityObjectSchema.capabilityMetadataPerCategory;
               int var8 = var7.length;

               for(int var9 = 0; var9 < var8; ++var9) {
                  CapabilityObjectMetadataPerCategory capabilityObjectMetadataPerCategory = var7[var9];
                  if (!ArrayUtils.isEmpty(capabilityObjectMetadataPerCategory.capabilityMetadata)) {
                     CapabilityMetadata[] var11 = capabilityObjectMetadataPerCategory.capabilityMetadata;
                     int var12 = var11.length;

                     for(int var13 = 0; var13 < var12; ++var13) {
                        CapabilityMetadata capabilityMetadata = var11[var13];
                        if (!ArrayUtils.isEmpty(capabilityMetadata.propertyMetadata)) {
                           PropertyMetadata[] var15 = capabilityMetadata.propertyMetadata;
                           int var16 = var15.length;

                           for(int var17 = 0; var17 < var16; ++var17) {
                              PropertyMetadata propertyMetadata = var15[var17];
                              if (propertyMetadata.getSummary() != null && propertyMetadata.getId() != null && propertyMetadata.getSummary().getLabel() != null) {
                                 propertyMetadataInfos.put(propertyMetadata.getId(), propertyMetadata.getSummary().getLabel());
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return propertyMetadataInfos;
      }
   }

   @TsService
   public List getHostsDataByDatastoreRefs(ManagedObjectReference[] datastoreRefs) throws VsanUiLocalizableException {
      if (ArrayUtils.isEmpty(datastoreRefs)) {
         return new ArrayList();
      } else {
         ArrayList result = new ArrayList();

         try {
            Map hostAccessibilityMap = this.getHostsAccessibility(datastoreRefs);
            List hostsData = this.buildHostsData(hostAccessibilityMap);
            result.addAll(hostsData);
            return result;
         } catch (Exception var5) {
            logger.error("Cannot retrieve mounted hosts data: ", var5);
            throw new VsanUiLocalizableException();
         }
      }
   }

   private Map getHostsAccessibility(ManagedObjectReference[] datastoreRefs) throws Exception {
      Map hostAccessibilityMap = new HashMap();
      DataServiceResponse dataServiceResponse = QueryUtil.getProperties(datastoreRefs, new String[]{"host"});
      ManagedObjectReference[] var4 = datastoreRefs;
      int var5 = datastoreRefs.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference datastoreRef = var4[var6];
         HostMount[] hostMounts = (HostMount[])dataServiceResponse.getProperty(datastoreRef, "host");
         if (ArrayUtils.isEmpty(hostMounts)) {
            logger.warn("There are no hosts to which the datastore: " + datastoreRef.getValue() + " is mounted");
         } else {
            HostMount[] var9 = hostMounts;
            int var10 = hostMounts.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               HostMount hostMount = var9[var11];
               if (hostMount.getMountInfo() != null) {
                  hostAccessibilityMap.put(hostMount.getKey(), hostMount.getMountInfo().accessible);
               }
            }
         }
      }

      return hostAccessibilityMap;
   }

   private List buildHostsData(Map hostAccessibilityMap) throws Exception {
      List result = new ArrayList();
      if (MapUtils.isEmpty(hostAccessibilityMap)) {
         return result;
      } else {
         ManagedObjectReference[] hostMoRefs = (ManagedObjectReference[])hostAccessibilityMap.keySet().toArray(new ManagedObjectReference[0]);
         String[] hostProperties = new String[]{"name", "primaryIconId"};
         DataServiceResponse dataServiceResponse = QueryUtil.getProperties(hostMoRefs, hostProperties);
         ManagedObjectReference[] var6 = hostMoRefs;
         int var7 = hostMoRefs.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            ManagedObjectReference hostRef = var6[var8];
            String hostName = (String)dataServiceResponse.getProperty(hostRef, "name");
            String hostIconId = (String)dataServiceResponse.getProperty(hostRef, "primaryIconId");
            Boolean isDatastoreAccessibleFromHost = (Boolean)hostAccessibilityMap.get(hostRef);
            CnsDatastoreAccessibilityStatus hostAccessibility = isDatastoreAccessibleFromHost ? CnsDatastoreAccessibilityStatus.accessible : CnsDatastoreAccessibilityStatus.notAccessible;
            CnsHostData hostData = new CnsHostData(hostName, hostIconId, hostAccessibility);
            result.add(hostData);
         }

         return result;
      }
   }

   @TsService
   public ReconfigOutcome[] reapplyStoragePolicy(ManagedObjectReference contextObjectRef, Volume[] volumes) {
      Validate.notNull(contextObjectRef);
      Validate.notEmpty(volumes);

      try {
         PbmConnection pbmConn = this.pbmClient.getConnection(contextObjectRef.getServerGuid());
         Throwable var4 = null;

         try {
            ProfileManager profileManager = pbmConn.getProfileManager();
            List serverObjectRefList = new ArrayList();
            Volume[] var7 = volumes;
            int var8 = volumes.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               Volume volume = var7[var9];
               ServerObjectRef serverObjectRef = new ServerObjectRef(ObjectType.virtualDiskUUID.toString(), volume.id, contextObjectRef.getServerGuid());
               serverObjectRefList.add(serverObjectRef);
            }

            ReconfigOutcome[] var23 = profileManager.applyAssociated((ServerObjectRef[])serverObjectRefList.toArray(new ServerObjectRef[0]), false);
            return var23;
         } catch (Throwable var20) {
            var4 = var20;
            throw var20;
         } finally {
            if (pbmConn != null) {
               if (var4 != null) {
                  try {
                     pbmConn.close();
                  } catch (Throwable var19) {
                     var4.addSuppressed(var19);
                  }
               } else {
                  pbmConn.close();
               }
            }

         }
      } catch (Exception var22) {
         logger.error("Unable to reapply the storage policy", var22);
         throw new VsanUiLocalizableException("vsan.cns.policies.reapply.error");
      }
   }

   @TsService
   public ManagedObjectReference deleteVolume(ManagedObjectReference objectRef, String volumeId) {
      Validate.notNull(objectRef);
      Validate.notEmpty(volumeId);
      VsanConnection connection = this.vsanClient.getConnection(objectRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var10;
      try {
         VolumeManager volumeManager = connection.getCnsVolumeManager();
         VolumeId[] volumeIds = new VolumeId[]{new VolumeId(volumeId)};

         try {
            Measure measure = new Measure("VolumeManager.delete");
            Throwable var8 = null;

            try {
               ManagedObjectReference result = volumeManager.delete(volumeIds, true);
               VmodlHelper.assignServerGuid(result, objectRef.getServerGuid());
               var10 = result;
            } catch (Throwable var35) {
               var8 = var35;
               throw var35;
            } finally {
               if (measure != null) {
                  if (var8 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var34) {
                        var8.addSuppressed(var34);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var37) {
            logger.error("VolumeManager delete returns error: ", var37);
            throw new VsanUiLocalizableException("vsan.cns.delete.volume.error");
         }
      } catch (Throwable var38) {
         var4 = var38;
         throw var38;
      } finally {
         if (connection != null) {
            if (var4 != null) {
               try {
                  connection.close();
               } catch (Throwable var33) {
                  var4.addSuppressed(var33);
               }
            } else {
               connection.close();
            }
         }

      }

      return var10;
   }
}
