package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class DvsData {
   public String dvsName;
   public VlanType vlanType;
   public String vlan;
}
