package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.config.SpaceEfficiencyConfig;

@TsModel
public class SpaceEfficiencyCapacityData {
   public SpaceEfficiencyConfig spaceEfficiencyConfig;
   public long usedSpaceBefore;
   public long usedSpaceAfter;
   public long savings;
   public double ratio;

   public String toString() {
      return "savings=" + this.savings + ", \nratio=" + this.ratio + ", \nusedSpaceAfter=" + this.usedSpaceAfter + ", \nusedSpaceBefore=" + this.usedSpaceBefore;
   }
}
