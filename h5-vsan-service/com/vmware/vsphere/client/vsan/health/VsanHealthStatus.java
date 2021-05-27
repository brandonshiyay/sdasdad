package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

@TsModel
public enum VsanHealthStatus {
   red,
   yellow,
   green,
   skipped,
   info,
   unknown;

   public static VsanHealthStatus parse(String value) {
      return (VsanHealthStatus)EnumUtils.fromString(VsanHealthStatus.class, value, unknown);
   }
}
