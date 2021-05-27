package com.vmware.vsphere.client.vsan.spec;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class VsanDiskMappingSpec {
   public ManagedObjectReference clusterRef;
   public Object[] mappings;
}
