package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum PrecheckTaskType {
   HOST_PRECHECK,
   HOST_ENTER_MAINTENANCE_MODE,
   DISKGROUP_PRECHECK,
   DISKGROUP_REMOVAL,
   DISKGROUP_UNMOUNT,
   DISKGROUP_RECREATE,
   DISK_PRECHECK,
   DISK_REMOVAL;
}
