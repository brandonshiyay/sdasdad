package com.vmware.vsan.client.services.ioinsight.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import java.util.List;

@TsModel
public class HostIoInsightInfo {
   public InventoryNode host;
   public String faultMessage;
   @ProxygenSerializer.ElementType(ManagedObjectReference.class)
   public List monitoredVms;
}
