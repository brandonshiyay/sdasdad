package com.vmware.vsan.client.util;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.dataprotection.model.PscConnectionDetails;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcInfo;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutorResult;
import com.vmware.vsan.client.util.dataservice.query.QueryModel;
import com.vmware.vsan.client.util.dataservice.query.QueryResult;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class VsanInventoryHelper {
   private static final Log logger = LogFactory.getLog(VsanInventoryHelper.class);
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private QueryExecutor queryExecutor;

   public ManagedObjectReference getVmFolderOfDataCenter(ManagedObjectReference target) {
      ManagedObjectReference datacenterRef;
      VcConnection vcConnection;
      Throwable var4;
      ManagedObjectReference parentFolderRef;
      if (this.vmodlHelper.isOfType(target, ClusterComputeResource.class)) {
         vcConnection = this.vcClient.getConnection(target.getServerGuid());
         var4 = null;

         try {
            ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, target);
            parentFolderRef = cluster.getParent();
            datacenterRef = ((Folder)vcConnection.createStub(Folder.class, parentFolderRef)).getParent();
         } catch (Throwable var29) {
            var4 = var29;
            throw var29;
         } finally {
            if (vcConnection != null) {
               if (var4 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var26) {
                     var4.addSuppressed(var26);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      } else {
         datacenterRef = target;
      }

      if (this.vmodlHelper.isOfType(datacenterRef, Datacenter.class)) {
         vcConnection = this.vcClient.getConnection(target.getServerGuid());
         var4 = null;

         try {
            ManagedObjectReference rootVmFolderRef = ((Datacenter)vcConnection.createStub(Datacenter.class, datacenterRef)).getVmFolder();
            parentFolderRef = VmodlHelper.assignServerGuid(rootVmFolderRef, target.getServerGuid());
         } catch (Throwable var28) {
            var4 = var28;
            throw var28;
         } finally {
            if (vcConnection != null) {
               if (var4 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var27) {
                     var4.addSuppressed(var27);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return parentFolderRef;
      } else {
         return target;
      }
   }

   public ManagedObjectReference getVsanDatastore(ManagedObjectReference moRef) throws VsanUiLocalizableException {
      try {
         PropertyValue[] vmDatastores = QueryUtil.getPropertiesForRelatedObjects(moRef, "datastore", "datastore", new String[]{"summary.type"}).getPropertyValues();
         PropertyValue[] var3 = vmDatastores;
         int var4 = vmDatastores.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            PropertyValue datastore = var3[var5];
            if (datastore.propertyName.equals("summary.type") && datastore.value.equals("vsan")) {
               return (ManagedObjectReference)datastore.resourceObject;
            }
         }

         logger.warn("No vSAN datastore found for VM " + moRef);
         return null;
      } catch (Exception var7) {
         logger.error("Unable to retrieve vSAN Datastore.", var7);
         throw new VsanUiLocalizableException("dataproviders.spbm.datastore");
      }
   }

   public ManagedObjectReference getVsanDatastore(ManagedObjectReference moRef, String datastoreUrl) throws Exception {
      DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(moRef, "datastore", "datastore", new String[]{"summary.type", "summary.url"});
      Iterator var4 = response.getResourceObjects().iterator();

      Object dsRef;
      do {
         if (!var4.hasNext()) {
            logger.warn(String.format("No vSAN datastore with URL %s is found for moRef %s", datastoreUrl, moRef));
            return null;
         }

         dsRef = var4.next();
      } while(!"vsan".equals(response.getProperty(dsRef, "summary.type")) || !datastoreUrl.equals(response.getProperty(dsRef, "summary.url")));

      return (ManagedObjectReference)dsRef;
   }

   public ClusterComputeResource getCluster(ManagedObjectReference clusterRef, PscConnectionDetails pscDetails) {
      VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid(), LookupSvcInfo.from(pscDetails));
      Throwable var4 = null;

      ClusterComputeResource var5;
      try {
         var5 = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
      } catch (Throwable var14) {
         var4 = var14;
         throw var14;
      } finally {
         if (vcConnection != null) {
            if (var4 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var13) {
                  var4.addSuppressed(var13);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var5;
   }

   public List listTwoHostVsanClusters(ManagedObjectReference moRef) {
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select("name", "primaryIconId").from(ClusterComputeResource.class.getSimpleName()).where().propertyEquals("host._length", 2).and().propertyEquals("configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled", true).and().propertyEquals("serverGuid", moRef.getServerGuid()).end().build();
      QueryExecutorResult result = this.queryExecutor.execute(requestSpec);
      QueryResult twoHostClustersResult = result.getQueryResult();
      if (CollectionUtils.isEmpty(twoHostClustersResult.items)) {
         return new ArrayList();
      } else {
         List twoHostClusters = (List)twoHostClustersResult.items.stream().map((clusterResult) -> {
            return this.createClusterData(clusterResult);
         }).filter((clusterData) -> {
            return clusterData != null;
         }).collect(Collectors.toList());
         return twoHostClusters;
      }
   }

   private InventoryNode createClusterData(QueryModel queryModel) {
      if (queryModel != null && queryModel.properties != null) {
         Map props = queryModel.properties;
         InventoryNode clusterData = new InventoryNode((ManagedObjectReference)queryModel.id, (String)props.get("name"), (String)props.get("primaryIconId"));
         return clusterData;
      } else {
         logger.error("Cannot query cluster's properties");
         return null;
      }
   }

   public static Map getInventoryNodes(List moRefs) {
      if (CollectionUtils.isEmpty(moRefs)) {
         return new HashMap();
      } else {
         Map result = new HashMap();
         String[] properties = new String[]{"name", "primaryIconId"};

         DataServiceResponse dsResponse;
         try {
            dsResponse = QueryUtil.getProperties((ManagedObjectReference[])moRefs.toArray(new ManagedObjectReference[moRefs.size()]), properties);
         } catch (Exception var6) {
            logger.error("Cannot fetch objects' properties", var6);
            throw new VsanUiLocalizableException("vsan.common.generic.error");
         }

         Iterator var4 = moRefs.iterator();

         while(var4.hasNext()) {
            ManagedObjectReference moRef = (ManagedObjectReference)var4.next();
            result.put(moRef, new InventoryNode(moRef, (String)dsResponse.getProperty(moRef, "name"), (String)dsResponse.getProperty(moRef, "primaryIconId")));
         }

         return result;
      }
   }

   public static InventoryNode getInventoryNode(ManagedObjectReference moRef) {
      List moRefs = new ArrayList();
      moRefs.add(moRef);
      return (InventoryNode)getInventoryNodes(moRefs).get(moRef);
   }

   public int getNumberOfClusterHosts(ManagedObjectReference clusterRef) {
      try {
         return (Integer)QueryUtil.getProperty(clusterRef, "host._length", (Object)null);
      } catch (Exception var3) {
         logger.error("Failed to get host count for cluster: " + clusterRef, var3);
         return 0;
      }
   }
}
