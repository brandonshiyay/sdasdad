package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum ObjectHealthIncomplianceReason implements EnumWithKey {
   OTHER("othernoncompliant"),
   REDUCED_AVAILABILITY_WITH_DURABILITY("reducedavailabilitywithdurability"),
   REDUCED_AVAILABILITY_WITHOUT_DURABILITY("reducedavailabilitywithnodurability"),
   INCOMPLIANCE_UNKNOWN("VsanObjectHealthIncomplianceReason_Unknown");

   private String key;

   private ObjectHealthIncomplianceReason(String key) {
      this.key = key;
   }

   public Object getKey() {
      return this.key;
   }
}
