package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class DvpgData {
   public String name;
   public ManagedObjectReference existingDvpg;
}
