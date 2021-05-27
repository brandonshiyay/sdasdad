package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtStorageControllerData {
   public String operationalMode;
   public HardwareMgmtDeviceData deviceData;
   public HardwareMgmtCommonData common;

   public String toString() {
      return "HardwareMgmtStorageControllerData{, operationalMode=" + this.operationalMode + ", deviceData=" + this.deviceData + ", common=" + this.common + '}';
   }
}
