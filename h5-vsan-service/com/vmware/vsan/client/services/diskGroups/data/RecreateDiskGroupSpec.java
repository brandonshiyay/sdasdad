package com.vmware.vsan.client.services.diskGroups.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class RecreateDiskGroupSpec {
   public VsanDiskMapping mapping;
   public DecommissionMode decommissionMode;
}
