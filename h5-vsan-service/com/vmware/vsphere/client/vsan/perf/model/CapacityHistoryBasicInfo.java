package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.Map;

@TsModel
public class CapacityHistoryBasicInfo {
   public Map entityTypes;
   public boolean isPerformanceServiceEnabled;
   public boolean hasEditPolicyPermission;
   public ManagedObjectReference clusterRef;
   public boolean isEmptyCluster;
}
