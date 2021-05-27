package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum EvacuationEntityType {
   HOST,
   DISK_GROUP,
   CAPACITY_DISK;
}
