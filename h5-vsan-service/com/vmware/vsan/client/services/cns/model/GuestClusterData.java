package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.inventory.InventoryNode;

@TsModel
public class GuestClusterData {
   public String id;
   public String namespace;
   public InventoryNode clusterData = new InventoryNode();
}
