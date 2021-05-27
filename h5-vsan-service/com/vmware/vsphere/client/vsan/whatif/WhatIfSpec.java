package com.vmware.vsphere.client.vsan.whatif;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class WhatIfSpec {
   public String entityUuid;
   public ManagedObjectReference clusterRef;
   public boolean detailed;
}
