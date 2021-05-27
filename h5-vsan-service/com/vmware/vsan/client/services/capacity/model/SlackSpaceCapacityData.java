package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class SlackSpaceCapacityData {
   public long rebuildToleranceThreshold;
   public long rebuildToleranceReservation;
   public long operationSpaceThreshold;
   public long operationSpaceReservation;
   public boolean enforceReservationSupported;
   public long rebuildToleranceReservationAdjusted;
   public long operationSpaceReservationAdjusted;

   public String toString() {
      return "SlackSpaceCapacityData {\nrebuildToleranceThreshold=" + this.rebuildToleranceThreshold + ", \nrebuildToleranceReservation=" + this.rebuildToleranceReservation + ", \noperationSpaceThreshold=" + this.operationSpaceThreshold + ", \noperationSpaceReservation=" + this.operationSpaceReservation + ", \nenforceReservationSupported=" + this.enforceReservationSupported + "}";
   }
}
