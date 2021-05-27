package com.vmware.vsan.client.services.capacity.vm;

import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSpaceSummary;
import com.vmware.vsan.client.services.capacity.CapacityObjectsAccessor;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.capacity.model.VmCapacityData;
import com.vmware.vsan.client.util.NumberUtils;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;

public class PMemVmCapacityCalculator extends VmCapacityCalculator {
   public PMemVmCapacityCalculator(CapacityObjectsAccessor capacityObjectsAccessor) {
      super(capacityObjectsAccessor);
   }

   public VmCapacityData calculate() {
      VmCapacityData result = super.calculate();
      VsanObjectSpaceSummary vmdkUsageData = this.getAny(VsanObjectType.vdisk);
      if (vmdkUsageData != null) {
         result.vmdkPrimaryUsage = NumberUtils.toLong(vmdkUsageData.usedB);
      }

      result.totalVmUsage = this.getTotalVmCapacityUsage(result);
      result.datastoreType = DatastoreType.PMEM;
      return result;
   }
}
