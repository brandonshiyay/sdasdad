package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import org.apache.commons.lang3.StringUtils;

@TsModel
public enum VsanFileShareNfsSecurityType implements EnumWithKey {
   AUTH_SYS("SYS"),
   KRB5("KRB5"),
   KRB5I("KRB5I"),
   KRB5P("KRB5P"),
   UNKNOWN("FileShareNfsSecType_Unknown");

   public final String value;

   private VsanFileShareNfsSecurityType(String value) {
      this.value = value;
   }

   public static VsanFileShareNfsSecurityType parse(String value) {
      return StringUtils.isBlank(value) ? null : (VsanFileShareNfsSecurityType)EnumUtils.fromString(VsanFileShareNfsSecurityType.class, value, UNKNOWN);
   }

   public String getKey() {
      return this.value;
   }
}
