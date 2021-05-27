package com.vmware.vsan.client.services.capacity.vm;

import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSpaceSummary;
import com.vmware.vsan.client.services.capacity.CapacityObjectsAccessor;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.capacity.model.VmCapacityData;
import com.vmware.vsan.client.util.NumberUtils;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;

public class VsanVmCapacityCalculator extends VmCapacityCalculator {
   public VsanVmCapacityCalculator(CapacityObjectsAccessor capacityObjectsAccessor) {
      super(capacityObjectsAccessor);
   }

   public VsanVmCapacityCalculator(VsanObjectSpaceSummary[] objectSpaceSummaries) {
      super(new CapacityObjectsAccessor(objectSpaceSummaries));
   }

   public VmCapacityData calculate() {
      VmCapacityData result = super.calculate();
      VsanObjectSpaceSummary vmdkUsageData = this.getAny(VsanObjectType.vdisk);
      if (vmdkUsageData != null) {
         result.vmdkPrimaryUsage = NumberUtils.toLong(vmdkUsageData.primaryCapacityB);
         result.vmdkPolicyOverheadUsage = NumberUtils.toLong(vmdkUsageData.overheadB);
      }

      result.totalVmUsage = this.getTotalVmCapacityUsage(result);
      result.datastoreType = DatastoreType.VSAN;
      return result;
   }
}
