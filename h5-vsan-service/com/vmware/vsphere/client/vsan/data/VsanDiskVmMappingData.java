package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class VsanDiskVmMappingData {
   public ScsiDisk disk;
   public ManagedObjectReference[] vm;
}
