package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsage;
import com.vmware.vim.vsan.binding.vim.cluster.VsanWhatifCapacity;
import com.vmware.vsan.client.services.capacity.VsanCapacityBreakdownCalculator;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class WhatifCapacityData {
   public long total;
   public long free;
   public long uncommitted;

   private WhatifCapacityData(long total, long free, long uncommitted) {
      this.total = total;
      this.free = free;
      this.uncommitted = uncommitted;
   }

   public static WhatifCapacityData fromSpaceUsage(VsanSpaceUsage spaceUsage) {
      if (ArrayUtils.isEmpty(spaceUsage.getWhatifCapacities())) {
         return null;
      } else {
         VsanWhatifCapacity whatIfCapacity = spaceUsage.getWhatifCapacities()[0];
         WhatifCapacityData result = new WhatifCapacityData(whatIfCapacity.totalWhatifCapacityB, whatIfCapacity.freeWhatifCapacityB, spaceUsage.uncommittedB);
         updateSlackSpaceCapacity(result, spaceUsage);
         return result;
      }
   }

   private static void updateSlackSpaceCapacity(WhatifCapacityData whatIfData, VsanSpaceUsage spaceUsage) {
      if (spaceUsage.freeCapacityB != 0L && whatIfData.free != 0L) {
         long vsanOperationalSpace = (new VsanCapacityBreakdownCalculator(spaceUsage)).getVsanOperationalSpace();
         if (vsanOperationalSpace != 0L) {
            if (vsanOperationalSpace > spaceUsage.freeCapacityB) {
               whatIfData.free = 0L;
            } else {
               long policyRatio = spaceUsage.freeCapacityB / whatIfData.free;
               whatIfData.free = (spaceUsage.freeCapacityB - vsanOperationalSpace) / policyRatio;
            }
         }
      }
   }
}
