package com.vmware.vsan.client.util.fss;

import com.vmware.vim.featurestateswitch.FeatureState;

class FeatureStateJarProviderImpl implements FeatureStateProvider {
   FeatureStateJarProviderImpl() {
      FeatureState.init();
   }

   public boolean isEnvoySidecarEnabled() {
      return FeatureState.getVC_FIPS_SIDECAR();
   }

   public boolean isFipsEnabled() {
      return FeatureState.getVC_JAVA_FIPS();
   }
}
