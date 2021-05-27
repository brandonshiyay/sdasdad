package com.vmware.vsan.client.services.config.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import org.apache.commons.lang3.StringUtils;

@TsModel
public enum ClusterMode implements EnumWithKey {
   NONE("Mode_None"),
   COMPUTE("Mode_Compute"),
   UNKNOWN("Mode_Unknown");

   private final String value;

   private ClusterMode(String value) {
      this.value = value;
   }

   public static ClusterMode parse(String value) {
      return StringUtils.isBlank(value) ? UNKNOWN : (ClusterMode)EnumUtils.fromString(ClusterMode.class, value, UNKNOWN);
   }

   public String getKey() {
      return this.value;
   }
}
