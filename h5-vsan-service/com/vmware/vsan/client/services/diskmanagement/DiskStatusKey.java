package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum DiskStatusKey {
   VSAN_DISK_FORMAT_VERSION,
   VSAN_HEALTH_FLAG,
   SCSI_LUN_OPERATIONAL_STATE,
   IS_MOUNTED;
}
