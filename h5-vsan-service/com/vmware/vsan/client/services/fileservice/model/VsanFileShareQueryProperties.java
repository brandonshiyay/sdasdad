package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareQueryProperties;

@TsModel
public class VsanFileShareQueryProperties {
   public boolean includeBasic;
   public boolean includeAllLabels;
   public boolean includeUsedCapacity;
   public boolean includeVsanObjectUuids;

   public FileShareQueryProperties toVmodl() {
      FileShareQueryProperties properties = new FileShareQueryProperties();
      properties.includeBasic = this.includeBasic;
      properties.includeAllLabels = this.includeAllLabels;
      properties.includeUsedCapacity = this.includeUsedCapacity;
      properties.includeVsanObjectUuids = this.includeVsanObjectUuids;
      return properties;
   }

   public static VsanFileShareQueryProperties getFileSharesQueryProperties(boolean includeBasic, boolean includeAllLabels, boolean includeUsedCapacity, boolean includeVsanObjectUuids) {
      VsanFileShareQueryProperties properties = new VsanFileShareQueryProperties();
      properties.includeBasic = includeBasic;
      properties.includeAllLabels = includeAllLabels;
      properties.includeVsanObjectUuids = includeVsanObjectUuids;
      properties.includeUsedCapacity = includeUsedCapacity;
      return properties;
   }
}
