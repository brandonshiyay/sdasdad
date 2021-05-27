package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import java.util.ArrayList;
import java.util.List;

@TsModel
public enum DatastoreType implements EnumWithKey {
   VSAN("vsan"),
   VMFS("vsandirect"),
   PMEM("pmem");

   private String key;

   private DatastoreType(String key) {
      this.key = key;
   }

   public String getKey() {
      return this.key;
   }

   public static DatastoreType fromString(String text) {
      return (DatastoreType)EnumUtils.fromString(DatastoreType.class, text);
   }

   public static String[] getSupportedTypes(ManagedObjectReference clusterRef) {
      List supportedTypes = new ArrayList();
      supportedTypes.add(VSAN.getKey());
      if (VsanCapabilityUtils.isManagedVmfsSupportedOnVC(clusterRef)) {
         supportedTypes.add(VMFS.getKey());
      }

      if (VsanCapabilityUtils.isManagedPMemSupportedOnVC(clusterRef)) {
         supportedTypes.add(PMEM.getKey());
      }

      return (String[])supportedTypes.toArray(new String[0]);
   }
}
