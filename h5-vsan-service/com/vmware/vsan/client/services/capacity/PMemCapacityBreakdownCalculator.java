package com.vmware.vsan.client.services.capacity;

import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsage;
import com.vmware.vsan.client.services.capacity.model.AlertThreshold;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.capacity.vm.PMemVmCapacityCalculator;

public class PMemCapacityBreakdownCalculator extends CapacityBreakdownCalculator {
   public PMemCapacityBreakdownCalculator(VsanSpaceUsage spaceUsage) {
      super(spaceUsage);
      this.vmCapacityCalculator = new PMemVmCapacityCalculator(this.capacityObjectsAccessor);
   }

   public AlertThreshold getCapacityThresholds() {
      AlertThreshold threshold = this.spaceUsage.capacityHealthThreshold != null ? AlertThreshold.fromVmodlInBytes(this.spaceUsage.capacityHealthThreshold, this.spaceUsage.freeCapacityB) : super.getCapacityThresholds();
      threshold.datastoreType = DatastoreType.PMEM;
      return threshold;
   }
}
