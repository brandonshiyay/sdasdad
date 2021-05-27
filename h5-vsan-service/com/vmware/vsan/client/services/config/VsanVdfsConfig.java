package com.vmware.vsan.client.services.config;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceCommonConfig;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServicePrecheckResult;

@TsModel
public class VsanVdfsConfig {
   public VsanFileServiceCommonConfig config;
   public VsanFileServicePrecheckResult precheckResult;
   public int numberOfShares;
   public String networkName;
   public String networkIconId;

   public VsanVdfsConfig() {
   }

   public VsanVdfsConfig(VsanFileServiceCommonConfig config, VsanFileServicePrecheckResult precheckResult, int numberOfShares, String networkName, String networkIconId) {
      this.config = config;
      this.precheckResult = precheckResult;
      this.numberOfShares = numberOfShares;
      this.networkName = networkName;
      this.networkIconId = networkIconId;
   }
}
