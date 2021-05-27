package com.vmware.vsan.client.util.config;

import com.vmware.vsan.client.util.file.FileUtils;
import java.util.Properties;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigurationUtil {
   private static final Log logger = LogFactory.getLog(ConfigurationUtil.class);
   private static final String[] WEBCLIENT_CONFIG_FILE_PATH_CHUNKS = new String[]{"vsphere-client", "webclient.properties"};
   private static final String[] DS_CONFIG_FILE_PATH_CHUNKS = new String[]{"vsphere-ui", "config", "ds.properties"};
   private static final Properties webclientProps;
   private static final Properties dsProps;

   public static boolean isFling() {
      return Boolean.parseBoolean(System.getProperty("isFling"));
   }

   public static boolean isLocalDevEnv() {
      String propValue = getWebclientConfiguration("local.development");
      return Boolean.parseBoolean(propValue);
   }

   public static String getCmUrl() {
      return getWebclientConfiguration("cm.url");
   }

   public static String getKeystorePath() {
      return getWebclientConfiguration("keystore.jks.path");
   }

   public static String getKeystorePassword() {
      return getWebclientConfiguration("keystore.jks.password");
   }

   public static String getLocalDomainId() {
      return getDsConfiguration("service.homeLdu");
   }

   private static String getWebclientConfiguration(String key) {
      Validate.notNull(key);
      return webclientProps.getProperty(key);
   }

   private static String getDsConfiguration(String key) {
      Validate.notNull(key);
      return dsProps.getProperty(key);
   }

   public static String getConfigFolderPath() {
      return System.getenv("VMWARE_CFG_DIR");
   }

   private static Properties loadConfigFile(String[] filePathChunks) {
      String vsphereCfgDirPath = getConfigFolderPath();
      String configFilePath = FileUtils.createPathWithBase(vsphereCfgDirPath, filePathChunks);
      return FileUtils.loadPropertiesFile(configFilePath);
   }

   static {
      webclientProps = loadConfigFile(WEBCLIENT_CONFIG_FILE_PATH_CHUNKS);
      dsProps = loadConfigFile(DS_CONFIG_FILE_PATH_CHUNKS);
   }
}
