package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class CnsHostData {
   public String hostName;
   public String iconId;
   public CnsDatastoreAccessibilityStatus accessibility;

   public CnsHostData(String hostName, String iconId, CnsDatastoreAccessibilityStatus accessibility) {
      this.hostName = hostName;
      this.iconId = iconId;
      this.accessibility = accessibility;
   }
}
