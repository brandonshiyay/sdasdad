package com.vmware.vsan.client.services.resyncing.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanSyncingObjectsQuerySpec {
   public int start = 0;
   public int limit = Integer.MAX_VALUE;
   public boolean includeSummary = true;
   public String[] resyncTypes;
   public String status;
}
