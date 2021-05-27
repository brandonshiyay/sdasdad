package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum VsanFileServiceShareNetPermissionType implements EnumWithKey {
   READ_ONLY("READ_ONLY"),
   READ_WRITE("READ_WRITE"),
   NO_ACCESS("NO_ACCESS");

   public final String value;

   private VsanFileServiceShareNetPermissionType(String value) {
      this.value = value;
   }

   public static VsanFileServiceShareNetPermissionType parse(String value) {
      return (VsanFileServiceShareNetPermissionType)EnumUtils.fromString(VsanFileServiceShareNetPermissionType.class, value);
   }

   public String getKey() {
      return this.value;
   }
}
