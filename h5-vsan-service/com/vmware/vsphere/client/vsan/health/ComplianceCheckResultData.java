package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class ComplianceCheckResultData {
   public ComplianceCheckSummary summary;
   public ComplianceCheckResultObj[] details;
}
