package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtBootDeviceData {
   public String name;
   public String bootMode;
   public String errorMessage;

   public String toString() {
      return "HardwareMgmtBootDeviceData{name='" + this.name + "', bootMode=" + this.bootMode + ", errorMessage='" + this.errorMessage + "'}";
   }
}
