package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class PerfVirtualDiskEntity {
   public String vmdkPath;
   public String datastorePath;
   public String datastoreName;
   public String diskName;
   public int controllerKey;
}
