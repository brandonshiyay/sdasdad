package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.Date;

@TsModel
public class PerfTimeRangeData {
   public String name;
   public Date from;
   public Date to;
   public ManagedObjectReference clusterRef;
}
