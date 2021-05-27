package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class ValidationData {
   public List vsanHealthChecks;
   public boolean isVsanEnabled;

   public ValidationData() {
   }

   public ValidationData(List vsanHealthChecks, boolean isVsanEnabled) {
      this.vsanHealthChecks = vsanHealthChecks;
      this.isVsanEnabled = isVsanEnabled;
   }
}
