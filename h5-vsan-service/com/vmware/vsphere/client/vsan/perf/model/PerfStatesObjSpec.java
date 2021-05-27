package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class PerfStatesObjSpec {
   public ManagedObjectReference clusterRef;
   public String profileId;
   public boolean isVerboseEnabled;
   public boolean isNetworkDiagnosticModeEnabled;
}
