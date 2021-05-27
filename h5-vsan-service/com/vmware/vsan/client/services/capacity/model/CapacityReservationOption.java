package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.CapacityReservationInfo;
import com.vmware.vsan.client.services.config.ReservationStatus;

@TsModel
public enum CapacityReservationOption {
   BOTH,
   VSAN_SLACK_SPACE_ONLY,
   NONE;

   public static CapacityReservationOption fromVmodl(CapacityReservationInfo config) {
      if (!isReservationEnforced(config.hostRebuildThreshold) && !isReservationEnforced(config.vsanOpSpaceThreshold)) {
         return NONE;
      } else {
         return isReservationEnforced(config.hostRebuildThreshold) ? BOTH : VSAN_SLACK_SPACE_ONLY;
      }
   }

   private static boolean isReservationEnforced(String value) {
      return ReservationStatus.fromString(value) == ReservationStatus.ENFORCED;
   }
}
