package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.config.CapacityReservationConfig;
import java.util.Map;

@TsModel
public class CapacityManagementData {
   public CapacityReservationConfig capacityReservationConfig;
   public Map allDatastoresCapacityData;
   public Map customizableAlertThresholds;
   public Map vSanDatastoreCapacityThresholdsData;

   public CapacityManagementData() {
   }

   public CapacityManagementData(CapacityReservationConfig capacityReservationConfig, Map allDatastoresCapacityData, Map customizableAlertThresholds, Map vSanDatastoreCapacityThresholdsData) {
      this.capacityReservationConfig = capacityReservationConfig;
      this.allDatastoresCapacityData = allDatastoresCapacityData;
      this.customizableAlertThresholds = customizableAlertThresholds;
      this.vSanDatastoreCapacityThresholdsData = vSanDatastoreCapacityThresholdsData;
   }

   public String toString() {
      return "CapacityManagementData(capacityReservationConfig=" + this.capacityReservationConfig + ", allDatastoresCapacityData=" + this.allDatastoresCapacityData + ", customizableAlertThresholds=" + this.customizableAlertThresholds + ", vSanDatastoreCapacityThresholdsData=" + this.vSanDatastoreCapacityThresholdsData + ")";
   }
}
