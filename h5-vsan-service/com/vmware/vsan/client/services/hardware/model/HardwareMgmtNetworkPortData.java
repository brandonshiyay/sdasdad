package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class HardwareMgmtNetworkPortData {
   public boolean isUsedByVsan;
   public String macAddress;
   public String deviceName;
   public HardwareMgmtNetworkPortData.LinkStatus linkStatus;
   public long linkSpeedMbps;
   public HardwareMgmtHealthStatus healthStatus;
   public String errorMessage;
   public List vmNics;
   public int index;

   public String toString() {
      return "HardwareMgmtNetworkPortData{isUsedByVsan='" + this.isUsedByVsan + "', macAddress='" + this.macAddress + "', linkStatus=" + this.linkStatus + ", linkSpeedMbps=" + this.linkSpeedMbps + ", healthStatus=" + this.healthStatus + ", errorMessage='" + this.errorMessage + "', index=" + this.index + ", vmNics=" + this.vmNics + "}";
   }

   @TsModel
   public static enum LinkStatus implements EnumWithKey {
      UP("up"),
      DOWN("down"),
      UNAVAILABLE("unavailable");

      private String value;

      private LinkStatus(String value) {
         this.value = value;
      }

      public static HardwareMgmtNetworkPortData.LinkStatus fromString(String name) {
         return (HardwareMgmtNetworkPortData.LinkStatus)EnumUtils.fromStringIgnoreCase(HardwareMgmtNetworkPortData.LinkStatus.class, StringUtils.trim(name), UNAVAILABLE);
      }

      public String toString() {
         return this.value;
      }

      public String getKey() {
         return this.value;
      }
   }
}
