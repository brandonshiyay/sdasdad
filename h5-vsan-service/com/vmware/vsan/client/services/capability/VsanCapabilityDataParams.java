package com.vmware.vsan.client.services.capability;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public class VsanCapabilityDataParams {
   public ManagedObjectReference moRef;
   public VsanCapabilityDataParams.Type cacheType;

   public VsanCapabilityDataParams(ManagedObjectReference moRef, VsanCapabilityDataParams.Type cacheType) {
      this.moRef = moRef;
      this.cacheType = cacheType;
   }

   public String toString() {
      return "VsanCapabilityDataParams(moRef=" + this.moRef + ", cacheType=" + this.cacheType + ")";
   }

   public static enum Type {
      VC,
      HOST,
      CLUSTER;
   }
}
