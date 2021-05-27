package com.vmware.vsphere.client.vsan.config;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.DataEfficiencyConfig;

@TsModel
public class DataEfficiencySpec {
   public boolean deduplicationState;
   public boolean compressionState;

   public DataEfficiencyConfig toVmodlSpec() {
      DataEfficiencyConfig vmodlSpec = new DataEfficiencyConfig();
      vmodlSpec.setDedupEnabled(this.deduplicationState);
      vmodlSpec.setCompressionEnabled(this.compressionState);
      return vmodlSpec;
   }
}
