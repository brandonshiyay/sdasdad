package com.vmware.vsan.client.services.config.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VsanClusterType {
   SINGLE_SITE_CLUSTER,
   CUSTOM_FD_CLUSTER,
   TWO_HOST_VSAN_CLUSTER,
   STRETCHED_CLUSTER,
   COMPUTE_ONLY,
   NO_VSAN;
}
