package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum PerfTopContributorsEntityType implements EnumWithKey {
   VIRTUAL_MACHINE("virtual-machine"),
   DISK_GROUP("disk-group");

   public String value;

   private PerfTopContributorsEntityType(String value) {
      this.value = value;
   }

   public String toVmodl() {
      return this.value;
   }

   public String getKey() {
      return this.value;
   }
}
