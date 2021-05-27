package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class PerfVirtualMachineDiskData {
   public List virtualDisks;
   public List vscsiEntities;
   public String vmUuid;
   public String entityLabelName;
}
