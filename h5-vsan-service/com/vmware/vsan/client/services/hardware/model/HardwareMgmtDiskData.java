package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtDiskData {
   public String deviceName;
   public String uuid;
   public String iconId;
   public boolean isSsd;
   public boolean isUsedByVsan;
   public boolean canBeClaimed;
   public Boolean isCapacity;
   public String diskGroupUuid;
   public boolean isLedOn;
   public boolean isLedOnSupported;
   public String interfaceType;
   public long capacityBytes;
   public HardwareMgmtStorageControllerData controller;
   public HardwareMgmtCommonData common;

   public String toString() {
      return "HardwareMgmtDiskData{deviceName='" + this.deviceName + '\'' + ", uuid='" + this.uuid + '\'' + ", iconId='" + this.iconId + '\'' + ", isSsd=" + this.isSsd + ", isUsedByVsan=" + this.isUsedByVsan + ", canBeClaimed=" + this.canBeClaimed + ", isCapacity=" + this.isCapacity + ", diskGroupUuid='" + this.diskGroupUuid + '\'' + ", isLedOn=" + this.isLedOn + ", isLedOnSupported=" + this.isLedOnSupported + ", interfaceType=" + this.interfaceType + ", capacityBytes=" + this.capacityBytes + ", controller=" + this.controller + ", common=" + this.common + '}';
   }
}
