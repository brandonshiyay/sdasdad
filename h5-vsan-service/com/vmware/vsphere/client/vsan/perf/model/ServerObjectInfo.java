package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class ServerObjectInfo {
   public String name;
   public String vsanUuid;
   public boolean isCluster;
   public String icon;
}
