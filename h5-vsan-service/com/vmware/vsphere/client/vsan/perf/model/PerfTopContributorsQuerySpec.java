package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfTopQuerySpec;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;

@TsModel
public class PerfTopContributorsQuerySpec {
   public PerfTopContributorsEntityType entity;
   public String metricId;
   public Long timeStamp;

   public static VsanPerfTopQuerySpec toVmodl(PerfTopContributorsQuerySpec spec) {
      VsanPerfTopQuerySpec querySpec = new VsanPerfTopQuerySpec();
      querySpec.entity = spec.entity.toVmodl();
      querySpec.metricId = spec.metricId;
      querySpec.timeStamp = BaseUtils.getCalendarFromLong(spec.timeStamp);
      return querySpec;
   }
}
