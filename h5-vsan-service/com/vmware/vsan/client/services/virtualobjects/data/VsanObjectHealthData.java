package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanObjectHealthData {
   public String vsanHealthState;
   public String policyName;

   public VsanObjectHealthData(String vsanHealthState, String policyName) {
      this.vsanHealthState = vsanHealthState;
      this.policyName = policyName;
   }
}
