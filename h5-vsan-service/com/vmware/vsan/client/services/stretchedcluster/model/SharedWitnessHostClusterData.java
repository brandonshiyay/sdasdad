package com.vmware.vsan.client.services.stretchedcluster.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.inventory.InventoryNode;

@TsModel
public class SharedWitnessHostClusterData {
   public InventoryNode cluster;
   public int componentsNumber;
}
