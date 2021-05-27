package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum PrecheckPersistentInstanceState {
   INACCESSIBLE,
   REDUCED_AVAILABILITY,
   REBUILD;
}
