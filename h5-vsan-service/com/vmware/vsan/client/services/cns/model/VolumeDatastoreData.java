package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class VolumeDatastoreData {
   public String name;
   public ManagedObjectReference reference;
}
