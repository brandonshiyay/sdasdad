package com.vmware.vsan.client.services.common.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class IpAddressesRequestSpec {
   public String ipAddress;
   public String subnetMask;
   public int hostsNumber;
}
