package com.vmware.vsan.client.services.capacity.model;

public class ReducedCapacityData {
   public String[] reducedCapacityMessages;
   public boolean hasHealthyHosts;

   public ReducedCapacityData(String[] reducedCapacityMessages, boolean hasHealthyHosts) {
      this.reducedCapacityMessages = reducedCapacityMessages;
      this.hasHealthyHosts = hasHealthyHosts;
   }
}
