package com.vmware.vsan.client.services.resyncing.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class DelayTimerData {
   public long delayTimer;
   public boolean isSupported;
   public String errorMessage;
}
