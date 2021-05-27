package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareOverallHealth {
   public Integer total;
   public Integer issueCount;
   public String overallStatus;
}
