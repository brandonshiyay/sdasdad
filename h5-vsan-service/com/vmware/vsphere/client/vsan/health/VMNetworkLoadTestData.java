package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VMNetworkLoadTestData {
   public String hostName;
   public boolean client;
   public long bandWidth;
   public long totalBytes;
}
