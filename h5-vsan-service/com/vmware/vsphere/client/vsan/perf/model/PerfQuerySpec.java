package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfQuerySpec;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;

@TsModel
public class PerfQuerySpec {
   public String entityType;
   public String entityUuid;
   public String group;
   public Long startTime;
   public Long endTime;
   public Integer interval;
   public String[] labels;

   public static VsanPerfQuerySpec toVmodl(PerfQuerySpec spec) {
      VsanPerfQuerySpec querySpec = new VsanPerfQuerySpec();
      querySpec.endTime = BaseUtils.getCalendarFromLong(spec.endTime);
      querySpec.startTime = BaseUtils.getCalendarFromLong(spec.startTime);
      querySpec.group = spec.group;
      querySpec.interval = spec.interval;
      querySpec.labels = spec.labels;
      querySpec.entityRefId = spec.entityType + ":" + spec.entityUuid;
      return querySpec;
   }
}
