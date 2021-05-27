package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class ComplianceCheckResultObj {
   public boolean isNew;
   public boolean hasChanged;
   public long originalCapacity = 0L;
   public long finalCapacity = 0L;
   public long initCapacity = 0L;
   public long finalUsedCapacity = 0L;
   public long originalCacheCapacity = 0L;
   public long finalCacheCapacity = 0L;
   public long initCacheCapacity = 0L;
   public long finalUsedCacheCapacity = 0L;
   public String uuid;
   public String name;
   public String objectType;
   public ComplianceCheckResultObj[] childDevices;
}
