package com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.target;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class InitiatorGroupTargetRemoveSpec {
   public String initiatorGroupName;
   public String targetAlias;
}
