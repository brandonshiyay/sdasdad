package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class HardwareMgmtDiskBox {
   public String boxId;
   public String title;
   public HardwareMgmtDiskBox.Position position;
   public List diskSlots = new ArrayList();

   public String toString() {
      return "HardwareMgmtDiskBox{boxId='" + this.boxId + '\'' + ", position=" + this.position + ", diskSlots=" + this.diskSlots + "}";
   }

   @TsModel
   public static enum Position implements EnumWithKey {
      FRONT("front"),
      BACK("back"),
      MIDDLE("middle"),
      UNKNOWN("StorageEnclosurePosition_Unknown");

      private String value;

      private Position(String value) {
         this.value = value;
      }

      public static HardwareMgmtDiskBox.Position fromString(String name) {
         return (HardwareMgmtDiskBox.Position)EnumUtils.fromStringIgnoreCase(HardwareMgmtDiskBox.Position.class, name, UNKNOWN);
      }

      public String toString() {
         return this.value;
      }

      public String getKey() {
         return this.value;
      }
   }
}
