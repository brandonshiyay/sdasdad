package com.vmware.vsan.client.services.stretchedcluster.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.inventory.InventoryNode;

@TsModel
public class TwoHostClusterData {
   public InventoryNode cluster;
   public InventoryNode witnessHost;

   public TwoHostClusterData(InventoryNode cluster, InventoryNode witnessHost) {
      this.cluster = cluster;
      this.witnessHost = witnessHost;
   }
}
