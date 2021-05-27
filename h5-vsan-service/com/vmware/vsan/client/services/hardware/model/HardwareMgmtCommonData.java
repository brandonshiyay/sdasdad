package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtCommonData {
   public String model;
   public String vendor;
   public String serialNumber;
   public String sku;
   public HardwareMgmtHealthStatus healthStatus;
   public String errorMessage;

   public String toString() {
      return "HardwareMgmtCommonData{model='" + this.model + '\'' + ", vendor='" + this.vendor + '\'' + ", serialNumber='" + this.serialNumber + '\'' + ", sku='" + this.sku + '\'' + ", healthStatus=" + this.healthStatus + ", errorMessage='" + this.errorMessage + '\'' + '}';
   }
}
