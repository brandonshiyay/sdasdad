package com.vmware.vsphere.client.vsan.whatif;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class WhatIfResult {
   public WhatIfData noDataMigration;
   public WhatIfData ensureAccessibility;
   public WhatIfData fullDataMigration;
   public Boolean isWhatIfSupported;
}
