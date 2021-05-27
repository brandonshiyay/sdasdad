package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanWhatIfCapacityModel {
   public boolean isWhatIfCapacitySupported;
   public long freeWhatifCapacityB;
}
