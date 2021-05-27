package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.List;
import java.util.Map;

@TsModel
public class PerfMonitorCommonPropsData {
   public ManagedObjectReference clusterRef;
   public Long currentTimeOnMasterNode;
   public Map entityTypes;
   public boolean isPerformanceServiceEnabled;
   public boolean isVerboseModeEnabled;
   public boolean hasEditPrivilege;
   public boolean isIscsiServiceEnabled;
   public boolean isFileServiceEnabled;
   public boolean isEmptyClusterForIscsi;
   public boolean isIoInsightSupported;
   public boolean isPmemManagedByVsan;
   public boolean hasIoInsightViewPrivilege;
   public boolean isTopContributorsSupported;
   public boolean isComputeOnlyCluster;
   public List mountedRemoteDatastores;
   public List networkDiagnostics;
}
