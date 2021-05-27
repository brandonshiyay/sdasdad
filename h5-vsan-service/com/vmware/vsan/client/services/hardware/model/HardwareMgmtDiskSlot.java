package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtDiskSlot {
   public Integer index;
   public HardwareMgmtDiskData disk;

   public String toString() {
      return "HardwareMgmtDiskBox{index='" + this.index + '\'' + ", disk=" + this.disk + "}'";
   }
}
