package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum PerformanceObjectType {
   clusterVmConsumption,
   clusterBackend,
   clusterDomOwner,
   clusterIoInsight,
   hostBackend,
   hostVmConsumption,
   hostPnic,
   hostVnic,
   hostNet,
   hostIoInsight,
   diskGroup,
   cacheDisk,
   capacityDisk,
   vm,
   virtualDisk,
   vscsi,
   cmmds,
   clomDiskStats,
   clomHostStats;
}
