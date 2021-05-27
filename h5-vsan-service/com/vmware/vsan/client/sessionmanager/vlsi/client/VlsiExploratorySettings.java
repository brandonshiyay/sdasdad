package com.vmware.vsan.client.sessionmanager.vlsi.client;

import java.util.UUID;

public class VlsiExploratorySettings {
   private final VlsiSettings vlsiSettings;
   private final UUID serviceUuid;

   public VlsiExploratorySettings(VlsiSettings vlsiSettings, UUID serviceUuid) {
      this.vlsiSettings = vlsiSettings;
      this.serviceUuid = serviceUuid;
   }

   public VlsiSettings getSettings() {
      return this.vlsiSettings;
   }

   public UUID getServiceUuid() {
      return this.serviceUuid;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof VlsiExploratorySettings)) {
         return false;
      } else {
         VlsiExploratorySettings that = (VlsiExploratorySettings)o;
         if (!this.vlsiSettings.equals(that.vlsiSettings)) {
            return false;
         } else {
            return this.serviceUuid.equals(that.serviceUuid);
         }
      }
   }

   public int hashCode() {
      int result = this.vlsiSettings.hashCode();
      result = 31 * result + this.serviceUuid.hashCode();
      return result;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder("VlsiExploratorySettings{");
      sb.append("serviceSettingsTemplate=").append(this.vlsiSettings);
      sb.append(", serviceUuid=").append(this.serviceUuid);
      sb.append('}');
      return sb.toString();
   }
}
