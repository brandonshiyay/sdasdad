package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.List;

@TsModel
public class HostPhysicalMappingData {
   public ManagedObjectReference hostRef;
   public ManagedObjectReference clusterRef;
   public String name;
   public String primaryIconId;
   public List physicalDisks;
   public Object[] storageAdapterDevices;
   public String faultDomain;

   public HostPhysicalMappingData() {
   }

   public HostPhysicalMappingData(ManagedObjectReference clusterRef, ManagedObjectReference hostRef, String hostName, String primaryIconId, List physicalDsks, Object[] storageAdapters, String faultDomain) {
      this.clusterRef = clusterRef;
      this.hostRef = hostRef;
      this.name = hostName;
      this.primaryIconId = primaryIconId;
      this.physicalDisks = physicalDsks;
      this.storageAdapterDevices = storageAdapters;
      this.faultDomain = faultDomain;
   }
}
