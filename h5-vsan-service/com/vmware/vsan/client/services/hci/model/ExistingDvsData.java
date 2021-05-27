package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class ExistingDvsData {
   public ManagedObjectReference dvsRef;
   public String name;
   public String version;
   public String niocVersion;
   public String lacpVersion;
   public boolean isSelected;
}
