package com.vmware.vsan.client.util.fss;

import com.vmware.vsan.client.util.config.ConfigurationUtil;
import com.vmware.vsan.client.util.file.FileUtils;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class FeatureStateFileProviderImpl implements FeatureStateProvider {
   private static final Log logger = LogFactory.getLog(FeatureStateUtil.class);
   private static final String CONFIG_FOLDER = "vsphereFeatures";
   private static final String CONFIG_FILE = "vsphereFeatures.cfg";
   private static final String ENABLED = "enabled";
   private final Properties fssRegistry = this.loadConfigFile();

   public boolean isEnvoySidecarEnabled() {
      return this.isFssEnabled("VC_FIPS_SIDECAR");
   }

   public boolean isFipsEnabled() {
      return this.isFssEnabled("VC_JAVA_FIPS");
   }

   private Properties loadConfigFile() {
      String vsphereFeaturesCfgFilePath = this.getVsphereFeaturesCfgFilePath();
      return FileUtils.loadPropertiesFile(vsphereFeaturesCfgFilePath);
   }

   private String getVsphereFeaturesCfgFilePath() {
      String cfgDirPath = ConfigurationUtil.getConfigFolderPath();
      return FileUtils.createPath(cfgDirPath, "vsphereFeatures", "vsphereFeatures.cfg");
   }

   private boolean isFssEnabled(String fss) {
      String value = this.fssRegistry.getProperty(fss);
      return "enabled".equals(value);
   }
}
