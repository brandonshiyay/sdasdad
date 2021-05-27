package com.vmware.vsan.client.services.resyncing.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class RepairTimerData {
   public long minTimer;
   public long maxTimer;
   public long objectsCount;
   public long objectsCountWithRepairTimer;
   public long objectsCountPending;
   public boolean isSupported;
}
