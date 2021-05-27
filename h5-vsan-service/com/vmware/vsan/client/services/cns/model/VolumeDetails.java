package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class VolumeDetails {
   public ManagedObjectReference cluster;
   public VirtualObject virtualObject = new VirtualObject();
   public FileShareConfig fileShare = new FileShareConfig();
}
