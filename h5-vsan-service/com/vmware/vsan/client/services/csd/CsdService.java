package com.vmware.vsan.client.services.csd;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.AdvancedDatastoreConfig;
import com.vmware.vim.vsan.binding.vim.vsan.ClientDatastoreConfig;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.DatastoreSpec;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.ConfigInfoService;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.services.config.model.ClusterMode;
import com.vmware.vsan.client.services.csd.model.MountedRemoteDatastore;
import com.vmware.vsan.client.services.csd.model.ShareableDatastore;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutorResult;
import com.vmware.vsan.client.util.dataservice.query.QueryModel;
import com.vmware.vsan.client.util.dataservice.query.QueryResult;
import com.vmware.vsphere.client.vsan.impl.ConfigureVsanClusterMutationProvider;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CsdService {
   private static final Logger logger = LoggerFactory.getLogger(CsdService.class);
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private QueryExecutor queryExecutor;
   @Autowired
   private VsanConfigService vsanConfigService;
   @Autowired
   ConfigInfoService configInfoService;
   @Autowired
   private ConfigureVsanClusterMutationProvider configureClusterService;
   final String DATASTORES_QUERY = "Datastores";
   final String VMS_TO_DATASTORES_QUERY = "VMsToDatastores";

   @TsService
   public boolean isCsdSupported(ManagedObjectReference clusterRef) {
      if (!this.vmodlHelper.isCluster(clusterRef)) {
         return false;
      } else if (VsanCapabilityUtils.isCsdSupportedOnVC(clusterRef) && VsanCapabilityUtils.isCsdSupported(clusterRef)) {
         try {
            return (Integer)QueryUtil.getProperty(clusterRef, "host._length") != 0;
         } catch (Exception var3) {
            throw new VsanUiLocalizableException("vsan.common.generic.error");
         }
      } else {
         return false;
      }
   }

   @TsService
   public boolean isComputeOnlyCluster(ManagedObjectReference clusterRef) {
      if (clusterRef != null && this.vmodlHelper.isCluster(clusterRef)) {
         ConfigInfoEx vsanConfig = this.configInfoService.getVsanConfigInfo(clusterRef);
         return this.isComputeOnlyClusterByConfigInfoEx(vsanConfig);
      } else {
         throw new IllegalArgumentException(String.format("Expected cluster reference but found [%s]", clusterRef));
      }
   }

   public boolean isComputeOnlyClusterByConfigInfoEx(ConfigInfoEx configInfoEx) {
      return configInfoEx != null && ClusterMode.COMPUTE.getKey().equals(configInfoEx.mode);
   }

   @TsService
   public Collection getRemoteDatastoresInfos(ManagedObjectReference clusterRef) {
      try {
         return VsanInventoryHelper.getInventoryNodes(new ArrayList(this.getRemoteDatastores(clusterRef))).values();
      } catch (Exception var3) {
         throw new VsanUiLocalizableException("vsan.csd.loadMountedDatastores.error");
      }
   }

   @TsService
   public List getMountedDatastores(ManagedObjectReference clusterRef) {
      try {
         RequestSpec rs = (new QueryBuilder()).addQuery(this.getMountedDatastoresQuery(clusterRef)).addQuery(this.getVmsToDatastoresQuery(clusterRef)).build();
         QueryExecutorResult queryExecutorResult = this.queryExecutor.execute(rs);
         DataServiceResponse datastoresProperties = (DataServiceResponse)queryExecutorResult.getDataServiceResponses().get("Datastores");
         if (ArrayUtils.isEmpty(datastoresProperties.getPropertyValues())) {
            return Collections.EMPTY_LIST;
         } else {
            Map dsToVMsCount = this.getVmsCount(queryExecutorResult.getQueryResult("VMsToDatastores").getObjectToPropMap("datastore"));
            List result = new ArrayList();
            Iterator var7 = datastoresProperties.getResourceObjects().iterator();

            while(var7.hasNext()) {
               ManagedObjectReference datastoreRef = (ManagedObjectReference)var7.next();
               MountedRemoteDatastore mountedDatastore = this.getMountedDatastore(datastoreRef, clusterRef, datastoresProperties, dsToVMsCount);
               if (mountedDatastore != null) {
                  result.add(mountedDatastore);
               }
            }

            this.sortMountedRemoteDatastores(result);
            return result;
         }
      } catch (Exception var10) {
         throw new VsanUiLocalizableException("vsan.csd.loadMountedDatastores.error", var10);
      }
   }

   private Map getVmsCount(Map vmToDatastores) {
      Map dsToVMsCount = new HashMap();
      Iterator var3 = vmToDatastores.entrySet().iterator();

      while(true) {
         Entry vmToDatastoresEntry;
         do {
            if (!var3.hasNext()) {
               return dsToVMsCount;
            }

            vmToDatastoresEntry = (Entry)var3.next();
         } while(vmToDatastoresEntry.getValue() == null);

         ManagedObjectReference[] var5 = (ManagedObjectReference[])vmToDatastoresEntry.getValue();
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            ManagedObjectReference datastoreRef = var5[var7];
            dsToVMsCount.put(datastoreRef, dsToVMsCount.containsKey(datastoreRef) ? (Long)dsToVMsCount.get(datastoreRef) + 1L : 1L);
         }
      }
   }

   private MountedRemoteDatastore getMountedDatastore(ManagedObjectReference datastoreRef, ManagedObjectReference clusterRef, DataServiceResponse datastoresProperties, Map dsToVMsCount) {
      String dsContainerId = (String)datastoresProperties.getProperty(datastoreRef, "info.containerId");

      ManagedObjectReference serverCluster;
      List clientClusters;
      try {
         serverCluster = CsdUtils.getDatastoreServerCluster(datastoreRef);
         clientClusters = this.getDatastoreClientClusters(serverCluster, dsContainerId);
      } catch (Exception var9) {
         logger.warn("Failed to load server/client clusters: ", var9);
         return null;
      }

      ShareableDatastore shareableDatastore = ShareableDatastore.composeShareableDatastore(datastoresProperties, datastoreRef, serverCluster, clientClusters);
      return new MountedRemoteDatastore(shareableDatastore, clusterRef.equals(serverCluster), dsToVMsCount.get(datastoreRef) != null ? ((Long)dsToVMsCount.get(datastoreRef)).intValue() : 0);
   }

   @TsService
   public Set getClientClusters(ManagedObjectReference clusterRef) {
      RequestSpec rs = (new QueryBuilder()).addQuery(this.getMountedDatastoresQuery(clusterRef)).build();
      QueryResult queryResult = this.queryExecutor.execute(rs).getQueryResult("Datastores");
      Set result = new HashSet();
      Iterator var5 = queryResult.items.iterator();

      while(var5.hasNext()) {
         QueryModel ds = (QueryModel)var5.next();

         try {
            Map clientClusters = VsanInventoryHelper.getInventoryNodes(this.getDatastoreClientClusters(clusterRef, (String)ds.properties.get("info.containerId")));
            result.addAll(clientClusters.values());
         } catch (Exception var8) {
            logger.warn("Failed to load client clusters: ", var8);
         }
      }

      return result;
   }

   @TsService
   public ManagedObjectReference unmountRemoteDatastore(ManagedObjectReference clusterRef, ManagedObjectReference datastoreRef) {
      try {
         Set remoteDatastores = this.getRemoteDatastores(clusterRef);
         remoteDatastores.remove(datastoreRef);
         return this.setRemoteDatastores(clusterRef, remoteDatastores);
      } catch (Exception var4) {
         throw new VsanUiLocalizableException("vsan.csd.unmount.error", var4);
      }
   }

   public boolean isClusterClientOrServer(ManagedObjectReference clusterRef) {
      if (!VsanCapabilityUtils.isCsdSupported(clusterRef)) {
         return false;
      } else {
         try {
            List mountedRemoteDatastores = this.getMountedDatastores(clusterRef);
            Set clientClusters = this.getClientClusters(clusterRef);
            return mountedRemoteDatastores.stream().anyMatch((ds) -> {
               return !ds.isLocal;
            }) || clientClusters.stream().anyMatch((cluster) -> {
               return !cluster.moRef.equals(clusterRef);
            });
         } catch (Exception var4) {
            logger.warn("Could not fetch the client/server clusters!", var4);
            return false;
         }
      }
   }

   public Set getRemoteDatastores(ManagedObjectReference clusterRef) throws Exception {
      AdvancedDatastoreConfig advancedDatastoreConfig = this.getAdvancedDatastoreConfig(clusterRef);
      return (Set)((Stream)Optional.ofNullable(advancedDatastoreConfig.remoteDatastores).map(Arrays::stream).orElseGet(Stream::empty)).map((ds) -> {
         return VmodlHelper.assignServerGuid(ds, clusterRef.getServerGuid());
      }).collect(Collectors.toSet());
   }

   public ManagedObjectReference setRemoteDatastores(ManagedObjectReference clusterRef, Set remoteDatastores) {
      AdvancedDatastoreConfig advancedDatastoreConfig = this.getAdvancedDatastoreConfig(clusterRef);
      advancedDatastoreConfig.setRemoteDatastores((ManagedObjectReference[])remoteDatastores.toArray(new ManagedObjectReference[0]));
      advancedDatastoreConfig.setDatastores((DatastoreSpec[])null);
      ReconfigSpec reconfigSpec = new ReconfigSpec();
      reconfigSpec.setDatastoreConfig(advancedDatastoreConfig);
      return this.configureClusterService.startReconfigureTask(clusterRef, reconfigSpec);
   }

   private AdvancedDatastoreConfig getAdvancedDatastoreConfig(ManagedObjectReference clusterRef) {
      ConfigInfoEx clusterConfig = this.vsanConfigService.getConfigInfoEx(clusterRef);
      return clusterConfig != null && clusterConfig.datastoreConfig != null && clusterConfig.datastoreConfig instanceof AdvancedDatastoreConfig ? (AdvancedDatastoreConfig)clusterConfig.datastoreConfig : new AdvancedDatastoreConfig();
   }

   public List getDatastoreClientClusters(ManagedObjectReference dsServerClusterRef, String dsContainerId) {
      List result = new ArrayList();
      ConfigInfoEx configInfoEx = this.vsanConfigService.getConfigInfoEx(dsServerClusterRef);
      if (configInfoEx != null && configInfoEx.datastoreConfig != null && configInfoEx.datastoreConfig.datastores != null) {
         DatastoreSpec[] var5 = configInfoEx.datastoreConfig.datastores;
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            DatastoreSpec datastoreSpec = var5[var7];
            String formattedClusterUuid = CsdUtils.convertClusterUuidToDsUuidFormat(datastoreSpec.uuid);
            if (dsContainerId.equals(formattedClusterUuid) && datastoreSpec instanceof ClientDatastoreConfig) {
               ClientDatastoreConfig clientDatastoreConfig = (ClientDatastoreConfig)datastoreSpec;
               ManagedObjectReference[] var11 = clientDatastoreConfig.clusters;
               int var12 = var11.length;

               for(int var13 = 0; var13 < var12; ++var13) {
                  ManagedObjectReference clusterRef = var11[var13];
                  VmodlHelper var10001 = this.vmodlHelper;
                  result.add(VmodlHelper.assignServerGuid(clusterRef, dsServerClusterRef.getServerGuid()));
               }

               return result;
            }
         }

         return result;
      } else {
         return result;
      }
   }

   private QuerySpec getMountedDatastoresQuery(ManagedObjectReference clusterRef) {
      return (QuerySpec)(new QueryBuilder()).newQuery("Datastores").select(CsdUtils.SHAREABLE_DATASTORE_QUERY_PROPERTIES).from(clusterRef).join(Datastore.class).on("datastore").where().propertyEquals("summary.type", "vsan").end().getQueries().get(0);
   }

   private QuerySpec getVmsToDatastoresQuery(ManagedObjectReference clusterRef) {
      return (QuerySpec)(new QueryBuilder()).newQuery("VMsToDatastores").select("datastore").from(clusterRef).join(HostSystem.class).on("host").join(VirtualMachine.class).on("vm").end().getQueries().get(0);
   }

   private void sortMountedRemoteDatastores(List datastores) {
      datastores.sort((ds1, ds2) -> {
         int namesComparison = ds1.shareableDatastore.datastore.name.compareTo(ds2.shareableDatastore.datastore.name);
         return ds1.isLocal ? (ds2.isLocal ? namesComparison : -1) : (ds2.isLocal ? 1 : namesComparison);
      });
   }
}
