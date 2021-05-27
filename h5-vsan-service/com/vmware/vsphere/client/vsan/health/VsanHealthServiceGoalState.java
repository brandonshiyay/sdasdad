package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VsanHealthServiceGoalState {
   uninstalled,
   installed,
   enabled,
   unknown;
}
