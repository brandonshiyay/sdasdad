package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import org.apache.commons.lang3.StringUtils;

@TsModel
public enum FileOperationType implements EnumWithKey {
   READ("read"),
   WRITE("write"),
   DELETE("delete"),
   UNKNOWN("unknown");

   private final String value;

   private FileOperationType(String value) {
      this.value = value;
   }

   public static FileOperationType parse(String value) {
      return StringUtils.isBlank(value) ? UNKNOWN : (FileOperationType)EnumUtils.fromString(FileOperationType.class, value.toLowerCase(), UNKNOWN);
   }

   public String getKey() {
      return this.value;
   }
}
