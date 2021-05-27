package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class CapacityBasicInfo {
   public ManagedObjectReference clusterRef;
   public boolean isComputeOnlyCluster;
   public boolean isCsdSupported;
   public boolean isHistoricalCapacitySupported;
   public int hostCount = 0;
   public int faultyHostsTotalCount = 0;

   public String toString() {
      return "CapacityBasicInfo(clusterRef=" + this.clusterRef + ", isComputeOnlyCluster=" + this.isComputeOnlyCluster + ", isCsdSupported=" + this.isCsdSupported + ", isHistoricalCapacitySupported=" + this.isHistoricalCapacitySupported + ", hostCount=" + this.hostCount + ", faultyHostsTotalCount=" + this.faultyHostsTotalCount + ")";
   }
}
