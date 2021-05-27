package com.vmware.vsan.client.services.csd.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class MountedRemoteDatastore {
   public ShareableDatastore shareableDatastore;
   public boolean isLocal;
   public int registeredVMCount;

   public MountedRemoteDatastore() {
   }

   public MountedRemoteDatastore(ShareableDatastore shareableDatastore, boolean isLocal, int registeredVMCount) {
      this.shareableDatastore = shareableDatastore;
      this.isLocal = isLocal;
      this.registeredVMCount = registeredVMCount;
   }
}
