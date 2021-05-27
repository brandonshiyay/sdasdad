package com.vmware.vsan.client.services.health.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHistoricalHealthConfig;

@TsModel
public class HistoricalHealthConfig {
   public boolean isEnabled;

   public HistoricalHealthConfig() {
   }

   public HistoricalHealthConfig(VsanHistoricalHealthConfig vmodl) {
      this.isEnabled = vmodl.enabled;
   }

   public VsanHistoricalHealthConfig toVmodl() {
      VsanHistoricalHealthConfig result = new VsanHistoricalHealthConfig();
      result.setEnabled(this.isEnabled);
      return result;
   }

   public String toString() {
      return "HistoricalHealthConfig(isEnabled=" + this.isEnabled + ")";
   }
}
