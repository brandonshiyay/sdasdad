package com.vmware.vsphere.client.vsan.iscsi.models.target.initiator;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class TargetInitiatorRemoveSpec {
   public String targetAlias;
   public String[] targetInitiatorNames;
}
