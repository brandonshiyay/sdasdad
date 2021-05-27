package com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.target;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class InitiatorGroupTarget {
   public String alias;
   public String iqn;

   public InitiatorGroupTarget(String targetAlias, String targetIqn) {
      this.alias = targetAlias;
      this.iqn = targetIqn;
   }
}
