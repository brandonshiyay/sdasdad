package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class PerfGraphMetricsData {
   public List values;
   public PerfGraphThreshold threshold;
   public String key;
   public String subKey;
}
