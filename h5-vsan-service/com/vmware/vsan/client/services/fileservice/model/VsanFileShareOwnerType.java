package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum VsanFileShareOwnerType implements EnumWithKey {
   CNS("cns"),
   USER("user"),
   UNKNOWN("unknown");

   private final String value;

   private VsanFileShareOwnerType(String value) {
      this.value = value;
   }

   public static VsanFileShareOwnerType fromVmodl(String value) {
      return (VsanFileShareOwnerType)EnumUtils.fromString(VsanFileShareOwnerType.class, value, UNKNOWN);
   }

   public String getKey() {
      return this.value;
   }
}
