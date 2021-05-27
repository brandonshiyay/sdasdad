package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;

@TsModel
public class VirtualObjectType {
   public VsanObjectType vmodlType;
   public DisplayObjectType displayType;
   public String extendedTypeId;
   public String extendedTypeName;

   public VirtualObjectType(String vmodlType) {
      this(VsanObjectType.parse(vmodlType));
   }

   public VirtualObjectType(VsanObjectType vmodlType) {
      this.vmodlType = vmodlType;
      this.displayType = DisplayObjectType.fromVmodlType(vmodlType);
   }

   public VirtualObjectType(DisplayObjectType displayType) {
      this.displayType = displayType;
   }

   public VirtualObjectType() {
   }

   public String toString() {
      return "VirtualObjectType(vmodlType=" + this.vmodlType + ", displayType=" + this.displayType + ", extendedTypeId=" + this.extendedTypeId + ", extendedTypeName=" + this.extendedTypeName + ")";
   }
}
