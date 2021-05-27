package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class ProactiveTestData {
   public VsanTestData generalData;
   public Long timestamp;
   public ProactiveTestData.PerfTestType perfTestType;
   public ManagedObjectReference taskMoRef = null;

   @TsModel
   public static enum PerfTestType {
      vmCreation,
      multicast,
      unicast;
   }
}
