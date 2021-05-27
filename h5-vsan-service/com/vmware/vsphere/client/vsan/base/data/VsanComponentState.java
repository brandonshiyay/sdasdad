package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VsanComponentState {
   ACTIVE,
   ACTIVE_STALE,
   ABSENT,
   ABSENT_RESYNC,
   DEGRADED,
   RECONFIG,
   UNKNOWN;

   private static final int VSAN_CONFIG_INCOMPLETE = 8;

   public static VsanComponentState fromCmmdsData(int stateNumber, long bytesToSyncProp, int flagsProp) {
      switch(stateNumber) {
      case 5:
         if (flagsProp == 8) {
            return ACTIVE_STALE;
         }

         return ACTIVE;
      case 6:
         if (bytesToSyncProp > 0L) {
            return ABSENT_RESYNC;
         }

         return ABSENT;
      case 7:
      case 8:
      default:
         return UNKNOWN;
      case 9:
         return DEGRADED;
      case 10:
         return RECONFIG;
      }
   }
}
