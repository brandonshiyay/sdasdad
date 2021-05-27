package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareSnapshot;

@TsModel
public class VsanFileShareSnapshot {
   public VsanFileShareSnapshotConfig config;
   public long creationTime;
   public long usedCapacity;

   public static VsanFileShareSnapshot fromVmodl(FileShareSnapshot vmodl) {
      VsanFileShareSnapshot snapshot = new VsanFileShareSnapshot();
      snapshot.config = VsanFileShareSnapshotConfig.fromVmodl(vmodl.config);
      snapshot.creationTime = vmodl.creationTime.getTimeInMillis();
      snapshot.usedCapacity = vmodl.usedCapacity;
      return snapshot;
   }
}
