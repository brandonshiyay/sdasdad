package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class ClusterEvacuationCapacityData {
   public EvacuationCapacityData preOperationCapacity = new EvacuationCapacityData();
   public EvacuationCapacityData postOperationCapacity = new EvacuationCapacityData();
   public int warningThreshold;
   public int errorThreshold;
   public List faultDomains = new ArrayList();
   public List standaloneHosts = new ArrayList();
   public int faultDomainsNeeded;
   public String[] errorMessages;
   public PrecheckResultStatusType status;

   public void addVsanDirectClusterCapacityData(long usedCapacity, long totalCapacity, boolean isHostSelected) {
      this.preOperationCapacity.addCapacity(usedCapacity, totalCapacity);
      if (!isHostSelected) {
         this.postOperationCapacity.addCapacity(usedCapacity, totalCapacity);
      }

   }
}
