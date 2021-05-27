package com.vmware.vsan.client.services.diskGroups.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class UnmountDiskGroupSpec {
   public VsanDiskMapping diskMapping;
   public DecommissionMode decommissionMode;

   public String toString() {
      return "UnmountDiskGroupSpec [diskMapping=" + this.diskMapping + ", decommissionMode=" + this.decommissionMode + "]";
   }
}
