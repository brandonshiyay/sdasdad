package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareSnapshotQuerySpec;

@TsModel
public class VsanFileShareSnapshotQuerySpec {
   private static final int SNAPSHOT_PAGE_SIZE = 32;
   public String shareUuid;
   public int pageIndex;

   public FileShareSnapshotQuerySpec toVmodl() {
      FileShareSnapshotQuerySpec spec = new FileShareSnapshotQuerySpec();
      spec.shareUuid = this.shareUuid;
      spec.pageNumber = this.pageIndex;
      spec.pageSize = 32;
      return spec;
   }
}
