package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceConfig;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceDomainConfig;
import com.vmware.vsan.client.util.VmodlHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class VsanFileServiceCommonConfig {
   private static final Log logger = LogFactory.getLog(VsanFileServiceCommonConfig.class);
   public VsanFileServiceDomain domainConfig;
   public ManagedObjectReference network;
   public boolean isFileAnalyticsEnabled;
   public boolean isPerformanceServiceEnabled;

   public static VsanFileServiceCommonConfig fromVmodl(ConfigInfoEx vsanConfig, ManagedObjectReference moRef) {
      if (vsanConfig != null && vsanConfig.fileServiceConfig != null) {
         VsanFileServiceCommonConfig config = new VsanFileServiceCommonConfig();
         config.isFileAnalyticsEnabled = BooleanUtils.isTrue(vsanConfig.fileServiceConfig.fileAnalyticsEnabled);
         config.isPerformanceServiceEnabled = vsanConfig.perfsvcConfig != null && BooleanUtils.isTrue(vsanConfig.perfsvcConfig.enabled);
         config.network = vsanConfig.fileServiceConfig.network;
         if (config.network != null) {
            VmodlHelper.assignServerGuid(config.network, moRef.getServerGuid());
         }

         if (ArrayUtils.isNotEmpty(vsanConfig.fileServiceConfig.domains)) {
            config.domainConfig = VsanFileServiceDomain.fromVmodl(vsanConfig.fileServiceConfig.domains[0]);
         } else {
            logger.warn("No domains configured!" + vsanConfig.fileServiceConfig.toString());
         }

         return config;
      } else {
         return null;
      }
   }

   public FileServiceConfig toVmodl() {
      FileServiceConfig vmodl = new FileServiceConfig();
      if (this.domainConfig != null) {
         FileServiceDomainConfig domain = this.domainConfig.toVmodl();
         vmodl.domains = new FileServiceDomainConfig[]{domain};
      }

      vmodl.network = this.network;
      vmodl.enabled = true;
      vmodl.fileAnalyticsEnabled = this.isFileAnalyticsEnabled;
      return vmodl;
   }

   public static FileServiceConfig createDisabledConfig() {
      FileServiceConfig vmodl = new FileServiceConfig();
      vmodl.enabled = false;
      return vmodl;
   }

   public String toString() {
      return "VsanFileServiceCommonConfig(domainConfig=" + this.domainConfig + ", network=" + this.network + ", isFileAnalyticsEnabled=" + this.isFileAnalyticsEnabled + ", isPerformanceServiceEnabled=" + this.isPerformanceServiceEnabled + ")";
   }
}
