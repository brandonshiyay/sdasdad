package com.vmware.vsphere.client.vsan.health.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HealthCapabilityData {
   public boolean isCloudHealthSupported;
   public boolean isSilentCheckSupported;
   public boolean isCeipServiceEnabled;
   public boolean isHistoricalHealthSupported;
   public boolean isHealthTaskSupported;
}
