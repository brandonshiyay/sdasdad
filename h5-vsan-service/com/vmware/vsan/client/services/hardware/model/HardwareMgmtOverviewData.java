package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtOverviewData {
   public HardwareMgmtCommonData common;
   public HardwareMgmtBootDeviceData bootDevice;

   public String toString() {
      return "HardwareMgmtOverviewData{common=" + this.common + ", bootDevice=" + this.bootDevice + '}';
   }
}
