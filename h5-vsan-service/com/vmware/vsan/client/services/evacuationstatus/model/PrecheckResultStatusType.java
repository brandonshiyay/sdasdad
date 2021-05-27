package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum PrecheckResultStatusType implements EnumWithKey {
   GREEN(0, "green"),
   YELLOW(1, "yellow"),
   RED(2, "red");

   private final int severity;
   public String key;

   private PrecheckResultStatusType(int val, String key) {
      this.severity = val;
      this.key = key;
   }

   public boolean isMoreSevereThan(PrecheckResultStatusType statusType) {
      if (statusType == null) {
         return true;
      } else {
         return this.severity > statusType.severity;
      }
   }

   public String getKey() {
      return this.key;
   }
}
