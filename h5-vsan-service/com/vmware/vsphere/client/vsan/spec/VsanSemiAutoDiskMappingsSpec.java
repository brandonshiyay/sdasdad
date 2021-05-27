package com.vmware.vsphere.client.vsan.spec;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class VsanSemiAutoDiskMappingsSpec {
   public ManagedObjectReference clusterRef;
   public ManagedObjectReference hostRef;
   public VsanSemiAutoDiskSpec[] disks;
   public boolean isAllFlashSupported;
}
