package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceCommonConfig;

@TsModel
public class FileAnalyticsConfigData {
   public VsanFileServiceCommonConfig fileServiceConfig;
   public FileAnalyticsScanStatusInfo scanStatus;

   public FileAnalyticsConfigData() {
   }

   public FileAnalyticsConfigData(VsanFileServiceCommonConfig fileServiceConfig, FileAnalyticsScanStatusInfo scanStatus) {
      this.fileServiceConfig = fileServiceConfig;
      this.scanStatus = scanStatus;
   }

   public String toString() {
      return "FileAnalyticsConfigData(fileServiceConfig=" + this.fileServiceConfig + ", scanStatus=" + this.scanStatus + ")";
   }
}
