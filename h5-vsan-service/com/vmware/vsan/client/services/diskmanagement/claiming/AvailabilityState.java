package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum AvailabilityState {
   IN_USE_BY_VSAN,
   ONLY_MANAGED_BY_VSAN,
   ELIGIBLE,
   INELIGIBLE;
}
