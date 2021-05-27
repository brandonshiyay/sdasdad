package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;

@TsModel
public class EntityPerfStateObject {
   public String errorMessage;
   public VsanPerfEntityMetricCSV[] metrics;
}
