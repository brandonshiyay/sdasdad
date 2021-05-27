package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class InventoryEntryData {
   public ManagedObjectReference nodeRef;
   public String name;
   public boolean isLeafNode;
   public String iconShape;
   public boolean connected;
   public boolean isDrsEnabled;
   public boolean isDisabled;
}
