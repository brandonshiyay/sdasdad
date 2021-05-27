package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HardwareMgmtProcessorData {
   public int id;
   public Long speedMHz;
   public int totalCores;
   public Long totalThreads;
   public HardwareMgmtCommonData common;

   public String toString() {
      return "HardwareMgmtProcessorData{id=" + this.id + ", speedMHz=" + this.speedMHz + ", totalCores=" + this.totalCores + ", totalThreads=" + this.totalThreads + ", common=" + this.common + "}";
   }
}
