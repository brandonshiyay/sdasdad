package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum ObjectHealthPolicyState implements EnumWithKey {
   PENDING("pending"),
   FAILED("failed"),
   POLICY_UNKNOWN("VsanObjectHealthPolicyApplicationState_Unknown");

   private String key;

   private ObjectHealthPolicyState(String key) {
      this.key = key;
   }

   public Object getKey() {
      return this.key;
   }
}
