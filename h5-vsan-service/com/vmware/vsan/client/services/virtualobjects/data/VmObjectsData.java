package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.common.data.VmData;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class VmObjectsData {
   public ManagedObjectReference vmRef;
   public String name;
   public String primaryIconId;
   public List vmObjects;

   public VmObjectsData() {
   }

   public VmObjectsData(VmData vmData) {
      this.vmRef = vmData.vmRef;
      this.name = vmData.name;
      this.primaryIconId = vmData.primaryIconId;
      this.vmObjects = new ArrayList();
   }
}
