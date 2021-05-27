package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class ExistingDvpgData {
   public ManagedObjectReference dvpgRef;
   public String name;
   public boolean isSelected;
}
