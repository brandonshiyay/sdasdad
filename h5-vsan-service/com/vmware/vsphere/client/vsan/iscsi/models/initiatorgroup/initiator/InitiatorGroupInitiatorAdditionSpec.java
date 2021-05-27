package com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.initiator;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class InitiatorGroupInitiatorAdditionSpec {
   public String initiatorGroupName;
   public String[] initiatorNames;
}
