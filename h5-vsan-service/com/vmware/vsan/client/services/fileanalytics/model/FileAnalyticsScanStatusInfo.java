package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Date;

@TsModel
public class FileAnalyticsScanStatusInfo {
   public long duration;
   public Date lastScanTime;

   public FileAnalyticsScanStatusInfo() {
   }

   public FileAnalyticsScanStatusInfo(long duration, Date lastScanTime) {
      this.duration = duration;
      this.lastScanTime = lastScanTime;
   }

   public String toString() {
      return "FileAnalyticsScanStatusInfo(duration=" + this.duration + ", lastScanTime=" + this.lastScanTime + ")";
   }
}
