package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum AffinitySiteLocation implements EnumWithKey {
   NONE("None"),
   PREFERRED_DOMAIN("Preferred"),
   NON_PREFERRED_DOMAIN("NonPreferred");

   public String vmodl;

   private AffinitySiteLocation(String vmodl) {
      this.vmodl = vmodl;
   }

   public String toVmodl() {
      return this.vmodl;
   }

   public static AffinitySiteLocation parse(String vmodl) {
      return (AffinitySiteLocation)EnumUtils.fromString(AffinitySiteLocation.class, vmodl, NONE);
   }

   public String getKey() {
      return this.vmodl;
   }
}
