package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanHostData {
   public String name;
   public String nodeUuid;
   public String faultDomainName;
}
