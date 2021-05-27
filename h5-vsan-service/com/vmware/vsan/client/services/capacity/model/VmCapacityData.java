package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VmCapacityData {
   public long totalVmUsage;
   public long vmdkPrimaryUsage;
   public long vmdkPolicyOverheadUsage;
   public long blockContainerPrimaryDataUsage;
   public long blockContainerPolicyOverheadUsage;
   public long swapObjectsUsage;
   public long vmMemorySnapshotUsage;
   public long homeObjectsUsage;
   public long vrSourceUsage;
   public long overReservedSpace;
   public DatastoreType datastoreType;
   public long totalVmCapacity;

   public String toString() {
      return "totalVmUsage=" + this.totalVmUsage + ",\nvmdkPrimaryUsage=" + this.vmdkPrimaryUsage + ",\nvmdkPolicyOverheadUsage=" + this.vmdkPolicyOverheadUsage + ",\nblockContainerPrimaryDataUsage=" + this.blockContainerPrimaryDataUsage + ",\nblockContainerPolicyOverheadUsage=" + this.blockContainerPolicyOverheadUsage + ",\nswapObjectsUsage=" + this.swapObjectsUsage + ",\nvmMemorySnapshotUsage=" + this.vmMemorySnapshotUsage + ",\nhomeObjectsUsage=" + this.homeObjectsUsage + ",\nvrSourceUsage=" + this.vrSourceUsage + ",\noverReservedSpace=" + this.overReservedSpace + ",\ntotalVmCapacity=" + this.totalVmCapacity + ",\ndatastoreType=" + this.datastoreType;
   }
}
