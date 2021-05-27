package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum ObjectHealthComplianceState implements EnumWithKey {
   COMPLIANT("compliant"),
   REMOTE_ACCESSIBLE_V2("remoteAccessible"),
   NON_COMPLIANT("noncompliant"),
   INACCESSIBLE_V2("inaccessible"),
   COMPLIANCE_UNKNOWN("VsanObjectHealthComplianceLevel_Unknown");

   private String key;

   private ObjectHealthComplianceState(String key) {
      this.key = key;
   }

   public Object getKey() {
      return this.key;
   }
}
