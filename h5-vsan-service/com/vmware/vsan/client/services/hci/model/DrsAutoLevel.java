package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum DrsAutoLevel implements EnumWithKey {
   FULLY_AUTOMATED("fullyAutomated"),
   MANUAL("manual"),
   PARTIALLY_AUTOMATED("partiallyAutomated");

   private String text;

   private DrsAutoLevel(String text) {
      this.text = text;
   }

   public String valueOf() {
      return this.text;
   }

   public static DrsAutoLevel fromString(String text) {
      return (DrsAutoLevel)EnumUtils.fromString(DrsAutoLevel.class, text);
   }

   public String getKey() {
      return this.text;
   }
}
