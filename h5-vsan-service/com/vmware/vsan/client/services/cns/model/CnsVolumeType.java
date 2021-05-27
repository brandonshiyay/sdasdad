package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

@TsModel
public enum CnsVolumeType {
   BLOCK,
   FILE;

   public static CnsVolumeType fromName(String value) {
      return (CnsVolumeType)EnumUtils.fromString(CnsVolumeType.class, value, BLOCK);
   }
}
