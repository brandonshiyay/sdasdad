package com.vmware.vsan.client.services.csd.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import java.util.Set;

@TsModel
public class VmCsdConfig {
   public Boolean isRemote;
   public boolean hasStorageClusterReadAccess;
   public InventoryNode computeCluster;
   public Set storageClusters;

   public VmCsdConfig() {
   }

   public VmCsdConfig(InventoryNode computeCluster, Set storageClusters) {
      this.computeCluster = computeCluster;
      this.storageClusters = storageClusters;
      if (storageClusters.isEmpty()) {
         this.isRemote = null;
         this.hasStorageClusterReadAccess = false;
      } else {
         this.isRemote = storageClusters.size() > 1 || !computeCluster.moRef.equals(((InventoryNode)storageClusters.iterator().next()).moRef);
         this.hasStorageClusterReadAccess = true;
      }
   }
}
