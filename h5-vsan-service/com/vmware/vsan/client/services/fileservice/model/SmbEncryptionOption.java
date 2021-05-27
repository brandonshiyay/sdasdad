package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import org.apache.commons.lang3.StringUtils;

@TsModel
public enum SmbEncryptionOption implements EnumWithKey {
   ENABLED("enabled"),
   DISABLED("disabled"),
   MANDATORY("mandatory");

   public static final String SMB_ENCRYPTION_KEY = "encryption";
   public final String value;

   private SmbEncryptionOption(String value) {
      this.value = value;
   }

   public static SmbEncryptionOption parse(String value) {
      return StringUtils.isBlank(value) ? null : (SmbEncryptionOption)EnumUtils.fromString(SmbEncryptionOption.class, value, ENABLED);
   }

   public String getKey() {
      return this.value;
   }
}
