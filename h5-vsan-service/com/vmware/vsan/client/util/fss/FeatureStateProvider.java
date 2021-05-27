package com.vmware.vsan.client.util.fss;

interface FeatureStateProvider {
   boolean isEnvoySidecarEnabled();

   boolean isFipsEnabled();
}
