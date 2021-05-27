package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum ClaimOption {
   ClaimForStorage,
   ClaimForCache,
   DoNotClaim,
   Custom,
   VMFS,
   PMEM;

   public static boolean isClaimedForVsan(ClaimOption claimOption) {
      return claimOption == ClaimForStorage || claimOption == ClaimForCache;
   }
}
