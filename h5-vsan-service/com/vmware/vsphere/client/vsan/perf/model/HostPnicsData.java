package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class HostPnicsData {
   public String hostName;
   public List pnics;
}
