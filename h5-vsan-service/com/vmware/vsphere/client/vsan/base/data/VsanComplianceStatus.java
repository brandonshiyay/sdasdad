package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VsanComplianceStatus {
   COMPLIANT,
   NOT_COMPLIANT,
   NOT_APPLICABLE,
   OUT_OF_DATE,
   UNKNOWN;
}
