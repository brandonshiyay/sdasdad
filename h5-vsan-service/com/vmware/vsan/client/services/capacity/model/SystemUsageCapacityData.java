package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class SystemUsageCapacityData {
   public long totalSystemUsage;
   public long performanceMgmtObjects;
   public long fileServiceOverhead;
   public long haMetadataObject;
   public long checksumOverhead;
   public long spaceEfficiencyOverhead;
   public long transientSpace;

   public String toString() {
      return "totalSystemUsage=" + this.totalSystemUsage + ",\nperformanceMgmtObjects=" + this.performanceMgmtObjects + ",\nfileServiceOverhead=" + this.fileServiceOverhead + ",\nhaMetadataObject=" + this.haMetadataObject + ",\nchecksumOverhead=" + this.checksumOverhead + ",\nspaceEfficiencyOverhead=" + this.spaceEfficiencyOverhead + ",\ntransientSpace=" + this.transientSpace;
   }
}
