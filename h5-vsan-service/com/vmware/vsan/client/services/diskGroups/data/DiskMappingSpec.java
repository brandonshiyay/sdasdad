package com.vmware.vsan.client.services.diskGroups.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class DiskMappingSpec {
   public ManagedObjectReference clusterRef;
   public VsanDiskMapping[] mappings;
}
