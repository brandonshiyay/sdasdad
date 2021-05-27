package com.vmware.vsan.client.services.advancedoptions;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vim.vsan.binding.vim.vsan.VsanExtendedConfig;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthConfigSpec;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthThreshold;
import com.vmware.vim.vsan.binding.vim.vsan.VsanUnmapConfig;
import com.vmware.vsan.client.services.capacity.model.AlertThreshold;
import com.vmware.vsan.client.services.config.ConfigInfoService;
import com.vmware.vsphere.client.vsan.impl.ConfigureVsanClusterMutationProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdvancedOptionsService {
   @Autowired
   private ConfigureVsanClusterMutationProvider configureClusterService;
   @Autowired
   private ConfigInfoService configInfoService;

   @TsService
   public AdvancedOptionsInfo getAdvancedOptionsInfo(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfo = this.configInfoService.getVsanConfigInfo(clusterRef);
      return AdvancedOptionsInfo.fromVmodl(configInfo, clusterRef);
   }

   @TsService
   public ManagedObjectReference configureAdvancedOptionsExtended(ManagedObjectReference clusterRef, AdvancedOptionsInfo advancedOptionsInfo, List alertThresholds) {
      ReconfigSpec spec = this.prepareReconfigureSpec(clusterRef, advancedOptionsInfo, alertThresholds);
      return this.configureClusterService.startReconfigureTask(clusterRef, spec);
   }

   @TsService
   public ManagedObjectReference configureAdvancedOptions(ManagedObjectReference clusterRef, AdvancedOptionsInfo advancedOptionsInfo) {
      ReconfigSpec spec = this.prepareReconfigureSpec(clusterRef, advancedOptionsInfo, (List)null);
      return this.configureClusterService.startReconfigureTask(clusterRef, spec);
   }

   private ReconfigSpec prepareReconfigureSpec(ManagedObjectReference clusterRef, AdvancedOptionsInfo advancedOptionsInfo, List alertThresholds) {
      ReconfigSpec spec = new ReconfigSpec();
      if (advancedOptionsInfo != null) {
         VsanExtendedConfig extendedConfig = AdvancedOptionsInfo.toVmodl(advancedOptionsInfo, clusterRef);
         spec.setExtendedConfig(extendedConfig);
         VsanUnmapConfig unmapConfig = new VsanUnmapConfig();
         unmapConfig.enable = advancedOptionsInfo.isGuestTrimUnmapEnabled;
         spec.setUnmapConfig(unmapConfig);
      }

      if (alertThresholds != null) {
         VsanHealthConfigSpec healthConfigSpec = new VsanHealthConfigSpec();
         healthConfigSpec.healthCheckThresholdSpec = (VsanHealthThreshold[])alertThresholds.stream().map(AlertThreshold::toVmodl).toArray((x$0) -> {
            return new VsanHealthThreshold[x$0];
         });
         spec.vsanHealthConfig = healthConfigSpec;
      }

      return spec;
   }
}
