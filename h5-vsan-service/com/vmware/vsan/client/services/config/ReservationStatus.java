package com.vmware.vsan.client.services.config;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum ReservationStatus implements EnumWithKey {
   UNKNOWN("State_Unknown"),
   REPORTED("Reported"),
   ENFORCED("Enforced"),
   DISABLED("Disabled"),
   UNSUPPORTED("Unsupported");

   private String value;

   private ReservationStatus(String value) {
      this.value = value;
   }

   public String toString() {
      return this.isSupported() ? this.value : null;
   }

   public boolean isEnforced() {
      return this == ENFORCED;
   }

   public boolean isSupported() {
      return this == ENFORCED || this == REPORTED;
   }

   public static ReservationStatus fromBoolean(boolean enabled) {
      return enabled ? ENFORCED : REPORTED;
   }

   public static ReservationStatus fromString(String text) {
      return (ReservationStatus)EnumUtils.fromStringIgnoreCase(ReservationStatus.class, text, UNKNOWN);
   }

   public String getKey() {
      return this.value;
   }
}
