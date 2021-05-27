package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.LinkedList;
import java.util.List;

@TsModel
public class HardwareMgmtNicData {
   public List ports = new LinkedList();
   public HardwareMgmtDeviceData deviceData;
   public int index;
   public HardwareMgmtCommonData common;

   public String toString() {
      return "HardwareMgmtNicData{ports=" + this.ports + ", deviceData='" + this.deviceData + "', common=" + this.common + "', index=" + this.index + "}";
   }
}
