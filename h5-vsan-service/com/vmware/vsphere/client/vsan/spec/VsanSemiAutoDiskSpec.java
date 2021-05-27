package com.vmware.vsphere.client.vsan.spec;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vsphere.client.vsan.data.ClaimOption;

@TsModel
public class VsanSemiAutoDiskSpec {
   public ScsiDisk disk;
   public ClaimOption claimOption;
   public boolean markedAsFlash;

   public ClaimOption getClaimOption() {
      return this.claimOption;
   }
}
