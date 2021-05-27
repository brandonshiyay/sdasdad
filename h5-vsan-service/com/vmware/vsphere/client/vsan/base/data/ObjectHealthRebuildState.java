package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum ObjectHealthRebuildState implements EnumWithKey {
   ACTIVE("activerebuild"),
   SCHEDULED("scheduledrebuild"),
   PAUSE("pausedrebuild"),
   NONE("norebuild"),
   REBUILD_UNKNOWN("VsanObjectHealthRebuildState_Unknown");

   private String key;

   private ObjectHealthRebuildState(String key) {
      this.key = key;
   }

   public Object getKey() {
      return this.key;
   }
}
