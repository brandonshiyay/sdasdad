package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum DiskStatusType {
   VSAN,
   VSAN_DIRECT_SCSI_LUN,
   NOT_CLAIMED_SCSI_LUN,
   PMEM;
}
