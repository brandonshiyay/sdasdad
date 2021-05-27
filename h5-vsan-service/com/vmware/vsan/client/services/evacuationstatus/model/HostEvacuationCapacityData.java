package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HostEvacuationCapacityData {
   public String hostName;
   public String iconId;
   public long capacityNeeded;
   public boolean isComponentLimitReached;
   public EvacuationCapacityData preOperationCapacity;
   public EvacuationCapacityData postOperationCapacity;
   public EvacuationEntityType selectedEntityType;

   public HostEvacuationCapacityData() {
   }

   public HostEvacuationCapacityData(String hostName) {
      this.hostName = hostName;
      this.preOperationCapacity = new EvacuationCapacityData();
      this.postOperationCapacity = new EvacuationCapacityData();
   }

   public HostEvacuationCapacityData(String hostName, String iconId, boolean isHostSelected) {
      this.hostName = hostName;
      this.iconId = iconId;
      if (isHostSelected) {
         this.selectedEntityType = EvacuationEntityType.HOST;
      }

      this.preOperationCapacity = new EvacuationCapacityData();
      this.postOperationCapacity = new EvacuationCapacityData();
   }

   public HostEvacuationCapacityData addVsanDirectHostCapacityData(long usedCapacity, long totalCapacity, boolean isHostSelected) {
      this.preOperationCapacity.addCapacity(usedCapacity, totalCapacity);
      if (!isHostSelected) {
         this.postOperationCapacity.addCapacity(usedCapacity, totalCapacity);
      }

      return this;
   }
}
