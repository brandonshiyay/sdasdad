package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.KeyValue;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class VirtualObjectBasicModel {
   public String uid;
   public String name;
   public String diskUuid;

   public static VirtualObjectBasicModel fromVmodl(VsanObjectIdentity identity) {
      VirtualObjectBasicModel virtualObject = new VirtualObjectBasicModel();
      virtualObject.uid = identity.uuid;
      virtualObject.name = identity.description;
      virtualObject.diskUuid = findDiskUuid(identity.metadatas);
      return virtualObject;
   }

   private static String findDiskUuid(KeyValue[] metadatas) {
      if (ArrayUtils.isEmpty(metadatas)) {
         return null;
      } else {
         KeyValue diskUuidMetadata = (KeyValue)Arrays.stream(metadatas).filter((metadata) -> {
            return metadata.key.equals("vsanDirectDiskUuid");
         }).findFirst().orElse((Object)null);
         return diskUuidMetadata != null ? diskUuidMetadata.value : null;
      }
   }
}
