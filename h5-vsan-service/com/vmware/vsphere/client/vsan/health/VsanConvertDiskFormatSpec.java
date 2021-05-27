package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class VsanConvertDiskFormatSpec {
   public ManagedObjectReference clusterRef;
   public Boolean allowReducedRedundancy;
}
