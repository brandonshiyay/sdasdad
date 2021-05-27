package com.vmware.vsan.client.util.fss;

import com.vmware.vsan.client.util.config.ConfigurationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FeatureStateUtil {
   private static final Log logger = LogFactory.getLog(FeatureStateUtil.class);
   private static final FeatureStateProvider provider = getFeatureStateProvider();

   public static boolean isEnvoySidecarEnabled() {
      return provider.isEnvoySidecarEnabled();
   }

   public static boolean isFipsEnabled() {
      return provider.isFipsEnabled();
   }

   private static FeatureStateProvider getFeatureStateProvider() {
      return (FeatureStateProvider)(ConfigurationUtil.isLocalDevEnv() ? new FeatureStateFileProviderImpl() : new FeatureStateJarProviderImpl());
   }
}
