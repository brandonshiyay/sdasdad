package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class ExternalProxySettingsConfig {
   public boolean isAutoDiscovered;
   public String hostName;
   public Integer port;
   public String userName;
   public String password;
   public Boolean enableInternetAccess = false;
}
