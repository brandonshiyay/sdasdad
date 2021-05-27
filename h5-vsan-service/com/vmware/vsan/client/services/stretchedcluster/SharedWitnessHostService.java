package com.vmware.vsan.client.services.stretchedcluster;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ClusterRuntimeInfo;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessHostClusterData;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessLimits;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SharedWitnessHostService {
   private static final Log logger = LogFactory.getLog(SharedWitnessHostService.class);
   @Autowired
   private SharedWitnessHelper sharedWitnessHelper;

   @TsService
   public List getWitnessHostData(ManagedObjectReference hostRef) {
      ClusterRuntimeInfo[] clustersInfo = this.sharedWitnessHelper.getWitnessHostClustersInfo(hostRef);
      if (ArrayUtils.isEmpty(clustersInfo)) {
         return null;
      } else {
         DataServiceResponse clusterProperties = this.getClusterDsProperties(clustersInfo, hostRef.getServerGuid());
         return this.prepareResult(clustersInfo, clusterProperties);
      }
   }

   private DataServiceResponse getClusterDsProperties(ClusterRuntimeInfo[] clustersInfo, String serverGuid) {
      ManagedObjectReference[] clusterRefs = (ManagedObjectReference[])Arrays.stream(clustersInfo).map((ci) -> {
         return VmodlHelper.assignServerGuid(ci.cluster, serverGuid);
      }).toArray((x$0) -> {
         return new ManagedObjectReference[x$0];
      });

      try {
         return QueryUtil.getProperties(clusterRefs, new String[]{"name", "primaryIconId"});
      } catch (Exception var5) {
         logger.error("Cannot fetch clusters' properties", var5);
         throw new VsanUiLocalizableException();
      }
   }

   private List prepareResult(ClusterRuntimeInfo[] clustersInfo, DataServiceResponse clusterProperties) {
      List result = new ArrayList();
      ClusterRuntimeInfo[] var4 = clustersInfo;
      int var5 = clustersInfo.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ClusterRuntimeInfo clusterInfo = var4[var6];
         SharedWitnessHostClusterData witnessHostClusterData = new SharedWitnessHostClusterData();
         InventoryNode clusterData = new InventoryNode(clusterInfo.cluster, (String)clusterProperties.getProperty(clusterInfo.cluster, "name"), (String)clusterProperties.getProperty(clusterInfo.cluster, "primaryIconId"));
         witnessHostClusterData.cluster = clusterData;
         witnessHostClusterData.componentsNumber = clusterInfo.totalComponentsCount;
         result.add(witnessHostClusterData);
      }

      return result;
   }

   @TsService
   public SharedWitnessLimits getSharedWitnessLimits(ManagedObjectReference hostRef) {
      return this.sharedWitnessHelper.getSharedWitnessLimits(hostRef);
   }
}
