package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VsanOperationalStatus {
   HEALTHY,
   HEALTHY_TRANSITIONAL,
   UNHEALTHY_TRANSITIONAL,
   UNHEALTHY_DISK_UNAVAILABLE;
}
