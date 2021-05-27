package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HostAdapter {
   public String name;
   public String deviceName;
   public String dvsName;

   public static HostAdapter create(String name, String deviceName) {
      HostAdapter result = new HostAdapter();
      result.name = name;
      result.deviceName = deviceName;
      return result;
   }
}
