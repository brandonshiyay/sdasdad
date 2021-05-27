package com.vmware.vsan.client.services.config;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.CapacityReservationInfo;

@TsModel
public class CapacityReservationConfig {
   public ReservationStatus hostFailureReservation;
   public ReservationStatus vsanOperationReservation;

   public CapacityReservationConfig() {
   }

   public CapacityReservationConfig(boolean hostReservation, boolean vsanSlackReservation) {
      this.hostFailureReservation = ReservationStatus.fromBoolean(hostReservation);
      this.vsanOperationReservation = ReservationStatus.fromBoolean(vsanSlackReservation);
   }

   public static CapacityReservationConfig fromVmodl(CapacityReservationInfo capacityReservationInfo) {
      if (capacityReservationInfo == null) {
         return null;
      } else {
         CapacityReservationConfig result = new CapacityReservationConfig();
         result.hostFailureReservation = ReservationStatus.fromString(capacityReservationInfo.hostRebuildThreshold);
         result.vsanOperationReservation = ReservationStatus.fromString(capacityReservationInfo.vsanOpSpaceThreshold);
         return result;
      }
   }

   public static CapacityReservationInfo toVmodl(CapacityReservationConfig reservationConfig) {
      if (reservationConfig == null) {
         return null;
      } else {
         String hostRebuildConfig = reservationConfig.hostFailureReservation.toString();
         String vSanOperationConfig = reservationConfig.vsanOperationReservation.toString();
         return hostRebuildConfig == null && vSanOperationConfig == null ? null : new CapacityReservationInfo(hostRebuildConfig, vSanOperationConfig);
      }
   }
}
