package com.vmware.vsan.client.services.diskGroups.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;

@TsModel
public class RemoveDiskSpec {
   public DecommissionMode decommissionMode;
   public ScsiDisk[] disks;
}
