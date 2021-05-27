package com.vmware.vsphere.client.vsan.whatif;

import com.vmware.proxygen.ts.TsModel;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class WhatIfData {
   public String summary;
   public boolean success;
   public long bytesToSync;
   public long extraSpaceNeeded;
   public boolean failedDueToInaccessibleObjects;
   public boolean successWithInaccessibleOrNonCompliantObjects;
   public boolean successWithoutDataLoss;
   public List objects = new ArrayList();
   public long repairTime;
}
