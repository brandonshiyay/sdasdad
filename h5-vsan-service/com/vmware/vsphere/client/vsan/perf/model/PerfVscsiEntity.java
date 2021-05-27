package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class PerfVscsiEntity {
   public Integer busId;
   public Integer position;
   public String vmdkName;
   public String deviceName;
   public int controllerKey;
}
