package com.vmware.vsan.client.services.common.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

@TsModel
public enum StorageCompliance {
   outOfDate,
   compliant,
   nonCompliant,
   notApplicable,
   unknown;

   public static StorageCompliance fromName(String value) {
      return (StorageCompliance)EnumUtils.fromString(StorageCompliance.class, value);
   }
}
