package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum DurabilityState {
   GUARANTEED,
   EXCEEDED_COMP_LIMIT,
   NO_SPACE,
   NO_RESOURCE,
   STALE_DURABILITY_COMP,
   UNKNOWN;
}
