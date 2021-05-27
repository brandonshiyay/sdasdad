package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class DiagnosticIssueEntity {
   public String recommendation;

   public DiagnosticIssueEntity() {
   }

   public DiagnosticIssueEntity(String recommendation) {
      this.recommendation = recommendation;
   }
}
