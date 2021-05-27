package com.vmware.vsan.client.services.vum;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vim.vsan.binding.vim.vsan.VsanVumConfig;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsphere.client.vsan.data.VumBaselineRecommendationType;
import com.vmware.vsphere.client.vsan.impl.ConfigureVsanClusterMutationProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VumBaselineRecommendationService {
   @Autowired
   private ConfigureVsanClusterMutationProvider configureClusterService;
   @Autowired
   private VsanConfigService vsanConfigService;

   @TsService
   public ManagedObjectReference setClusterVumBaselineRecommendation(ManagedObjectReference clusterRef, String recommendation) {
      VsanVumConfig vumConfig = this.getVumConfig(clusterRef);
      vumConfig.baselinePreferenceType = recommendation;
      ReconfigSpec spec = new ReconfigSpec();
      spec.vumConfig = vumConfig;
      return this.configureClusterService.startReconfigureTask(clusterRef, spec);
   }

   @TsService
   public BaselineRecommendationData getVumBaselineRecommendation(ManagedObjectReference vcRoot, ManagedObjectReference clusterRef) {
      BaselineRecommendationData recommendationData = new BaselineRecommendationData();
      recommendationData.vcRecommendation = this.getVcVumBaselineRecommendation(vcRoot);
      if (clusterRef != null) {
         recommendationData.clusterRecommendation = this.getClusterVumBaselineRecommendation(clusterRef);
      }

      return recommendationData;
   }

   private VumBaselineRecommendationType getVcVumBaselineRecommendation(ManagedObjectReference vcRoot) {
      return VumBaselineRecommendationType.latestRelease;
   }

   public VumBaselineRecommendationType getClusterVumBaselineRecommendation(ManagedObjectReference clusterRef) {
      VsanVumConfig vumConfig = this.getVumConfig(clusterRef);
      return StringUtils.isEmpty(vumConfig.baselinePreferenceType) ? VumBaselineRecommendationType.latestRelease : VumBaselineRecommendationType.valueOf(vumConfig.getBaselinePreferenceType());
   }

   private VsanVumConfig getVumConfig(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = this.vsanConfigService.getConfigInfoEx(clusterRef);
      VsanVumConfig vumConfig = configInfoEx.vumConfig;
      if (vumConfig == null) {
         vumConfig = new VsanVumConfig();
      }

      return vumConfig;
   }
}
