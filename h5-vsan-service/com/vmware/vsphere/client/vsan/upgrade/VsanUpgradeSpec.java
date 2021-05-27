package com.vmware.vsphere.client.vsan.upgrade;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanUpgradeSpec {
   public boolean performObjectUpgrade;
   public boolean downgradeFormat;
   public boolean allowReducedRedundancy;
}
