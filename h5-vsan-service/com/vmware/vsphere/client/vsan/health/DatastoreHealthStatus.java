package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

@TsModel
public enum DatastoreHealthStatus {
   GRAY,
   RED,
   YELLOW,
   GREEN,
   UNKNOWN;

   public static DatastoreHealthStatus parse(String status) {
      return (DatastoreHealthStatus)EnumUtils.fromStringIgnoreCase(DatastoreHealthStatus.class, status);
   }
}
