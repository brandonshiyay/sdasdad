package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class EvacuationCapacityData {
   public long totalCapacity;
   public long usedCapacity;

   public void addCapacity(long used, long total) {
      this.usedCapacity += used;
      this.totalCapacity += total;
   }
}
