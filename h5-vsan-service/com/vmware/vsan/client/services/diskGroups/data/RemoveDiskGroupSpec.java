package com.vmware.vsan.client.services.diskGroups.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class RemoveDiskGroupSpec {
   public DecommissionMode decommissionMode;
   public VsanDiskMapping[] mappings;
}
