package com.vmware.vsan.client.services.capability;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vsphere.client.vsan.base.data.VsanCapabilityData;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanCapabilityProvider {
   private static final Log _logger = LogFactory.getLog(VsanCapabilityProvider.class);
   @Autowired
   private ObjectReferenceService objectRefService;

   @TsService
   public VsanCapabilityData getVcCapabilityData(ManagedObjectReference moRef) {
      VsanCapabilityData capabilityData = new VsanCapabilityData();

      try {
         capabilityData = VsanCapabilityUtils.getVcCapabilities(moRef);
      } catch (Exception var4) {
         _logger.error("Cannot load capabilities", var4);
      }

      return capabilityData;
   }

   @TsService
   public VsanCapabilityData getClusterCapabilityData(ManagedObjectReference moRef) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(moRef);
      VsanCapabilityData capabilityData = new VsanCapabilityData();

      try {
         capabilityData = VsanCapabilityUtils.getCapabilities(clusterRef);
      } catch (Exception var5) {
         _logger.error("Cannot load capabilities", var5);
      }

      return capabilityData;
   }

   @TsService
   public VsanCapabilityData getHostCapabilityData(ManagedObjectReference moRef) {
      VsanCapabilityData capabilityData = new VsanCapabilityData();

      try {
         capabilityData = VsanCapabilityUtils.getCapabilities(moRef);
      } catch (Exception var4) {
         _logger.error("Cannot load capabilities", var4);
      }

      return capabilityData;
   }

   @TsService
   public Map getHostsCapabilitiyData(ManagedObjectReference[] hostRefs) {
      Map result = new HashMap();
      ManagedObjectReference[] var3 = hostRefs;
      int var4 = hostRefs.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ManagedObjectReference hostRef = var3[var5];
         result.put(this.objectRefService.getUid(hostRef), this.getHostCapabilityData(hostRef));
      }

      return result;
   }

   @TsService
   public boolean getIsDeduplicationSupported(ManagedObjectReference clusterRef) {
      boolean dedupSupported = VsanCapabilityUtils.isDeduplicationAndCompressionSupported(clusterRef);
      return dedupSupported;
   }

   @TsService
   public boolean getIsEncryptionSupported(ManagedObjectReference clusterRef) {
      boolean encryptionSupported = VsanCapabilityUtils.isDataAtRestEncryptionSupported(clusterRef);
      return encryptionSupported;
   }

   @TsService
   public Boolean getIsAllFlashSupportedOnHost(ManagedObjectReference hostRef) {
      return VsanCapabilityUtils.isAllFlashSupportedOnHost(hostRef);
   }

   @TsService
   public boolean getIsObjectIdentitiesSupportedOnCluster(ManagedObjectReference clusterRef) {
      return this.getClusterCapabilityData(clusterRef).isObjectIdentitiesSupported;
   }

   @TsService
   public boolean getIsObjectsHealthV2SupportedOnVc(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isObjectsHealthV2SupportedOnVc(clusterRef);
   }

   @TsService
   public boolean getIsPerfVerboseModeSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isPerfVerboseModeSupportedOnVc(clusterRef);
   }

   @TsService
   public boolean getIsPerfNetworkDiagnosticModeSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isPerfDiagnosticModeSupported(clusterRef);
   }

   @TsService
   public boolean getIsPerfDiagnosticsFeedbackSupportedOnVc(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isPerfDiagnosticsFeedbackSupportedOnVc(clusterRef);
   }

   @TsService
   public boolean getIsAdvancedClusterSettingsSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isAdvancedClusterSettingsSupported(clusterRef);
   }

   @TsService
   public boolean getIsRecreateDiskGroupSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isRecreateDiskGroupSupported(clusterRef);
   }

   @TsService
   public boolean getIsPurgeInaccessibleVmSwapObjectsSupported(ManagedObjectReference moRef) {
      return VsanCapabilityUtils.isPurgeInaccessibleVmSwapObjectsSupported(moRef);
   }

   @TsService
   public boolean getIsCapacityCustomizableThresholdsSupported(ManagedObjectReference moRef) {
      return VsanCapabilityUtils.isCapacityCustomizableThresholdsSupportedOnVc(moRef);
   }

   @TsService
   public boolean getIsUpdateVumReleaseCatalogOfflineSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isUpdateVumReleaseCatalogOfflineSupported(clusterRef);
   }

   @TsService
   public boolean getIsIscsiOnlineResizeSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isIscsiOnlineResizeSupportedOnCluster(clusterRef);
   }

   @TsService
   public boolean getIsIscsiStretchedClusterSupportedOnCluster(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isIscsiStretchedClusterSupportedOnCluster(clusterRef);
   }

   @TsService
   public boolean getIsVmLevelCapacityMonitoringSupported(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isVmLevelCapacityMonitoringSupportedOnVc(objectRef);
   }

   @TsService
   public boolean getIsSlackSpaceCapacitySupported(ManagedObjectReference clusterRef) {
      return this.getClusterCapabilityData(clusterRef).isSlackSpaceCapacitySupported;
   }

   @TsService
   public boolean getIsWhatIfCapacitySupported(ManagedObjectReference clusterRef) {
      return this.getClusterCapabilityData(clusterRef).isWhatIfCapacitySupported;
   }

   @TsService
   public boolean getIsHostReservedCapacitySupported(ManagedObjectReference vcRef) {
      return VsanCapabilityUtils.isHostReservedCapacitySupportedOnVc(vcRef);
   }

   @TsService
   public boolean getIsUnmountWithMaintenanceModeSupported(ManagedObjectReference moRef) {
      return VsanCapabilityUtils.isUnmountWithMaintenanceModeSupported(moRef);
   }

   @TsService
   public boolean getIsSharedWitnessSupportedOnVc(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isSharedWitnessSupportedOnVc(objectRef);
   }

   @TsService
   public boolean getIsSharedWitnessSupported(ManagedObjectReference hostRef) {
      return VsanCapabilityUtils.isSharedWitnessSupported(hostRef);
   }

   @TsService
   public boolean getIsDiskResourcePrecheckSupported(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isDiskResourcePrecheckSupported(objectRef);
   }

   @TsService
   public boolean getIsFileVolumesSupportedOnVc(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isFileVolumesSupportedOnVc(objectRef);
   }

   @TsService
   public boolean getIsHistoricalHealthSupported(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isHistoricalHealthSupported(objectRef);
   }

   @TsService
   public boolean getIsCsdSupported(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isCsdSupported(objectRef);
   }

   @TsService
   public boolean getIsHardwareManagementSupported(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isHardwareManagementSupported(objectRef);
   }

   @TsService
   public boolean isPersistenceServiceAirGapSupportedOnVc(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isPersistenceServiceAirGapSupportedOnVc(objectRef);
   }

   @TsService
   public boolean isCapacityCustomizableThresholdsSupportedOnVc(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isCapacityCustomizableThresholdsSupportedOnVc(objectRef);
   }

   @TsService
   public boolean getIsEnsureDurabilitySupported(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isEnsureDurabilitySupported(objectRef);
   }

   @TsService
   public boolean getIsDecomModeForVsanDirectDisksSupported(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isDecomModeForVsanDirectDisksSupportedOnVc(objectRef);
   }

   @TsService
   public boolean getIsDitSharedWitnessInteroperabilitySupported(ManagedObjectReference objectRef) {
      return VsanCapabilityUtils.isDitSharedWitnessInteroperabilitySupported(objectRef);
   }
}
