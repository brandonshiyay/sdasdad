package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtDeviceData {
   public String deviceName;
   public String displayName;
   public String vendorId;
   public String subVendorId;
   public String deviceId;
   public String subDeviceId;
   public String driverName;
   public String driverVersion;

   public String toString() {
      return "HardwareMgmtDeviceData{deviceName='" + this.deviceName + '\'' + ", displayName='" + this.displayName + '\'' + ", , vendorId='" + this.vendorId + '\'' + ", subVendorId='" + this.subVendorId + '\'' + ", deviceId='" + this.deviceId + '\'' + ", subDeviceId='" + this.subDeviceId + '\'' + ", driverName='" + this.driverName + '\'' + ", driverVersion='" + this.driverVersion + '\'' + '}';
   }
}
