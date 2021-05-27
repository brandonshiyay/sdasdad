package com.vmware.vsan.client.services.capability;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VSANStretchedClusterCapability;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.data.VsanCapabilityData;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VsanCapabilityUtils {
   private static final Log _logger = LogFactory.getLog(VsanCapabilityUtils.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanCapabilityUtils.class);
   private static VsanCapabilityCacheManager capabilityCacheManager;

   public static void setVsanCapabilityCacheManager(VsanCapabilityCacheManager capabilityCacheManager) {
      VsanCapabilityUtils.capabilityCacheManager = capabilityCacheManager;
   }

   public static VsanCapabilityData getVcCapabilities(ManagedObjectReference moRef) {
      return capabilityCacheManager.getVcCapabilities(moRef);
   }

   public static VsanCapabilityData getCapabilities(ManagedObjectReference moRef) {
      validateMoRef(moRef);
      VsanCapabilityData capabilityData = HostSystem.class.getSimpleName().equals(moRef.getType()) ? capabilityCacheManager.getHostCapabilities(moRef) : capabilityCacheManager.getClusterCapabilities(moRef);
      return capabilityData;
   }

   public static boolean isUpgradeSystemExSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isUpgradeSupported;
   }

   public static boolean isUpgradeSystem2SupportedOnVc(ManagedObjectReference moRef) {
      return isUpgradeSystemExSupportedOnVc(moRef);
   }

   public static boolean isObjectSystemSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isObjectIdentitiesSupported;
   }

   public static boolean isObjectSystemSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isObjectIdentitiesSupported;
   }

   public static boolean isObjectsHealthV2SupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isObjectsHealthV2Supported;
   }

   public static boolean isSupportInsightSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isSupportInsightSupported;
   }

   public static boolean isClusterConfigSystemSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isClusterConfigSupported;
   }

   public static boolean isSpaceEfficiencySupported(ManagedObjectReference moRef) {
      boolean deduplicationAndCompressionIsSupported = isDeduplicationAndCompressionSupportedOnVc(moRef) && isDeduplicationAndCompressionSupported(moRef);
      boolean isCompressionOnlySupported = isCompressionOnlySupportedOnVc(moRef) && isCompressionOnlySupported(moRef);
      return deduplicationAndCompressionIsSupported || isCompressionOnlySupported;
   }

   public static boolean isDeduplicationAndCompressionSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isDeduplicationAndCompressionSupported;
   }

   public static boolean isCompressionOnlySupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isCompressionOnlySupported;
   }

   public static boolean isDeduplicationAndCompressionSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isDeduplicationAndCompressionSupported;
   }

   public static boolean isCompressionOnlySupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isCompressionOnlySupported;
   }

   public static boolean isIscsiTargetsSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isIscsiTargetsSupported;
   }

   public static boolean isIscsiTargetsSupportedOnHost(ManagedObjectReference moRef) {
      validateMoRef(moRef);
      return getCapabilities(moRef).isIscsiTargetsSupported;
   }

   public static boolean isIscsiTargetsSupportedOnCluster(ManagedObjectReference moRef) {
      validateMoRef(moRef);
      return getCapabilities(moRef).isIscsiTargetsSupported;
   }

   public static boolean isStretchedClusterSupported(ManagedObjectReference moRef, VsanClient vsanClient) {
      validateMoRef(moRef);
      boolean isSupported = false;
      if (ClusterComputeResource.class.getSimpleName().equals(moRef.getType())) {
         isSupported = isStretchedClusterSupportedOnCluster(moRef, vsanClient);
      } else if (HostSystem.class.getSimpleName().equals(moRef.getType())) {
         isSupported = isStretchedClusterSupportedOnHost(moRef, vsanClient);
      }

      return isSupported;
   }

   public static boolean isAllFlashSupported(ManagedObjectReference moRef) {
      validateMoRef(moRef);
      boolean isSupported = false;
      if (ClusterComputeResource.class.getSimpleName().equals(moRef.getType())) {
         isSupported = isAllFlashSupportedOnCluster(moRef);
      } else if (HostSystem.class.getSimpleName().equals(moRef.getType())) {
         isSupported = isAllFlashSupportedOnHost(moRef);
      }

      return isSupported;
   }

   public static boolean isDataAtRestEncryptionSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isEncryptionSupported;
   }

   public static boolean isDataAtRestEncryptionSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isEncryptionSupported;
   }

   public static boolean isDataInTransitEncryptionSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isDataInTransitEncryptionSupported;
   }

   public static boolean isDataInTransitEncryptionSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isDataInTransitEncryptionSupported;
   }

   public static boolean isSilentCheckSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isEncryptionSupported;
   }

   public static boolean isPerfVerboseModeSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isPerfVerboseModeSupported;
   }

   public static boolean isPerfSvcAutoConfigSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isPerfSvcAutoConfigSupported;
   }

   public static boolean isResyncThrottlingSupported(ManagedObjectReference moRef) {
      VsanCapabilityData capabilityData = getCapabilities(moRef);
      _logger.info("Resync throttling supported on cluster is: " + capabilityData.isResyncThrottlingSupported);
      return capabilityData.isResyncThrottlingSupported;
   }

   public static boolean isConfigAssistSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isConfigAssistSupported;
   }

   public static boolean isUpdatesMgmtSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isUpdatesMgmtSupported;
   }

   public static boolean isWhatIfComplianceSupported(ManagedObjectReference moRef) {
      validateMoRef(moRef);
      return getVcCapabilities(moRef).isWhatIfComplianceSupported;
   }

   public static boolean isHistoricalCapacitySupportedOnHost(ManagedObjectReference hostRef) {
      validateMoRef(hostRef);
      VsanCapabilityData capabilities = getCapabilities(hostRef);
      return capabilities.isDisconnected || capabilities.isHistoricalCapacitySupported;
   }

   public static boolean isWhatIfSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isWhatIfSupported;
   }

   public static boolean isPerfAnalysisSupportedOnVc(ManagedObjectReference moRef) {
      validateMoRef(moRef);
      return true;
   }

   public static boolean isCloudHealthSupportedOnVc(ManagedObjectReference moRef) {
      validateMoRef(moRef);
      return getVcCapabilities(moRef).isCloudHealthSupported;
   }

   public static boolean isStretchedClusterSupportedOnHost(ManagedObjectReference moRef, VsanClient vsanClient) {
      boolean isSupported = false;
      VsanConnection conn = vsanClient.getConnection(moRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();

         try {
            VsanProfiler.Point point = _profiler.point("stretchedClusterSystem.retrieveStretchedClusterHostCapability");
            Throwable var7 = null;

            try {
               VSANStretchedClusterCapability capability = stretchedClusterSystem.retrieveStretchedClusterHostCapability(moRef);
               isSupported = capability.isSupported;
            } catch (Throwable var32) {
               var7 = var32;
               throw var32;
            } finally {
               if (point != null) {
                  if (var7 != null) {
                     try {
                        point.close();
                     } catch (Throwable var31) {
                        var7.addSuppressed(var31);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var34) {
            _logger.error("Failed to retrieve VSAN stretched cluster capability for host: " + var34);
            isSupported = false;
         }
      } catch (Throwable var35) {
         var4 = var35;
         throw var35;
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

      return isSupported;
   }

   public static boolean isStretchedClusterSupportedOnCluster(ManagedObjectReference moRef, VsanClient vsanClient) {
      boolean isSupported = true;
      VsanConnection conn = vsanClient.getConnection(moRef.getServerGuid());
      Throwable var4 = null;

      boolean var41;
      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();

         try {
            VsanProfiler.Point point = _profiler.point("stretchedClusterSystem.retrieveStretchedClusterVcCapability");
            Throwable var7 = null;

            try {
               VSANStretchedClusterCapability[] capabilities = stretchedClusterSystem.retrieveStretchedClusterVcCapability(moRef, false);
               VSANStretchedClusterCapability[] var9 = capabilities;
               int var10 = capabilities.length;

               for(int var11 = 0; var11 < var10; ++var11) {
                  VSANStretchedClusterCapability vcCapability = var9[var11];
                  if (vcCapability.isSupported == null || !vcCapability.isSupported) {
                     isSupported = false;
                     break;
                  }
               }
            } catch (Throwable var36) {
               var7 = var36;
               throw var36;
            } finally {
               if (point != null) {
                  if (var7 != null) {
                     try {
                        point.close();
                     } catch (Throwable var35) {
                        var7.addSuppressed(var35);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var38) {
            _logger.error("Failed to retrieve stretched cluster capabilities: " + var38);
            isSupported = false;
         }

         var41 = isSupported;
      } catch (Throwable var39) {
         var4 = var39;
         throw var39;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var34) {
                  var4.addSuppressed(var34);
               }
            } else {
               conn.close();
            }
         }

      }

      return var41;
   }

   public static boolean isAllFlashSupportedOnCluster(ManagedObjectReference clusterRef) {
      VsanCapabilityData capabilityData = getCapabilities(clusterRef);
      return capabilityData != null && capabilityData.isAllFlashSupported;
   }

   public static boolean isAllFlashSupportedOnHost(ManagedObjectReference hostRef) {
      VsanCapabilityData capabilityData = getCapabilities(hostRef);
      return capabilityData != null && (capabilityData.isDisconnected || capabilityData.isAllFlashSupported);
   }

   public static boolean isResyncEnhancedApiSupported(ManagedObjectReference hostRef) {
      return getCapabilities(hostRef).isResyncEnhancedApiSupported;
   }

   public static boolean isFileServiceSupported(ManagedObjectReference clusterRef) {
      return getCapabilities(clusterRef).isFileServiceSupported;
   }

   public static boolean isFileServiceSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isFileServiceSupported;
   }

   public static boolean isFileAnalyticsSupported(ManagedObjectReference clusterRef) {
      return getCapabilities(clusterRef).isFileAnalyticsSupported;
   }

   public static boolean isFileAnalyticsSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isFileAnalyticsSupported;
   }

   public static boolean isFileVolumesSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isFileVolumesSupported;
   }

   public static boolean isPersistenceServiceSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isPersistenceServiceSupported;
   }

   public static boolean isNetworkPerfTestSupportedOnCluster(ManagedObjectReference clusterRef) {
      return getCapabilities(clusterRef).isNetworkPerfTestSupported;
   }

   public static boolean isVsanVumIntegrationSupported(ManagedObjectReference clusterRef) {
      return getVcCapabilities(clusterRef).isVsanVumIntegrationSupported;
   }

   public static boolean isVumBaselineRecommendationSupportedOnVc(ManagedObjectReference clusterRef) {
      return getVcCapabilities(clusterRef).isVumBaselineRecommendationSupported;
   }

   public static boolean isWhatIfCapacitySupported(ManagedObjectReference clusterRef) {
      return getVcCapabilities(clusterRef).isWhatIfCapacitySupported;
   }

   public static boolean isIscsiOnlineResizeSupportedOnCluster(ManagedObjectReference clusterRef) {
      return getCapabilities(clusterRef).isIscsiOnlineResizeSupported;
   }

   public static boolean isIscsiStretchedClusterSupportedOnCluster(ManagedObjectReference clusterRef) {
      return getCapabilities(clusterRef).isIscsiStretchedClusterSupported;
   }

   public static boolean isVsanNestedFdsSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isNestedFdsSupported;
   }

   public static boolean isRepairTimerInResyncStatsSupported(ManagedObjectReference hostRef) {
      return getCapabilities(hostRef).isRepairTimerInResyncStatsSupported;
   }

   public static boolean isAutomaticRebalanceSupported(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isAutomaticRebalanceSupported;
   }

   public static boolean isPurgeInaccessibleVmSwapObjectsSupported(ManagedObjectReference clusterRef) {
      return getCapabilities(clusterRef).isPurgeInaccessibleVmSwapObjectsSupported;
   }

   public static boolean isRecreateDiskGroupSupported(ManagedObjectReference clusterRef) {
      return getCapabilities(clusterRef).isRecreateDiskGroupSupported;
   }

   public static boolean isUpdateVumReleaseCatalogOfflineSupported(ManagedObjectReference clusterRef) {
      return getVcCapabilities(clusterRef).isUpdateVumReleaseCatalogOfflineSupported;
   }

   public static boolean isAdvancedClusterSettingsSupported(ManagedObjectReference clusterRef) {
      return getCapabilities(clusterRef).isAdvancedClusterOptionsSupported;
   }

   public static boolean isPerfDiagnosticModeSupported(ManagedObjectReference clusterRef) {
      VsanCapabilityData capabilityData = getCapabilities(clusterRef);
      _logger.debug("Performance Diagnostic Mode Supported: " + capabilityData.isPerfDiagnosticModeSupported);
      return capabilityData.isPerfDiagnosticModeSupported;
   }

   public static boolean isPerfDiagnosticsFeedbackSupportedOnVc(ManagedObjectReference clusterRef) {
      VsanCapabilityData capabilityData = getVcCapabilities(clusterRef);
      _logger.debug("Performance Diagnostics Feedback Supported: " + capabilityData.isPerfDiagnosticsFeedbackSupported);
      return capabilityData.isPerfDiagnosticsFeedbackSupported;
   }

   public static boolean isGetHclLastUpdateOnVcSupported(ManagedObjectReference vcRef) {
      VsanCapabilityData capabilityData = getVcCapabilities(vcRef);
      _logger.debug("GetHclLastUpdate methdo on VC Supported: " + capabilityData.isGetHclLastUpdateOnVcSupported);
      return capabilityData.isGetHclLastUpdateOnVcSupported;
   }

   public static boolean isVerboseModeInClusterConfigurationSupported(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isVerboseModeInClusterConfigurationSupported;
   }

   public static boolean isHistoricalHealthSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isHistoricalHealthSupported;
   }

   public static boolean isHistoricalHealthSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isHistoricalHealthSupported;
   }

   public static boolean isCnsVolumesSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isCnsVolumesSupported;
   }

   public static boolean isHostResourcePrecheckSupportedOnVc(ManagedObjectReference objRef) {
      return getVcCapabilities(objRef).isHostResourcePrecheckSupported;
   }

   public static boolean isHostResourcePrecheckSupported(ManagedObjectReference objRef) {
      return getCapabilities(objRef).isHostResourcePrecheckSupported;
   }

   public static boolean isDiskResourcePrecheckSupportedOnVc(ManagedObjectReference objRef) {
      return getVcCapabilities(objRef).isDiskResourcePrecheckSupported;
   }

   public static boolean isDiskResourcePrecheckSupported(ManagedObjectReference objRef) {
      return getCapabilities(objRef).isDiskResourcePrecheckSupported;
   }

   public static boolean isHostReservedCapacitySupportedOnVc(ManagedObjectReference objectRef) {
      return getVcCapabilities(objectRef).isHostReservedCapacitySupported;
   }

   public static boolean isPersistenceResourceCheckSupportedOnVc(ManagedObjectReference objectRef) {
      return getVcCapabilities(objectRef).isPersistenceResourceCheckSupported;
   }

   public static boolean isVmLevelCapacityMonitoringSupportedOnVc(ManagedObjectReference moRef) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(moRef);
      return getVcCapabilities(clusterRef).isVmLevelCapacityMonitoringSupported;
   }

   public static boolean isSlackSpaceCapacitySupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isSlackSpaceCapacitySupported;
   }

   public static boolean isUnmountWithMaintenanceModeSupported(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isUnmountWithMaintenanceModeSupported;
   }

   public static boolean isSharedWitnessSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isSharedWitnessSupported;
   }

   public static boolean isSharedWitnessSupported(List hosts) {
      if (!getVcCapabilities((ManagedObjectReference)hosts.get(0)).isSharedWitnessSupported) {
         return false;
      } else {
         Iterator var1 = hosts.iterator();

         ManagedObjectReference host;
         do {
            if (!var1.hasNext()) {
               return true;
            }

            host = (ManagedObjectReference)var1.next();
         } while(getCapabilities(host).isSharedWitnessSupported);

         return false;
      }
   }

   public static boolean isSharedWitnessSupportedOnVc(ManagedObjectReference hostRef) {
      return getVcCapabilities(hostRef).isSharedWitnessSupported;
   }

   public static boolean isPmanIntegrationSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isPmanIntegrationSupported;
   }

   public static boolean isIoInsightSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isIoInsightSupported;
   }

   public static boolean isIoInsightSupported(ManagedObjectReference moRef) {
      if (!HostSystem.class.getSimpleName().equals(moRef.getType()) && !ClusterComputeResource.class.getSimpleName().equals(moRef.getType())) {
         throw new IllegalArgumentException(String.format("Unsupported ManagedObjectReference type: %s. Expected is: %s.", moRef.getType(), HostSystem.class.getSimpleName()));
      } else {
         return getCapabilities(moRef).isIoInsightSupported;
      }
   }

   public static boolean isSmbProtocolSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isSmbProtocolSupported;
   }

   public static boolean isNfsv3ProtocolSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isNfsv3ProtocolSupported;
   }

   public static boolean isCsdSupportedOnVC(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isCsdSupported;
   }

   public static boolean isCsdSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isCsdSupported;
   }

   public static boolean isHardwareManagementSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isHardwareManagementSupported;
   }

   public static boolean isHardwareManagementSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isHardwareManagementSupported;
   }

   public static boolean isRealTimePhysicalDiskHealthSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isRealTimePhysicalDiskHealthSupported;
   }

   public static boolean isDefaultGatewaySupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isDefaultGatewaySupported;
   }

   public static boolean isManagedVmfsSupportedOnVC(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isManagedVmfsSupported;
   }

   public static boolean isManagedVmfsSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isManagedVmfsSupported;
   }

   public static boolean isManagedPMemSupportedOnVC(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isManagedPMemSupported;
   }

   public static boolean isManagedPMemSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isManagedPMemSupported;
   }

   public static boolean isSlackSpaceReservationSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isSlackSpaceReservationSupported;
   }

   public static boolean isSlackSpaceReservationSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isSlackSpaceReservationSupported;
   }

   public static boolean isRdmaSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isRdmaSupported;
   }

   public static boolean isPersistenceServiceAirGapSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isPersistenceServiceAirGapSupported;
   }

   public static boolean isPolicySatisfiabilitySupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isPolicySatisfiabilitySupported;
   }

   public static boolean isCapacityCustomizableThresholdsSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isCapacityCustomizableThresholdsSupported;
   }

   public static boolean isVmIoDiagnosticsSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isVmIoDiagnosticsSupported;
   }

   public static boolean isNetworkDiagnosticsSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isNetworkDiagnosticsSupported;
   }

   public static boolean isEnsureDurabilitySupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isEnsureDurabilitySupported;
   }

   public static boolean isTopContributorsSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isTopContributorsSupported;
   }

   public static boolean isTopContributorsSupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isTopContributorsSupported;
   }

   public static boolean isEnsureDurabilitySupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isEnsureDurabilitySupported;
   }

   public static boolean isDiskMgmtRedesignSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isDiskMgmtRedesignSupported;
   }

   public static boolean isComputeOnlySupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isComputeOnlySupported;
   }

   public static boolean isComputeOnlySupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isComputeOnlySupported;
   }

   public static boolean isDecomModeForVsanDirectDisksSupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isDecomModeForVsanDirectDisksSupported;
   }

   public static boolean isVsanHciMeshPolicySupportedOnVc(ManagedObjectReference moRef) {
      return getVcCapabilities(moRef).isVsanHciMeshPolicySupported;
   }

   public static boolean isDitSharedWitnessInteroperabilitySupported(ManagedObjectReference moRef) {
      return getCapabilities(moRef).isDitSharedWitnessInteroperabilitySupported;
   }

   public static boolean isDitSharedWitnessInteroperabilitySupported(ManagedObjectReference[] clusterRefs) {
      ManagedObjectReference[] var1 = clusterRefs;
      int var2 = clusterRefs.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ManagedObjectReference clusterRef = var1[var3];
         if (!isDitSharedWitnessInteroperabilitySupported(clusterRef)) {
            return false;
         }
      }

      return true;
   }

   private static void validateMoRef(ManagedObjectReference moRef) {
      Validate.notNull(moRef);
      String type = moRef.getType();
      if (!ClusterComputeResource.class.getSimpleName().equals(type) && !HostSystem.class.getSimpleName().equals(type)) {
         throw new IllegalArgumentException("Unsupported ManagedObjectReference type given: " + type);
      }
   }
}
