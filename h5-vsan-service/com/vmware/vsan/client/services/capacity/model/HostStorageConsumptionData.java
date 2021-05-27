package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class HostStorageConsumptionData {
   public ManagedObjectReference hostRef;
   public long userCapacity = 0L;
   public long reservedCapacity = 0L;
   public long totalCapacity = 0L;
}
