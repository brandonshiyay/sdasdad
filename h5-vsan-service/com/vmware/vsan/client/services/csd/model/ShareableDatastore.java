package com.vmware.vsan.client.services.csd.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.Datastore.Summary;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.capacity.model.CapacityData;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@TsModel
public class ShareableDatastore {
   public InventoryNode datastore;
   public long capacityB;
   public long freeSpaceB;
   public InventoryNode serverCluster;
   public List clientClusters;
   public int vmCount;
   public CapacityData capacityData;

   public void setCapacityData(CapacityData capacityData) {
      this.capacityData = capacityData;
   }

   public static ShareableDatastore composeShareableDatastore(DataServiceResponse datastoresProperties, ManagedObjectReference datastoreRef, ManagedObjectReference serverCluster, List clientClustersRefs) {
      ShareableDatastore shareableDatastore = new ShareableDatastore();
      Summary datastoreSummary = (Summary)datastoresProperties.getProperty(datastoreRef, "summary");
      shareableDatastore.datastore = new InventoryNode(datastoreRef, datastoreSummary.name, (String)datastoresProperties.getProperty(datastoreRef, "primaryIconId"));
      shareableDatastore.capacityB = datastoreSummary.capacity;
      shareableDatastore.freeSpaceB = datastoreSummary.freeSpace;
      shareableDatastore.vmCount = (Integer)datastoresProperties.getProperty(datastoreRef, "vm._length");
      List requiredClusters = new ArrayList(clientClustersRefs);
      if (serverCluster != null) {
         requiredClusters.add(serverCluster);
      }

      Map clustersInventoryNodes = VsanInventoryHelper.getInventoryNodes(requiredClusters);
      List clientClusters = new ArrayList();
      Iterator var9 = clientClustersRefs.iterator();

      while(var9.hasNext()) {
         ManagedObjectReference clientClusterRef = (ManagedObjectReference)var9.next();
         clientClusters.add(clustersInventoryNodes.get(clientClusterRef));
      }

      shareableDatastore.clientClusters = clientClusters;
      shareableDatastore.serverCluster = (InventoryNode)clustersInventoryNodes.get(serverCluster);
      return shareableDatastore;
   }

   public String toString() {
      return "ShareableDatastore(datastore=" + this.datastore + ", capacityB=" + this.capacityB + ", freeSpaceB=" + this.freeSpaceB + ", serverCluster=" + this.serverCluster + ", clientClusters=" + this.clientClusters + ", vmCount=" + this.vmCount + ", capacityData=" + this.capacityData + ")";
   }
}
