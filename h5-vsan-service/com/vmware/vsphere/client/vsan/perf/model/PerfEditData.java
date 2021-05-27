package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class PerfEditData {
   public boolean isPerformanceEnabled;
   public boolean isFileAnalyticsEnabled;
   public PerfStatsObjectInfo perfStatsObjectInfo;
   public String policyId;

   public String toString() {
      return "PerfEditData(isPerformanceEnabled=" + this.isPerformanceEnabled + ", isFileAnalyticsEnabled=" + this.isFileAnalyticsEnabled + ", perfStatsObjectInfo=" + this.perfStatsObjectInfo + ", policyId=" + this.policyId + ")";
   }
}
