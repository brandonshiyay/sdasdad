package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class PerformanceDiagnosticData {
   public List issues;
   public List entityRefIds;

   public PerformanceDiagnosticData() {
   }

   public PerformanceDiagnosticData(List issues, List entityRefIds) {
      this.issues = issues;
      this.entityRefIds = entityRefIds;
   }
}
