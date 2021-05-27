package com.vmware.vsan.client.services.capacity.vm;

import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSpaceSummary;
import com.vmware.vsan.client.services.capacity.CapacityObjectsAccessor;
import com.vmware.vsan.client.services.capacity.model.VmCapacityData;
import com.vmware.vsan.client.util.NumberUtils;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;

public class VmCapacityCalculator {
   private CapacityObjectsAccessor capacityObjectsAccessor;

   public VmCapacityCalculator(CapacityObjectsAccessor capacityObjectsAccessor) {
      this.capacityObjectsAccessor = capacityObjectsAccessor;
   }

   public VmCapacityData calculate() {
      VmCapacityData result = new VmCapacityData();
      result.homeObjectsUsage = this.getObjectUsedCapacity(this.getAny(VsanObjectType.namespace));
      result.vrSourceUsage = this.getObjectUsedCapacity(this.getAny(VsanObjectType.hbrPersist));
      result.swapObjectsUsage = this.getObjectUsedCapacity(this.getAny(VsanObjectType.vmswap));
      result.vmMemorySnapshotUsage = this.getObjectUsedCapacity(this.getAny(VsanObjectType.vmem));
      VsanObjectSpaceSummary blockPrimaryUsageSummary = this.getAny(VsanObjectType.attachedCnsVolBlock);
      if (blockPrimaryUsageSummary != null) {
         result.blockContainerPrimaryDataUsage = NumberUtils.toLong(blockPrimaryUsageSummary.primaryCapacityB);
         result.blockContainerPolicyOverheadUsage = NumberUtils.toLong(blockPrimaryUsageSummary.overheadB);
      }

      return result;
   }

   protected long getTotalVmCapacityUsage(VmCapacityData vmCapacityData) {
      long result = 0L;
      result += vmCapacityData.vmdkPrimaryUsage;
      result += vmCapacityData.vmdkPolicyOverheadUsage;
      result += vmCapacityData.homeObjectsUsage;
      result += vmCapacityData.vrSourceUsage;
      result += vmCapacityData.swapObjectsUsage;
      result += vmCapacityData.vmMemorySnapshotUsage;
      result += vmCapacityData.blockContainerPrimaryDataUsage;
      result += vmCapacityData.blockContainerPolicyOverheadUsage;
      result += vmCapacityData.overReservedSpace;
      return result;
   }

   protected long getObjectUsedCapacity(VsanObjectSpaceSummary objectSpaceSummary) {
      return this.capacityObjectsAccessor.getObjectUsedCapacity(objectSpaceSummary);
   }

   protected VsanObjectSpaceSummary getAny(VsanObjectType type) {
      return this.capacityObjectsAccessor.getAny(type);
   }
}
