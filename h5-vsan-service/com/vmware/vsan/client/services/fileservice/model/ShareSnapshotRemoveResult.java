package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class ShareSnapshotRemoveResult {
   public List taskRefs;
   public List errors;
}
