package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum DiskType {
   HDD,
   FLASH,
   PMEM;
}
