package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum Service implements EnumWithKey {
   MANAGEMENT("management"),
   VMOTION("vmotion"),
   VSAN("vsan");

   private String text;

   private Service(String text) {
      this.text = text;
   }

   public String getText() {
      return this.text;
   }

   public static Service fromString(String text) {
      return (Service)EnumUtils.fromString(Service.class, text);
   }

   public String getKey() {
      return this.text;
   }
}
