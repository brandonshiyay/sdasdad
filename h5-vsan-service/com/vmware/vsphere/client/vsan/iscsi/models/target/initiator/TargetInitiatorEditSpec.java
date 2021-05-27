package com.vmware.vsphere.client.vsan.iscsi.models.target.initiator;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class TargetInitiatorEditSpec {
   public String targetAlias;
   public String[] targetInitiatorNames;
}
