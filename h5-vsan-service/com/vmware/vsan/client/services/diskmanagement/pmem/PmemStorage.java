package com.vmware.vsan.client.services.diskmanagement.pmem;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.diskmanagement.StorageCapacity;
import com.vmware.vsphere.client.vsan.health.DatastoreHealthStatus;

public class PmemStorage {
   public ManagedObjectReference dsRef;
   public ManagedObjectReference hostRef;
   public String name;
   public String uuid;
   public DatastoreHealthStatus overallStatus;
   public boolean isAccessible;
   public boolean isMounted;
   public StorageCapacity capacity;
   public boolean isManageableByVsan;
}
