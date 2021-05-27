package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanDataEfficiencyData {
   public long originalUsedSize;
   public long actualUsedSize;
   public boolean dedupEnabled;
}
