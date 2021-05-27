package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareSnapshotQueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class VsanFileShareSnapshotQueryResult {
   public int total;
   public List snapshots = new ArrayList();

   public static VsanFileShareSnapshotQueryResult fromVmodl(FileShareSnapshotQueryResult result) {
      VsanFileShareSnapshotQueryResult queryResult = new VsanFileShareSnapshotQueryResult();
      if (result != null && !ArrayUtils.isEmpty(result.snapshots)) {
         queryResult.total = result.totalCount;
         queryResult.snapshots = (List)Arrays.stream(result.snapshots).map(VsanFileShareSnapshot::fromVmodl).collect(Collectors.toList());
         return queryResult;
      } else {
         return queryResult;
      }
   }
}
