package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VolumeFilterResult {
   public Volume[] volumes;
   public long total;
}
