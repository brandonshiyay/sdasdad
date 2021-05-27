package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VMCreationTestData {
   public String hostName;
   public VsanVMCreationStatus state;
   public Exception fault;
}
