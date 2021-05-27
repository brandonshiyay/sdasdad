package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum PerfDiagnosticType {
   eval,
   tput,
   iops,
   lat;
}
