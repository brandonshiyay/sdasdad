package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class DvsSpec {
   public String name;
   public Service[] services;
   public HostAdapter[] adapters;
   public ManagedObjectReference existingDvsMor;
}
