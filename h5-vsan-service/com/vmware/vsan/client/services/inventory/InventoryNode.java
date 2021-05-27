package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class InventoryNode {
   public ManagedObjectReference moRef;
   public String name;
   public String primaryIconId;

   public InventoryNode() {
   }

   public InventoryNode(ManagedObjectReference moRef) {
      this.moRef = moRef;
   }

   public InventoryNode(ManagedObjectReference moRef, String name, String primaryIconId) {
      this.moRef = moRef;
      this.name = name;
      this.primaryIconId = primaryIconId;
   }
}
