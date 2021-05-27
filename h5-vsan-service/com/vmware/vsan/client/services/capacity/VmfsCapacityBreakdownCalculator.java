package com.vmware.vsan.client.services.capacity;

import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsage;
import com.vmware.vsan.client.services.capacity.model.AlertThreshold;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.capacity.vm.VmfsVmCapacityCalculator;

public class VmfsCapacityBreakdownCalculator extends CapacityBreakdownCalculator {
   public VmfsCapacityBreakdownCalculator(VsanSpaceUsage spaceUsage) {
      super(spaceUsage);
      this.vmCapacityCalculator = new VmfsVmCapacityCalculator(this.capacityObjectsAccessor);
   }

   public AlertThreshold getCapacityThresholds() {
      AlertThreshold threshold = this.spaceUsage.capacityHealthThreshold != null ? AlertThreshold.fromVmodlInBytes(this.spaceUsage.capacityHealthThreshold, this.spaceUsage.totalCapacityB) : super.getCapacityThresholds();
      threshold.datastoreType = DatastoreType.VMFS;
      return threshold;
   }
}
