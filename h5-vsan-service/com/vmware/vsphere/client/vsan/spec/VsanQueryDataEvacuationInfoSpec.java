package com.vmware.vsphere.client.vsan.spec;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;

@TsModel
public class VsanQueryDataEvacuationInfoSpec {
   public ScsiDisk[] disks;
}
