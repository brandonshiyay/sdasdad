package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class FaultDomainEvacuationCapacityData {
   public String faultDomainName;
   public String message;
   public List hostsCapacityData;
   public EvacuationCapacityData preOperationCapacity;
   public EvacuationCapacityData postOperationCapacity;
   public boolean hasInsufficientSpace;
   public boolean isAdditionalHostNeeded;
   public boolean isComponentLimitReached;

   public FaultDomainEvacuationCapacityData() {
   }

   public FaultDomainEvacuationCapacityData(String faultDomainName) {
      this.faultDomainName = faultDomainName;
      this.preOperationCapacity = new EvacuationCapacityData();
      this.postOperationCapacity = new EvacuationCapacityData();
      this.hostsCapacityData = new ArrayList();
   }

   public FaultDomainEvacuationCapacityData addVsanDirectFaultDomainCapacityData(long usedCapacity, long totalCapacity, boolean isHostSelected) {
      this.preOperationCapacity.addCapacity(usedCapacity, totalCapacity);
      if (!isHostSelected) {
         this.postOperationCapacity.addCapacity(usedCapacity, totalCapacity);
      }

      return this;
   }
}
