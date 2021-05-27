package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VlanType {
   NONE,
   PVLAN,
   VLAN_ID,
   VLAN_TRUNK;
}
