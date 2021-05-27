package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class AggregatedVsanHealthSummary {
   public HardwareOverallHealth hostSummary;
   public HardwareOverallHealth physicalDiskSummary;
   public Boolean networkIssueDetected;
   public HardwareOverallHealth vmSummary;
}
