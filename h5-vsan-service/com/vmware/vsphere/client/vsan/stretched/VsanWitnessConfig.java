package com.vmware.vsphere.client.vsan.stretched;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class VsanWitnessConfig {
   public ManagedObjectReference host;
   public DiskMapping diskMapping;
   public String preferredFaultDomain;
}
