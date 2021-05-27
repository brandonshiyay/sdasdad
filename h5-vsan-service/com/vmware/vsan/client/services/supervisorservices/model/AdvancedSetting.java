package com.vmware.vsan.client.services.supervisorservices.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class AdvancedSetting {
   public String key;
   public String value;

   public AdvancedSetting() {
   }

   public AdvancedSetting(String key, String value) {
      this.key = key;
      this.value = value;
   }

   public String toString() {
      return "AdvancedSetting(key=" + this.key + ", value=" + this.value + ")";
   }
}
