package com.vmware.vsphere.client.vsan.iscsi.models.config;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.base.data.StoragePolicyData;

@TsModel
public class VsanIscsiConfigEditSpec {
   public Boolean enableIscsiTargetService;
   public String network;
   public Integer port;
   public VsanIscsiAuthSpec authSpec;
   public StoragePolicyData policy;
}
