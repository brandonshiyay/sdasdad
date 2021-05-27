package com.vmware.vsan.client.services.csd;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.csd.model.VmCsdConfig;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class CsdVmService {
   private static final String COMPUTE_CLUSTER_PROPS = "compute-cluster-props";
   private static final String STORAGE_CLUSTER_PROPS = "storage-cluster-props";
   @Autowired
   private QueryExecutor queryExecutor;

   @TsService
   public VmCsdConfig getVmCsdConfig(ManagedObjectReference vmRef) {
      Validate.notNull(vmRef);
      Map clustersData = this.getClustersData(vmRef);
      DataServiceResponse computeClusterProps = (DataServiceResponse)clustersData.get("compute-cluster-props");
      DataServiceResponse storageClusterProps = (DataServiceResponse)clustersData.get("storage-cluster-props");
      InventoryNode computeCluster = this.getComputeCluster(computeClusterProps);
      Set storageClusters = this.getStorageClusters(storageClusterProps);
      if (storageClusters.isEmpty() && !VsanCapabilityUtils.isCsdSupported(computeCluster.moRef)) {
         storageClusters.add(computeCluster);
      }

      return new VmCsdConfig(computeCluster, storageClusters);
   }

   private Set getStorageClusters(DataServiceResponse storageClusterProps) {
      Set storageClusters = new LinkedHashSet();
      if (storageClusterProps != null && !storageClusterProps.getResourceObjects().isEmpty()) {
         Set storageClusterRefs = storageClusterProps.getResourceObjects();
         if (CollectionUtils.isEmpty(storageClusterRefs)) {
            return storageClusters;
         } else {
            Iterator var4 = storageClusterRefs.iterator();

            while(var4.hasNext()) {
               ManagedObjectReference storageClusterRef = (ManagedObjectReference)var4.next();
               storageClusters.add(this.createInventoryNode(storageClusterRef, storageClusterProps));
            }

            return storageClusters;
         }
      } else {
         return storageClusters;
      }
   }

   private InventoryNode getComputeCluster(DataServiceResponse computeClusterProps) {
      if (computeClusterProps != null && !computeClusterProps.getResourceObjects().isEmpty()) {
         Set computeClusterRefs = computeClusterProps.getResourceObjects();
         ManagedObjectReference computeClusterRef = (ManagedObjectReference)computeClusterRefs.iterator().next();
         return this.createInventoryNode(computeClusterRef, computeClusterProps);
      } else {
         throw new IllegalStateException("No compute resource found for the given VM!");
      }
   }

   private Map getClustersData(ManagedObjectReference vmRef) {
      QueryBuilder queryBuilder = new QueryBuilder();
      queryBuilder.newQuery("compute-cluster-props").select("name", "primaryIconId").from(vmRef).join(ClusterComputeResource.class).on("cluster").end();
      if (VsanCapabilityUtils.isCsdSupportedOnVC(vmRef)) {
         queryBuilder.newQuery("storage-cluster-props").select("name", "primaryIconId").from(vmRef).join(Datastore.class).on("datastore").join(HostSystem.class).on("serverHosts").join(ClusterComputeResource.class).on("cluster").end();
      }

      Measure measure = new Measure("DataService[" + vmRef + "] - cluster data");
      Throwable var4 = null;

      Map var5;
      try {
         var5 = this.queryExecutor.execute(queryBuilder.build()).getDataServiceResponses();
      } catch (Throwable var14) {
         var4 = var14;
         throw var14;
      } finally {
         if (measure != null) {
            if (var4 != null) {
               try {
                  measure.close();
               } catch (Throwable var13) {
                  var4.addSuppressed(var13);
               }
            } else {
               measure.close();
            }
         }

      }

      return var5;
   }

   private InventoryNode createInventoryNode(ManagedObjectReference clusterRef, DataServiceResponse props) {
      String name = (String)props.getProperty(clusterRef, "name");
      String icon = (String)props.getProperty(clusterRef, "primaryIconId");
      return new InventoryNode(clusterRef, name, icon);
   }
}
