package com.vmware.vsan.client.services.diskGroups.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum DecommissionMode {
   noAction,
   ensureObjectAccessibility,
   evacuateAllData,
   ensureEnhancedDurability;
}
