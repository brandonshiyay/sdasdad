package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class ObjectWithName {
   public String objectName;
   public ManagedObjectReference object;

   public ObjectWithName(String objectName, ManagedObjectReference object) {
      this.objectName = objectName;
      this.object = object;
   }
}
