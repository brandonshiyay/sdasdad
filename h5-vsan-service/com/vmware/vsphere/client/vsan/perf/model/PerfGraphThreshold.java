package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDiagnosticsThreshold;
import com.vmware.vsan.client.util.NumberUtils;

@TsModel
public class PerfGraphThreshold {
   public String entityType;
   public String metric;
   public Integer yellow;
   public Integer red;
   public PerfGraphThresholdDirection direction;

   public static PerfGraphThreshold create(VsanDiagnosticsThreshold vsanDiagnosticsThreshold) {
      PerfGraphThreshold result = new PerfGraphThreshold();
      result.entityType = vsanDiagnosticsThreshold.entityType;
      result.metric = vsanDiagnosticsThreshold.metric;
      result.red = NumberUtils.toInt(vsanDiagnosticsThreshold.red);
      result.yellow = NumberUtils.toInt(vsanDiagnosticsThreshold.yellow);
      return result;
   }

   public static VsanDiagnosticsThreshold toVsanDiagnosticsThreshold(PerfGraphThreshold threshold) {
      VsanDiagnosticsThreshold result = new VsanDiagnosticsThreshold();
      result.entityType = threshold.entityType;
      result.metric = threshold.metric;
      result.red = threshold.red;
      result.yellow = threshold.yellow;
      return result;
   }
}
