package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareSnapshotConfig;

@TsModel
public class VsanFileShareSnapshotConfig {
   public String name;
   public String shareUuid;

   public FileShareSnapshotConfig toVmodl() {
      FileShareSnapshotConfig snapshot = new FileShareSnapshotConfig();
      snapshot.name = this.name;
      snapshot.shareUuid = this.shareUuid;
      return snapshot;
   }

   public static VsanFileShareSnapshotConfig fromVmodl(FileShareSnapshotConfig vmodl) {
      VsanFileShareSnapshotConfig result = new VsanFileShareSnapshotConfig();
      result.name = vmodl.name;
      result.shareUuid = vmodl.shareUuid;
      return result;
   }
}
