package com.vmware.vsan.client.services.hci;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.hci.model.BasicClusterConfigData;

@TsModel
public class QuickstartStatusData {
   public boolean isVsanEnabled;
   public BasicClusterConfigData clusterData;
   public ManagedObjectReference clusterRef;
}
