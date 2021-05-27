package com.vmware.vsan.client.services.common.data;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public class DatastoreIdentifier {
   public ManagedObjectReference moRef;
   public String containerId;

   public DatastoreIdentifier(ManagedObjectReference moRef, String containerId) {
      this.moRef = moRef;
      this.containerId = containerId;
   }
}
