package com.vmware.vsan.client.services.config;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServicePreflightCheckResult;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.advancedoptions.AdvancedOptionsInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.model.ClusterMode;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.services.fileservice.model.FileSharesPaginationSpec;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceCommonConfig;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServicePrecheckResult;
import com.vmware.vsan.client.services.health.model.HistoricalHealthConfig;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.data.VsanConfigSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.config.VsanIscsiTargetConfig;
import com.vmware.vsphere.client.vsan.iscsi.providers.VsanIscsiPropertyProvider;
import com.vmware.vsphere.client.vsan.perf.VsanPerfPropertyProvider;
import com.vmware.vsphere.client.vsan.perf.model.PerfStatsObjectInfo;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterService;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VsanConfigService {
   private static final Log logger = LogFactory.getLog(VsanConfigService.class);
   @Autowired
   private VsanIscsiPropertyProvider iscsiPropertyProvider;
   @Autowired
   private VsanPerfPropertyProvider perfPropertyProvider;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VcClient vcClient;
   @Autowired
   private CsdService csdService;
   @Autowired
   private VsanStretchedClusterService vsanStretchedClusterService;

   @TsService
   public VsanConfigSpec getVsanConfigSpec(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx;
      try {
         configInfoEx = this.getConfigInfoEx(clusterRef);
      } catch (Exception var6) {
         logger.error("Error while retrieve ConfigInfoEx. VsanCongigSpec will be null.");
         return null;
      }

      VsanConfigSpec vsanConfigSpec = new VsanConfigSpec();
      vsanConfigSpec.isEnabled = configInfoEx.enabled;
      vsanConfigSpec.clusterMode = ClusterMode.parse(configInfoEx.mode);
      vsanConfigSpec.spaceEfficiencyConfig = SpaceEfficiencyConfig.fromVmodl(configInfoEx.dataEfficiencyConfig);
      if (configInfoEx.dataEncryptionConfig != null && configInfoEx.dataEncryptionConfig.isEncryptionEnabled()) {
         vsanConfigSpec.enableDataAtRestEncryption = true;
         vsanConfigSpec.eraseDisksBeforeUse = configInfoEx.dataEncryptionConfig.eraseDisksBeforeUse;
         if (configInfoEx.dataEncryptionConfig.kmsProviderId != null) {
            vsanConfigSpec.kmipClusterId = configInfoEx.dataEncryptionConfig.kmsProviderId.id;
         }
      } else {
         vsanConfigSpec.enableDataAtRestEncryption = false;
      }

      if (configInfoEx.dataInTransitEncryptionConfig != null && BooleanUtils.isTrue(configInfoEx.dataInTransitEncryptionConfig.enabled)) {
         vsanConfigSpec.enableDataInTransitEncryption = true;
         if (configInfoEx.dataInTransitEncryptionConfig.rekeyInterval != null) {
            vsanConfigSpec.rekeyInterval = configInfoEx.dataInTransitEncryptionConfig.rekeyInterval;
         }
      }

      try {
         vsanConfigSpec.historicalHealthConfig = this.getHistoricalHealthConfig(clusterRef, configInfoEx);
      } catch (Exception var5) {
         logger.error(var5);
      }

      vsanConfigSpec.autoClaimDisks = configInfoEx.defaultConfig.autoClaimStorage != null ? configInfoEx.defaultConfig.autoClaimStorage : false;
      vsanConfigSpec.hasSharedWitnessHost = this.vsanStretchedClusterService.hasSharedWitnessHost(clusterRef);
      vsanConfigSpec.isServerOrClientCluster = this.csdService.isClusterClientOrServer(clusterRef);
      vsanConfigSpec.advancedOptions = AdvancedOptionsInfo.fromVmodl(configInfoEx, clusterRef);
      vsanConfigSpec.enableRdma = configInfoEx.rdmaConfig != null && configInfoEx.rdmaConfig.rdmaEnabled;
      return vsanConfigSpec;
   }

   @TsService
   public SpaceEfficiencyConfig getSpaceEfficiencyStatus(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = this.getConfigInfoEx(clusterRef);
      return SpaceEfficiencyConfig.fromVmodl(configInfoEx.dataEfficiencyConfig);
   }

   @TsService
   public VsanServiceData getPerformanceStatus(ManagedObjectReference clusterRef) {
      PerfStatsObjectInfo statsInfo = this.perfPropertyProvider.getPerfStatsInfo(clusterRef);
      VsanServiceStatus serviceStatus = statsInfo != null && statsInfo.serviceEnabled ? VsanServiceStatus.ENABLED : VsanServiceStatus.DISABLED;
      return new VsanServiceData(serviceStatus, statsInfo);
   }

   @TsService
   public VsanServiceData getIscsiTargetStatus(ManagedObjectReference clusterRef) {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnCluster(clusterRef)) {
         return new VsanServiceData(VsanServiceStatus.NOT_SUPPORTED);
      } else {
         VsanIscsiTargetConfig iscsiTargetConfig = this.iscsiPropertyProvider.getVsanIscsiTargetConfig(clusterRef);
         VsanServiceStatus serviceStatus = iscsiTargetConfig != null && iscsiTargetConfig.status ? VsanServiceStatus.ENABLED : VsanServiceStatus.DISABLED;
         return new VsanServiceData(serviceStatus, iscsiTargetConfig);
      }
   }

   @TsService
   public VsanServiceData getFileServicesStatus(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);
      if (!VsanCapabilityUtils.isFileServiceSupportedOnVc(clusterRef)) {
         return null;
      } else if (!VsanCapabilityUtils.isFileServiceSupported(clusterRef)) {
         return new VsanServiceData(VsanServiceStatus.NOT_SUPPORTED);
      } else {
         Measure measure = new Measure("Retrieving File Service configuration");
         Throwable var3 = null;

         VsanServiceData var5;
         try {
            VsanAsyncDataRetriever retriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadConfigInfoEx().loadFileServicePrecheckResult();
            var5 = this.getFileServicesStatus(clusterRef, retriever);
         } catch (Throwable var14) {
            var3 = var14;
            throw var14;
         } finally {
            if (measure != null) {
               if (var3 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var13) {
                     var3.addSuppressed(var13);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var5;
      }
   }

   public HistoricalHealthConfig getHistoricalHealthConfig(ManagedObjectReference clusterRef, ConfigInfoEx configInfoEx) {
      if (!VsanCapabilityUtils.isHistoricalHealthSupported(clusterRef)) {
         return null;
      } else if (configInfoEx != null && configInfoEx.vsanHealthConfig != null && configInfoEx.vsanHealthConfig.historicalHealthConfig != null) {
         return new HistoricalHealthConfig(configInfoEx.vsanHealthConfig.historicalHealthConfig);
      } else {
         throw new IllegalStateException("VsanHistoricalHealthConfig is not set. No info for historical health configuration.");
      }
   }

   private VsanServiceData getFileServicesStatus(ManagedObjectReference clusterRef, VsanAsyncDataRetriever retriever) {
      VsanFileServicePrecheckResult precheckResult = this.getPrecheckResult(clusterRef, retriever);
      if (this.fileServicesAreUnavailable(precheckResult)) {
         return new VsanServiceData(VsanServiceStatus.NOT_SUPPORTED);
      } else {
         ConfigInfoEx vsanConfig = this.getConfigInfoEx(retriever);
         VsanFileServiceCommonConfig vdfsConfig = VsanFileServiceCommonConfig.fromVmodl(vsanConfig, clusterRef);
         if (vsanConfig.fileServiceConfig != null && vsanConfig.fileServiceConfig.enabled) {
            VsanConfigService.NetworkProperties networkProperties = this.getNetworkProperties(vdfsConfig);
            this.loadFileSharesCount(precheckResult.paginationSupported, retriever);
            int numberOfShares = this.getNumberOfShares(clusterRef, retriever, precheckResult.paginationSupported);
            VsanVdfsConfig details = new VsanVdfsConfig(vdfsConfig, precheckResult, numberOfShares, networkProperties.name, networkProperties.iconId);
            VsanServiceData serviceData = new VsanServiceData(vdfsConfig.domainConfig == null ? VsanServiceStatus.PARTIAL : VsanServiceStatus.ENABLED, details);
            return serviceData;
         } else {
            VsanVdfsConfig details = new VsanVdfsConfig(vdfsConfig, precheckResult, 0, (String)null, (String)null);
            return new VsanServiceData(VsanServiceStatus.DISABLED, details);
         }
      }
   }

   private VsanFileServicePrecheckResult getPrecheckResult(ManagedObjectReference clusterRef, VsanAsyncDataRetriever dataRetriever) {
      VsanFileServicePrecheckResult precheckResult = null;

      try {
         VsanFileServicePreflightCheckResult vsanFileServicePreflightCheckResult = dataRetriever.getFileServicePrecheckResult();
         precheckResult = VsanFileServicePrecheckResult.fromVmodl(vsanFileServicePreflightCheckResult);
      } catch (Exception var16) {
         logger.error("Unable to get file service precheck for cluster: " + clusterRef + "'. Returning partial result.", var16);
      }

      if (precheckResult != null) {
         VcConnection conn = this.vcClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            precheckResult.vcVersion = conn.getContent().about.version;
         } catch (Throwable var15) {
            var5 = var15;
            throw var15;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var14) {
                     var5.addSuppressed(var14);
                  }
               } else {
                  conn.close();
               }
            }

         }
      }

      return precheckResult;
   }

   private boolean fileServicesAreUnavailable(VsanFileServicePrecheckResult precheckResult) {
      return precheckResult == null || precheckResult.fileServiceVersion == null && precheckResult.mixedModeIssue == null && precheckResult.vsanDatastoreIssue == null;
   }

   private int getNumberOfShares(ManagedObjectReference clusterRef, VsanAsyncDataRetriever dataRetriever, boolean paginationSupported) {
      int numberOfShares = 0;

      try {
         if (paginationSupported) {
            numberOfShares = dataRetriever.getFileSharesCountResult();
         } else {
            numberOfShares = dataRetriever.getFileShares().size();
         }
      } catch (Exception var6) {
         logger.error("Unable to load file shares for cluster: " + clusterRef + "'. Returning partial result.", var6);
      }

      return numberOfShares;
   }

   private VsanConfigService.NetworkProperties getNetworkProperties(VsanFileServiceCommonConfig vdfsConfig) {
      VsanConfigService.NetworkProperties networkProperties = new VsanConfigService.NetworkProperties();
      if (vdfsConfig != null && vdfsConfig.network != null) {
         try {
            DataServiceResponse properties = QueryUtil.getProperties(vdfsConfig.network, new String[]{"name", "primaryIconId"});
            networkProperties.name = (String)properties.getProperty(vdfsConfig.network, "name");
            networkProperties.iconId = (String)properties.getProperty(vdfsConfig.network, "primaryIconId");
         } catch (Exception var4) {
            logger.error("Unable to get the name of network: " + vdfsConfig.network + "'. Returning partial result.", var4);
         }

         return networkProperties;
      } else {
         return networkProperties;
      }
   }

   private void loadFileSharesCount(boolean isFileSharesPaginationSupported, VsanAsyncDataRetriever retriever) {
      if (isFileSharesPaginationSupported) {
         retriever.loadFileSharesCount();
      } else {
         FileSharesPaginationSpec spec = new FileSharesPaginationSpec();
         spec.pageSize = 32;
         retriever.loadFileShares(spec);
      }

   }

   public ConfigInfoEx getConfigInfoEx(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = null;

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
            Measure measure = new Measure("VsanVcClusterConfigSystem.getConfigInfoEx");
            Throwable var7 = null;

            try {
               configInfoEx = vsanConfigSystem.getConfigInfoEx(clusterRef);
            } catch (Throwable var32) {
               var7 = var32;
               throw var32;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var31) {
                        var7.addSuppressed(var31);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var34) {
            var4 = var34;
            throw var34;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var30) {
                     var4.addSuppressed(var30);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return configInfoEx;
      } catch (Exception var36) {
         logger.error("Unable to fetch ConfigInfoEx");
         throw new VsanUiLocalizableException("vsan.common.cluster.configuration.error", var36);
      }
   }

   private ConfigInfoEx getConfigInfoEx(VsanAsyncDataRetriever retriever) {
      try {
         return retriever.getConfigInfoEx();
      } catch (Exception var3) {
         logger.error("Unable to fetch ConfigInfoEx", var3);
         throw new VsanUiLocalizableException("vsan.common.cluster.configuration.error", var3);
      }
   }

   private class NetworkProperties {
      private String name;
      private String iconId;

      private NetworkProperties() {
      }

      // $FF: synthetic method
      NetworkProperties(Object x1) {
         this();
      }
   }
}
