package com.vmware.vsan.client.services.summary.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityType;
import com.vmware.vsphere.client.vsan.perf.model.PerfEntityStateData;

@TsModel
public class SummaryPerformanceData {
   public static final int LAST_TWO_HOURS = 2;
   public boolean isPerfEnabled;
   public boolean isTopContributorsSupported;
   public PerfEntityStateData chartsData;
   public VsanPerfEntityType clusterDomClientEntity;
}
