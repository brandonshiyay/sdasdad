package com.vmware.vsphere.client.vsan.iscsi.models.config;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetServiceConfig;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectInformation;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import org.apache.commons.lang3.BooleanUtils;

@TsModel
public class VsanIscsiConfig {
   public VsanIscsiTargetServiceConfig vsanIscsiTargetServiceConfig;
   public VsanObjectInformation vsanObjectInformation;

   public static VsanIscsiConfig from(ConfigInfoEx configInfoEx, VsanObjectInformation iscsiHomeObject) {
      VsanIscsiConfig vsanIscsiConfig = new VsanIscsiConfig();
      if (configInfoEx != null && !BooleanUtils.isNotTrue(configInfoEx.enabled) && configInfoEx.getIscsiConfig() != null && !BooleanUtils.isNotTrue(configInfoEx.getIscsiConfig().enabled)) {
         vsanIscsiConfig.vsanIscsiTargetServiceConfig = configInfoEx.getIscsiConfig();
         vsanIscsiConfig.vsanObjectInformation = iscsiHomeObject;
         return vsanIscsiConfig;
      } else {
         vsanIscsiConfig.vsanIscsiTargetServiceConfig = new VsanIscsiTargetServiceConfig();
         vsanIscsiConfig.vsanIscsiTargetServiceConfig.enabled = false;
         return vsanIscsiConfig;
      }
   }
}
