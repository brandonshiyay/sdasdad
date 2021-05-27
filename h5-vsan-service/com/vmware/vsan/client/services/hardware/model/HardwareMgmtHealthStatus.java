package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum HardwareMgmtHealthStatus implements EnumWithKey {
   OK("ok"),
   CRITICAL("critical"),
   WARNING("warning"),
   UNKNOWN("VsanHardwareHealth_Unknown");

   private String value;

   private HardwareMgmtHealthStatus(String value) {
      this.value = value;
   }

   public static HardwareMgmtHealthStatus fromString(String name) {
      return (HardwareMgmtHealthStatus)EnumUtils.fromStringIgnoreCase(HardwareMgmtHealthStatus.class, name, UNKNOWN);
   }

   public String toString() {
      return this.value;
   }

   public String getKey() {
      return this.value;
   }
}
