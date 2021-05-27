package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanStorageWorkloadType;

@TsModel
public class VsanStorageTestSpec {
   public String typeId;
   public String name;
   public Integer duration;
   public String description;
   public String profileId;

   public static VsanStorageTestSpec fromVmodl(VsanStorageWorkloadType model) {
      VsanStorageTestSpec type = new VsanStorageTestSpec();
      type.typeId = model.typeId;
      type.name = model.name;
      type.description = model.description;
      return type;
   }
}
