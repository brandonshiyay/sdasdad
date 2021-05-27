package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

@TsModel
public enum PerfGraphThresholdDirection {
   upper,
   lower;

   public static PerfGraphThresholdDirection fromVmodl(String direction) {
      return (PerfGraphThresholdDirection)EnumUtils.fromString(PerfGraphThresholdDirection.class, direction);
   }
}
