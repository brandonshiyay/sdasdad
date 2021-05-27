package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtPhysicalMemoryData {
   public String location;
   public long totalSizeMb;
   public long maxOperatingFrequencyMhz;
   public String memoryType;
   public HardwareMgmtCommonData common;
}
