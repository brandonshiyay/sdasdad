package com.vmware.vsan.client.services.obfuscation.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class ObfuscationData {
   public boolean ceipEnabled;
   public boolean obfuscationSupported;
   public String clusterVsanConfigUuid;
   public String obfuscationMap;
}
