package com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.target;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class InitiatorGroupTargetAdditionSpec {
   public String initiatorGroupName;
   public String[] targetAliases;
}
