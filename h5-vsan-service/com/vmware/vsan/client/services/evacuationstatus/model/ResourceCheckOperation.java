package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum ResourceCheckOperation {
   ENTER_MAINTENANCE_MODE("EnterMaintenanceMode"),
   DISK_DATA_EVACUATION("DiskDataEvacuation");

   public final String value;

   private ResourceCheckOperation(String value) {
      this.value = value;
   }
}
