package com.vmware.vsan.client.services.capability;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapability;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapabilitySystem;
import com.vmware.vim.vsan.binding.vsan.version.version8;
import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import com.vmware.vsan.client.sessionmanager.common.VersionService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.cache.TimeBasedCacheEntry;
import com.vmware.vsphere.client.vsan.base.cache.TimeBasedCacheManager;
import com.vmware.vsphere.client.vsan.base.data.VsanCapabilityData;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanCapabilityCacheManager extends TimeBasedCacheManager {
   private static final Log _logger = LogFactory.getLog(VsanCapabilityCacheManager.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanCapabilityCacheManager.class);
   private final AsyncUserSessionService sessionService;
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VersionService versionService;
   @Autowired
   private VsanClient vsanClient;

   public VsanCapabilityCacheManager(AsyncUserSessionService sessionService, int expirationTimeMin, int expirationTimeMax, int trustPeriod, int cleanThreshold) {
      super(expirationTimeMin, expirationTimeMax, trustPeriod, cleanThreshold);
      this.sessionService = sessionService;
   }

   public VsanCapabilityData getVcCapabilities(ManagedObjectReference ref) {
      this.validateMoRef(ref);
      return (VsanCapabilityData)super.get(new VsanCapabilityDataParams(ref, VsanCapabilityDataParams.Type.VC));
   }

   public VsanCapabilityData getClusterCapabilities(ManagedObjectReference ref) {
      this.validateMoRefAndType(ref, ClusterComputeResource.class.getSimpleName());
      return (VsanCapabilityData)super.get(new VsanCapabilityDataParams(ref, VsanCapabilityDataParams.Type.CLUSTER));
   }

   public VsanCapabilityData getHostCapabilities(ManagedObjectReference ref) {
      this.validateMoRefAndType(ref, HostSystem.class.getSimpleName());
      return (VsanCapabilityData)super.get(new VsanCapabilityDataParams(ref, VsanCapabilityDataParams.Type.HOST));
   }

   protected String getKey(VsanCapabilityDataParams params) {
      switch(params.cacheType) {
      case VC:
         return params.moRef.getServerGuid();
      case CLUSTER:
      case HOST:
         return params.moRef.getServerGuid() + ":" + params.moRef.getValue();
      default:
         throw new UnsupportedOperationException("Unsupported cache manager type!");
      }
   }

   protected String sessionKey() {
      String key = this.sessionService.getUserSession().clientId;
      if (key == null) {
         throw new RuntimeException("Failed to retrieve the clientId from the session. Most probably, the threadlocal context is not correctly set. Session: " + this.sessionService.getUserSession());
      } else {
         return key;
      }
   }

   protected TimeBasedCacheEntry createEntry(VsanCapabilityDataParams params) {
      switch(params.cacheType) {
      case VC:
         return new VsanCapabilityCacheManager.VcCapabilityTimeBasedCacheEntry(params);
      case CLUSTER:
         return new VsanCapabilityCacheManager.ClusterCapabilityTimeBasedCacheEntry(params);
      case HOST:
         return new VsanCapabilityCacheManager.HostCapabilityTimeBasedCacheEntry(params);
      default:
         throw new UnsupportedOperationException("Unsupported cache manager type!");
      }
   }

   private void validateMoRefAndType(ManagedObjectReference moRef, String expectedType) {
      this.validateMoRef(moRef);
      String moRefType = moRef.getType();
      if (!expectedType.equals(moRefType)) {
         throw new IllegalArgumentException(String.format("Unsupported ManagedObjectReference type: %s. Expected is: %s.", moRefType, expectedType));
      }
   }

   private void validateMoRef(ManagedObjectReference moRef) {
      Validate.notNull(moRef);
      Validate.notEmpty(moRef.getServerGuid());
   }

   private class HostCapabilityTimeBasedCacheEntry extends VsanCapabilityCacheManager.VsanTimeBasedCacheEntry {
      public HostCapabilityTimeBasedCacheEntry(VsanCapabilityDataParams params) {
         super(params);
      }

      protected ManagedObjectReference[] getArgs() {
         ManagedObjectReference clonedMoRef = new ManagedObjectReference(this.params.moRef.getType(), this.params.moRef.getValue());
         return new ManagedObjectReference[]{clonedMoRef};
      }

      protected String getValidationToken() {
         VcConnection vcConnection = VsanCapabilityCacheManager.this.vcClient.getConnection(this.params.moRef.getServerGuid());
         Throwable var2 = null;

         String var4;
         try {
            HostSystem host = (HostSystem)vcConnection.createStub(HostSystem.class, this.params.moRef);
            var4 = host.getRuntime().getConnectionState().name();
         } catch (Throwable var13) {
            var2 = var13;
            throw var13;
         } finally {
            if (vcConnection != null) {
               if (var2 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var12) {
                     var2.addSuppressed(var12);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return var4;
      }
   }

   private class ClusterCapabilityTimeBasedCacheEntry extends VsanCapabilityCacheManager.VsanTimeBasedCacheEntry {
      public ClusterCapabilityTimeBasedCacheEntry(VsanCapabilityDataParams params) {
         super(params);
      }

      protected VsanCapabilityData load() {
         VsanCapabilityData result = new VsanCapabilityData();

         try {
            VsanProfiler.Point point = VsanCapabilityCacheManager._profiler.point("VsanCapabilitySystem.getCapabilities");
            Throwable var3 = null;

            try {
               VsanConnection vsanConnection = VsanCapabilityCacheManager.this.vsanClient.getConnection(this.params.moRef.getServerGuid());
               Throwable var5 = null;

               try {
                  VsanCapabilitySystem capSystem = vsanConnection.getVsanCapabilitySystem();
                  ManagedObjectReference[] args = this.getArgs();
                  VsanCapability[] capabilities = capSystem.getCapabilities(args);
                  if (!ArrayUtils.isEmpty(capabilities)) {
                     result = VsanCapabilityData.fromVsanCapability(capabilities[0]);
                     if (capabilities.length > 1) {
                        for(int i = 1; i < capabilities.length; ++i) {
                           VsanCapability capability = capabilities[i];
                           VsanCapabilityData hostCapabilities = VsanCapabilityData.fromVsanCapability(capability);
                           result.isCapabilitiesSupported &= hostCapabilities.isDisconnected || hostCapabilities.isCapabilitiesSupported;
                           result.isAllFlashSupported &= hostCapabilities.isDisconnected || hostCapabilities.isAllFlashSupported;
                           result.isStretchedClusterSupported &= hostCapabilities.isDisconnected || hostCapabilities.isStretchedClusterSupported;
                           result.isClusterConfigSupported &= hostCapabilities.isDisconnected || hostCapabilities.isClusterConfigSupported;
                           result.isDeduplicationAndCompressionSupported &= hostCapabilities.isDisconnected || hostCapabilities.isDeduplicationAndCompressionSupported;
                           result.isCompressionOnlySupported &= hostCapabilities.isDisconnected || hostCapabilities.isCompressionOnlySupported;
                           result.isUpgradeSupported &= hostCapabilities.isDisconnected || hostCapabilities.isUpgradeSupported;
                           result.isObjectIdentitiesSupported &= hostCapabilities.isDisconnected || hostCapabilities.isObjectIdentitiesSupported;
                           result.isObjectsHealthV2Supported &= hostCapabilities.isDisconnected || hostCapabilities.isObjectsHealthV2Supported;
                           result.isIscsiTargetsSupported &= hostCapabilities.isDisconnected || hostCapabilities.isIscsiTargetsSupported;
                           result.isWitnessManagementSupported &= hostCapabilities.isDisconnected || hostCapabilities.isWitnessManagementSupported;
                           result.isPerfVerboseModeSupported &= hostCapabilities.isDisconnected || hostCapabilities.isPerfVerboseModeSupported;
                           result.isPerfSvcAutoConfigSupported &= hostCapabilities.isDisconnected || hostCapabilities.isPerfSvcAutoConfigSupported;
                           result.isConfigAssistSupported &= hostCapabilities.isDisconnected || hostCapabilities.isConfigAssistSupported;
                           result.isUpdatesMgmtSupported &= hostCapabilities.isDisconnected || hostCapabilities.isUpdatesMgmtSupported;
                           result.isWhatIfComplianceSupported &= hostCapabilities.isDisconnected || hostCapabilities.isWhatIfComplianceSupported;
                           result.isPerfAnalysisSupported &= hostCapabilities.isDisconnected || hostCapabilities.isPerfAnalysisSupported;
                           result.isResyncThrottlingSupported &= hostCapabilities.isDisconnected || hostCapabilities.isResyncThrottlingSupported;
                           result.isEncryptionSupported &= hostCapabilities.isDisconnected || hostCapabilities.isEncryptionSupported;
                           result.isVsanVumIntegrationSupported &= hostCapabilities.isDisconnected || hostCapabilities.isVsanVumIntegrationSupported;
                           result.isRepairTimerInResyncStatsSupported &= hostCapabilities.isDisconnected || hostCapabilities.isRepairTimerInResyncStatsSupported;
                           result.isFileServiceSupported &= hostCapabilities.isDisconnected || hostCapabilities.isFileServiceSupported;
                           result.isRdmaSupported &= hostCapabilities.isDisconnected || hostCapabilities.isRdmaSupported;
                           result.isResyncETAImprovementSupported &= hostCapabilities.isDisconnected || hostCapabilities.isResyncETAImprovementSupported;
                           result.isGuestTrimUnmapSupported &= hostCapabilities.isDisconnected || hostCapabilities.isGuestTrimUnmapSupported;
                           result.isIscsiOnlineResizeSupported &= hostCapabilities.isDisconnected || hostCapabilities.isIscsiOnlineResizeSupported;
                           result.isIscsiStretchedClusterSupported &= hostCapabilities.isDisconnected || hostCapabilities.isIscsiStretchedClusterSupported;
                           result.isHostResourcePrecheckSupported &= hostCapabilities.isDisconnected || hostCapabilities.isHostResourcePrecheckSupported;
                           result.isDiskResourcePrecheckSupported &= hostCapabilities.isDisconnected || hostCapabilities.isDiskResourcePrecheckSupported;
                           result.isHistoricalHealthSupported &= hostCapabilities.isDisconnected || hostCapabilities.isHistoricalHealthSupported;
                           result.isSlackSpaceCapacitySupported &= hostCapabilities.isDisconnected || hostCapabilities.isSlackSpaceCapacitySupported;
                           result.isCsdSupported &= hostCapabilities.isDisconnected || hostCapabilities.isCsdSupported;
                           result.isHardwareManagementSupported &= hostCapabilities.isDisconnected || hostCapabilities.isHardwareManagementSupported;
                           result.isDataInTransitEncryptionSupported &= hostCapabilities.isDisconnected || hostCapabilities.isDataInTransitEncryptionSupported;
                           result.isRealTimePhysicalDiskHealthSupported &= hostCapabilities.isDisconnected || hostCapabilities.isRealTimePhysicalDiskHealthSupported;
                           result.isDefaultGatewaySupported &= hostCapabilities.isDisconnected || hostCapabilities.isCsdSupported;
                           result.isManagedVmfsSupported &= hostCapabilities.isDisconnected || hostCapabilities.isManagedVmfsSupported;
                           result.isSlackSpaceReservationSupported &= hostCapabilities.isDisconnected || hostCapabilities.isSlackSpaceReservationSupported;
                           result.isAdaptiveResyncOnlySupported &= hostCapabilities.isDisconnected || hostCapabilities.isRepairTimerInResyncStatsSupported;
                           result.isIoInsightSupported &= hostCapabilities.isIoInsightSupported;
                           result.isSharedWitnessSupported &= hostCapabilities.isSharedWitnessSupported;
                           result.isFileServiceKerberosSupported &= hostCapabilities.isFileServiceKerberosSupported;
                           result.isManagedPMemSupported &= hostCapabilities.isDisconnected || hostCapabilities.isManagedPMemSupported;
                           result.isEnsureDurabilitySupported &= hostCapabilities.isDisconnected || hostCapabilities.isEnsureDurabilitySupported;
                           result.isComputeOnlySupported &= hostCapabilities.isDisconnected || hostCapabilities.isComputeOnlySupported;
                           result.isDitSharedWitnessInteroperabilitySupported &= hostCapabilities.isDisconnected || hostCapabilities.isDitSharedWitnessInteroperabilitySupported;
                           result.isTopContributorsSupported &= hostCapabilities.isTopContributorsSupported;
                        }
                     }
                  }
               } catch (Throwable var35) {
                  var5 = var35;
                  throw var35;
               } finally {
                  if (vsanConnection != null) {
                     if (var5 != null) {
                        try {
                           vsanConnection.close();
                        } catch (Throwable var34) {
                           var5.addSuppressed(var34);
                        }
                     } else {
                        vsanConnection.close();
                     }
                  }

               }
            } catch (Throwable var37) {
               var3 = var37;
               throw var37;
            } finally {
               if (point != null) {
                  if (var3 != null) {
                     try {
                        point.close();
                     } catch (Throwable var33) {
                        var3.addSuppressed(var33);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var39) {
            VsanCapabilityCacheManager._logger.error("Cannot retrieve capabilities", var39);
         }

         return result;
      }

      protected String getValidationToken() {
         VcConnection vcConnection = VsanCapabilityCacheManager.this.vcClient.getConnection(this.params.moRef.getServerGuid());
         Throwable var2 = null;

         try {
            ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, this.params.moRef);
            Set hosts = new HashSet();
            ManagedObjectReference[] var5 = cluster.getHost();
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               ManagedObjectReference hostRef = var5[var7];
               hosts.add(hostRef);
            }

            String var18 = String.valueOf(hosts.hashCode());
            return var18;
         } catch (Throwable var16) {
            var2 = var16;
            throw var16;
         } finally {
            if (vcConnection != null) {
               if (var2 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var15) {
                     var2.addSuppressed(var15);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      }

      protected ManagedObjectReference[] getArgs() {
         ManagedObjectReference clonedMoRef = new ManagedObjectReference(this.params.moRef.getType(), this.params.moRef.getValue());
         return new ManagedObjectReference[]{clonedMoRef};
      }
   }

   private class VcCapabilityTimeBasedCacheEntry extends VsanCapabilityCacheManager.VsanTimeBasedCacheEntry {
      public VcCapabilityTimeBasedCacheEntry(VsanCapabilityDataParams params) {
         super(params);
      }

      protected VsanCapabilityData load() {
         VsanCapabilityData result = super.load();
         result.isUnmountWithMaintenanceModeSupported = VsanCapabilityCacheManager.this.versionService.isVsanVmodlVersionHigherThan(this.params.moRef, version8.class);
         return result;
      }

      protected ManagedObjectReference[] getArgs() {
         return new ManagedObjectReference[0];
      }
   }

   private abstract class VsanTimeBasedCacheEntry extends TimeBasedCacheEntry {
      protected final VsanCapabilityDataParams params;

      public VsanTimeBasedCacheEntry(VsanCapabilityDataParams params) {
         this.params = params;
      }

      protected VsanCapabilityData load() {
         VsanCapabilityData result = new VsanCapabilityData();

         try {
            VsanProfiler.Point point = VsanCapabilityCacheManager._profiler.point("VsanCapabilitySystem.getCapabilities");
            Throwable var3 = null;

            try {
               VsanConnection vsanConnection = VsanCapabilityCacheManager.this.vsanClient.getConnection(this.params.moRef.getServerGuid());
               Throwable var5 = null;

               try {
                  VsanCapabilitySystem capSystem = vsanConnection.getVsanCapabilitySystem();
                  ManagedObjectReference[] args = this.getArgs();
                  VsanCapability[] capabilities = capSystem.getCapabilities(args);
                  if (!ArrayUtils.isEmpty(capabilities)) {
                     result = VsanCapabilityData.fromVsanCapability(capabilities[0]);
                  }
               } catch (Throwable var32) {
                  var5 = var32;
                  throw var32;
               } finally {
                  if (vsanConnection != null) {
                     if (var5 != null) {
                        try {
                           vsanConnection.close();
                        } catch (Throwable var31) {
                           var5.addSuppressed(var31);
                        }
                     } else {
                        vsanConnection.close();
                     }
                  }

               }
            } catch (Throwable var34) {
               var3 = var34;
               throw var34;
            } finally {
               if (point != null) {
                  if (var3 != null) {
                     try {
                        point.close();
                     } catch (Throwable var30) {
                        var3.addSuppressed(var30);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var36) {
            VsanCapabilityCacheManager._logger.error("Cannot retrieve capabilities", var36);
         }

         return result;
      }

      protected String getValidationToken() {
         return null;
      }

      protected abstract ManagedObjectReference[] getArgs();
   }
}
