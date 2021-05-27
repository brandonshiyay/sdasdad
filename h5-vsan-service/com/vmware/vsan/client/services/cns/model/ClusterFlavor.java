package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import org.apache.commons.lang3.StringUtils;

@TsModel
public enum ClusterFlavor implements EnumWithKey {
   VANILLA("VANILLA"),
   WORKLOAD("WORKLOAD"),
   GUEST("GUEST_CLUSTER"),
   UNKNOWN("ClusterFlavor_Unknown");

   private String key;

   private ClusterFlavor(String key) {
      this.key = key;
   }

   public String getKey() {
      return this.key;
   }

   public static ClusterFlavor fromString(String text) {
      return StringUtils.isBlank(text) ? VANILLA : (ClusterFlavor)EnumUtils.fromString(ClusterFlavor.class, text, UNKNOWN);
   }
}
