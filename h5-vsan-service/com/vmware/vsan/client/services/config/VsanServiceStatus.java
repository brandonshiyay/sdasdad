package com.vmware.vsan.client.services.config;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VsanServiceStatus {
   ENABLED,
   DISABLED,
   PARTIAL,
   NOT_SUPPORTED;
}
