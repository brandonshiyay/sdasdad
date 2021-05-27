package com.vmware.vsan.client.services.diskGroups.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class VsanDiskMapping {
   public ScsiDisk ssd;
   public ScsiDisk[] nonSsd;

   public DiskMapping toVmodl() {
      if (this.ssd != null && !ArrayUtils.isEmpty(this.nonSsd)) {
         DiskMapping result = new DiskMapping();
         result.ssd = this.ssd;
         result.nonSsd = this.nonSsd;
         return result;
      } else {
         return null;
      }
   }
}
