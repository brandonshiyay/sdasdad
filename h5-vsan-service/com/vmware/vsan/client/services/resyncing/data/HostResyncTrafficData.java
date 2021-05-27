package com.vmware.vsan.client.services.resyncing.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class HostResyncTrafficData {
   public ManagedObjectReference hostRef;
   public String primaryIconId;
   public String name;
   public int resyncTraffic;
}
