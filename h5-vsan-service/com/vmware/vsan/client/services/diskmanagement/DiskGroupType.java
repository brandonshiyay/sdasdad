package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum DiskGroupType {
   ALL_FLASH,
   HYBRID,
   VSAN_DIRECT,
   PMEM;
}
