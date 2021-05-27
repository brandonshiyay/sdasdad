package com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class InitiatorGroupAdditionSpec {
   public String initiatorGroupName;
   public String[] initiatorNames;
}
