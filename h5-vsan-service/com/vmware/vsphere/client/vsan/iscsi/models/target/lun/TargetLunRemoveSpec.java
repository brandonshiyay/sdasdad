package com.vmware.vsphere.client.vsan.iscsi.models.target.lun;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class TargetLunRemoveSpec {
   public String targetAlias;
   public Integer[] targetLunIds;
}
