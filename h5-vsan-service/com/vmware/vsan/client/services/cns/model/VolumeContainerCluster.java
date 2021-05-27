package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class VolumeContainerCluster {
   public String name;
   public InventoryNode clusterData;
   public List persistentVolumes = new ArrayList();
   public String type;
   public ClusterFlavor flavor;
}
