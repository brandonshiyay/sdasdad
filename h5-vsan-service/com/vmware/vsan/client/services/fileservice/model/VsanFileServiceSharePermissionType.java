package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum VsanFileServiceSharePermissionType implements EnumWithKey {
   READ("READ"),
   WRITE("WRITE"),
   EXECUTE("EXECUTE");

   public final String value;

   private VsanFileServiceSharePermissionType(String value) {
      this.value = value;
   }

   public static VsanFileServiceSharePermissionType parse(String value) {
      return (VsanFileServiceSharePermissionType)EnumUtils.fromString(VsanFileServiceSharePermissionType.class, value);
   }

   public String getKey() {
      return this.value;
   }
}
