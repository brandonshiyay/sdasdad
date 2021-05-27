package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapability;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapabilityStatus;
import com.vmware.vsphere.client.vsan.base.cache.Cacheable;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class VsanCapabilityData implements Cacheable {
   public boolean isDisconnected;
   public boolean isCapabilitiesSupported;
   public boolean isAllFlashSupported;
   public boolean isStretchedClusterSupported;
   public boolean isClusterConfigSupported;
   public boolean isDeduplicationAndCompressionSupported;
   public boolean isUpgradeSupported;
   public boolean isObjectIdentitiesSupported;
   public boolean isObjectsHealthV2Supported;
   public boolean isIscsiTargetsSupported;
   public boolean isWitnessManagementSupported;
   public boolean isPerfVerboseModeSupported;
   public boolean isPerfSvcAutoConfigSupported;
   public boolean isConfigAssistSupported;
   public boolean isUpdatesMgmtSupported;
   public boolean isWhatIfComplianceSupported;
   public boolean isPerfAnalysisSupported;
   public boolean isResyncThrottlingSupported;
   public boolean isEncryptionSupported;
   public boolean isWhatIfSupported;
   public boolean isCloudHealthSupported;
   public boolean isResyncEnhancedApiSupported;
   public boolean isFileServiceSupported;
   public boolean isCnsVolumesSupported;
   public boolean isNetworkPerfTestSupported;
   public boolean isVsanVumIntegrationSupported;
   public boolean isWhatIfCapacitySupported;
   public boolean isHistoricalCapacitySupported;
   public boolean isNestedFdsSupported;
   public boolean isRepairTimerInResyncStatsSupported;
   public boolean isPurgeInaccessibleVmSwapObjectsSupported;
   public boolean isRecreateDiskGroupSupported;
   public boolean isUpdateVumReleaseCatalogOfflineSupported;
   public boolean isAdvancedClusterOptionsSupported;
   public boolean isPerfDiagnosticModeSupported;
   public boolean isPerfDiagnosticsFeedbackSupported;
   public boolean isAdvancedPerformanceSupported;
   public boolean isGetHclLastUpdateOnVcSupported;
   public boolean isAutomaticRebalanceSupported;
   public boolean isRdmaSupported;
   public boolean isResyncETAImprovementSupported;
   public boolean isGuestTrimUnmapSupported;
   public boolean isIscsiOnlineResizeSupported;
   public boolean isIscsiStretchedClusterSupported;
   public boolean isVumBaselineRecommendationSupported;
   public boolean isSupportInsightSupported;
   public boolean isHostResourcePrecheckSupported;
   public boolean isDiskResourcePrecheckSupported;
   public boolean isVerboseModeInClusterConfigurationSupported;
   public boolean isVmLevelCapacityMonitoringSupported;
   public boolean isSlackSpaceCapacitySupported;
   public boolean isHostReservedCapacitySupported;
   public boolean isVsanCpuMetricsSupported;
   public boolean isFileServiceKerberosSupported;
   public boolean isUnmountWithMaintenanceModeSupported;
   public boolean isSharedWitnessSupported;
   public boolean isPmanIntegrationSupported;
   public boolean isPersistenceServiceSupported;
   public boolean isFileVolumesSupported;
   public boolean isNativeLargeClusterSupported;
   public boolean isIoInsightSupported;
   public boolean isHistoricalHealthSupported;
   public boolean isDataInTransitEncryptionSupported;
   public boolean isSmbProtocolSupported;
   public boolean isSmbPerformanceSupported;
   public boolean isNfsv3ProtocolSupported;
   public boolean isCsdSupported;
   public boolean isMultiVmPerfSupported;
   public boolean isHardwareManagementSupported;
   public boolean isCompressionOnlySupported;
   public boolean isRealTimePhysicalDiskHealthSupported;
   public boolean isDefaultGatewaySupported;
   public boolean isManagedVmfsSupported;
   public boolean isSlackSpaceReservationSupported;
   public boolean isIpRemovalInEditModeSupported;
   public boolean isFileSharePaginationSupported;
   public boolean isPersistenceServiceAirGapSupported;
   public boolean isPolicySatisfiabilitySupported;
   public boolean isManagedPMemSupported;
   public boolean isCapacityOversubscriptionSupported;
   public boolean isCapacityCustomizableThresholdsSupported;
   public boolean isFileServiceStretchedClusterSupported;
   public boolean isFileServiceOweSupported;
   public boolean isFileServiceSnapshotSupported;
   public boolean isVmIoDiagnosticsSupported;
   public boolean isNetworkDiagnosticsSupported;
   public boolean isEnsureDurabilitySupported;
   public boolean isDiskMgmtRedesignSupported;
   public boolean isComputeOnlySupported;
   public boolean isDitSharedWitnessInteroperabilitySupported;
   public boolean isTopContributorsSupported;
   public boolean isDecomModeForVsanDirectDisksSupported;
   public boolean isFileAnalyticsSupported;
   public boolean isVsanHciMeshPolicySupported;
   public boolean isPersistenceResourceCheckSupported;
   public boolean isAdaptiveResyncOnlySupported;

   public VsanCapabilityData clone() {
      VsanCapabilityData clone = new VsanCapabilityData();
      clone.isDisconnected = this.isDisconnected;
      clone.isClusterConfigSupported = this.isClusterConfigSupported;
      clone.isDeduplicationAndCompressionSupported = this.isDeduplicationAndCompressionSupported;
      clone.isObjectIdentitiesSupported = this.isObjectIdentitiesSupported;
      clone.isObjectsHealthV2Supported = this.isObjectsHealthV2Supported;
      clone.isUpgradeSupported = this.isUpgradeSupported;
      clone.isStretchedClusterSupported = this.isStretchedClusterSupported;
      clone.isAllFlashSupported = this.isAllFlashSupported;
      clone.isCapabilitiesSupported = this.isCapabilitiesSupported;
      clone.isIscsiTargetsSupported = this.isIscsiTargetsSupported;
      clone.isWitnessManagementSupported = this.isWitnessManagementSupported;
      clone.isPerfVerboseModeSupported = this.isPerfVerboseModeSupported;
      clone.isPerfSvcAutoConfigSupported = this.isPerfSvcAutoConfigSupported;
      clone.isConfigAssistSupported = this.isConfigAssistSupported;
      clone.isUpdatesMgmtSupported = this.isUpdatesMgmtSupported;
      clone.isWhatIfComplianceSupported = this.isWhatIfComplianceSupported;
      clone.isPerfAnalysisSupported = this.isPerfAnalysisSupported;
      clone.isResyncThrottlingSupported = this.isResyncThrottlingSupported;
      clone.isEncryptionSupported = this.isEncryptionSupported;
      clone.isWhatIfSupported = this.isWhatIfSupported;
      clone.isCloudHealthSupported = this.isCloudHealthSupported;
      clone.isResyncEnhancedApiSupported = this.isResyncEnhancedApiSupported;
      clone.isFileServiceSupported = this.isFileServiceSupported;
      clone.isCnsVolumesSupported = this.isCnsVolumesSupported;
      clone.isNetworkPerfTestSupported = this.isNetworkPerfTestSupported;
      clone.isVsanVumIntegrationSupported = this.isVsanVumIntegrationSupported;
      clone.isWhatIfCapacitySupported = this.isWhatIfCapacitySupported;
      clone.isHistoricalCapacitySupported = this.isHistoricalCapacitySupported;
      clone.isNestedFdsSupported = this.isNestedFdsSupported;
      clone.isRepairTimerInResyncStatsSupported = this.isRepairTimerInResyncStatsSupported;
      clone.isPurgeInaccessibleVmSwapObjectsSupported = this.isPurgeInaccessibleVmSwapObjectsSupported;
      clone.isRecreateDiskGroupSupported = this.isRecreateDiskGroupSupported;
      clone.isUpdateVumReleaseCatalogOfflineSupported = this.isUpdateVumReleaseCatalogOfflineSupported;
      clone.isAdvancedClusterOptionsSupported = this.isAdvancedClusterOptionsSupported;
      clone.isPerfDiagnosticModeSupported = this.isPerfDiagnosticModeSupported;
      clone.isPerfDiagnosticsFeedbackSupported = this.isPerfDiagnosticsFeedbackSupported;
      clone.isAdvancedPerformanceSupported = this.isAdvancedPerformanceSupported;
      clone.isGetHclLastUpdateOnVcSupported = this.isGetHclLastUpdateOnVcSupported;
      clone.isAutomaticRebalanceSupported = this.isAutomaticRebalanceSupported;
      clone.isRdmaSupported = this.isRdmaSupported;
      clone.isResyncETAImprovementSupported = this.isResyncETAImprovementSupported;
      clone.isGuestTrimUnmapSupported = this.isGuestTrimUnmapSupported;
      clone.isIscsiOnlineResizeSupported = this.isIscsiOnlineResizeSupported;
      clone.isIscsiStretchedClusterSupported = this.isIscsiStretchedClusterSupported;
      clone.isVumBaselineRecommendationSupported = this.isVumBaselineRecommendationSupported;
      clone.isSupportInsightSupported = this.isSupportInsightSupported;
      clone.isHostResourcePrecheckSupported = this.isHostResourcePrecheckSupported;
      clone.isDiskResourcePrecheckSupported = this.isDiskResourcePrecheckSupported;
      clone.isVerboseModeInClusterConfigurationSupported = this.isVerboseModeInClusterConfigurationSupported;
      clone.isVmLevelCapacityMonitoringSupported = this.isVmLevelCapacityMonitoringSupported;
      clone.isSlackSpaceCapacitySupported = this.isSlackSpaceCapacitySupported;
      clone.isHostReservedCapacitySupported = this.isHostReservedCapacitySupported;
      clone.isVsanCpuMetricsSupported = this.isVsanCpuMetricsSupported;
      clone.isFileServiceKerberosSupported = this.isFileServiceKerberosSupported;
      clone.isUnmountWithMaintenanceModeSupported = this.isUnmountWithMaintenanceModeSupported;
      clone.isSharedWitnessSupported = this.isSharedWitnessSupported;
      clone.isPmanIntegrationSupported = this.isPmanIntegrationSupported;
      clone.isPersistenceServiceSupported = this.isPersistenceServiceSupported;
      clone.isFileVolumesSupported = this.isFileVolumesSupported;
      clone.isNativeLargeClusterSupported = this.isNativeLargeClusterSupported;
      clone.isIoInsightSupported = this.isIoInsightSupported;
      clone.isHistoricalHealthSupported = this.isHistoricalHealthSupported;
      clone.isDataInTransitEncryptionSupported = this.isDataInTransitEncryptionSupported;
      clone.isSmbProtocolSupported = this.isSmbProtocolSupported;
      clone.isNfsv3ProtocolSupported = this.isNfsv3ProtocolSupported;
      clone.isCsdSupported = this.isCsdSupported;
      clone.isMultiVmPerfSupported = this.isMultiVmPerfSupported;
      clone.isHardwareManagementSupported = this.isHardwareManagementSupported;
      clone.isCompressionOnlySupported = this.isCompressionOnlySupported;
      clone.isRealTimePhysicalDiskHealthSupported = this.isRealTimePhysicalDiskHealthSupported;
      clone.isDefaultGatewaySupported = this.isDefaultGatewaySupported;
      clone.isManagedVmfsSupported = this.isManagedVmfsSupported;
      clone.isSlackSpaceReservationSupported = this.isSlackSpaceReservationSupported;
      clone.isIpRemovalInEditModeSupported = this.isIpRemovalInEditModeSupported;
      clone.isFileSharePaginationSupported = this.isFileSharePaginationSupported;
      clone.isAdaptiveResyncOnlySupported = this.isAdaptiveResyncOnlySupported;
      clone.isPersistenceServiceAirGapSupported = this.isPersistenceServiceAirGapSupported;
      clone.isPolicySatisfiabilitySupported = this.isPolicySatisfiabilitySupported;
      clone.isManagedPMemSupported = this.isManagedPMemSupported;
      clone.isCapacityOversubscriptionSupported = this.isCapacityOversubscriptionSupported;
      clone.isFileServiceStretchedClusterSupported = this.isFileServiceStretchedClusterSupported;
      clone.isFileServiceSnapshotSupported = this.isFileServiceSnapshotSupported;
      clone.isFileServiceOweSupported = this.isFileServiceOweSupported;
      clone.isSmbPerformanceSupported = this.isSmbPerformanceSupported;
      clone.isVmIoDiagnosticsSupported = this.isVmIoDiagnosticsSupported;
      clone.isNetworkDiagnosticsSupported = this.isNetworkDiagnosticsSupported;
      clone.isEnsureDurabilitySupported = this.isEnsureDurabilitySupported;
      clone.isCapacityCustomizableThresholdsSupported = this.isCapacityCustomizableThresholdsSupported;
      clone.isDiskMgmtRedesignSupported = this.isDiskMgmtRedesignSupported;
      clone.isComputeOnlySupported = this.isComputeOnlySupported;
      clone.isDitSharedWitnessInteroperabilitySupported = this.isDitSharedWitnessInteroperabilitySupported;
      clone.isTopContributorsSupported = this.isTopContributorsSupported;
      clone.isDecomModeForVsanDirectDisksSupported = this.isDecomModeForVsanDirectDisksSupported;
      clone.isFileAnalyticsSupported = this.isFileAnalyticsSupported;
      clone.isVsanHciMeshPolicySupported = this.isVsanHciMeshPolicySupported;
      clone.isPersistenceResourceCheckSupported = this.isPersistenceResourceCheckSupported;
      return clone;
   }

   public static VsanCapabilityData fromVsanCapability(VsanCapability vsanCapability) {
      VsanCapabilityData result = new VsanCapabilityData();
      String[] var2;
      int var3;
      int var4;
      String capability;
      if (vsanCapability.statuses != null) {
         var2 = vsanCapability.statuses;
         var3 = var2.length;

         for(var4 = 0; var4 < var3; ++var4) {
            capability = var2[var4];
            if (capability.equals(VsanCapabilityStatus.disconnected.toString())) {
               result.isDisconnected = true;
               break;
            }
         }
      }

      if (vsanCapability != null && ArrayUtils.isNotEmpty(vsanCapability.capabilities)) {
         var2 = vsanCapability.capabilities;
         var3 = var2.length;

         for(var4 = 0; var4 < var3; ++var4) {
            capability = var2[var4];
            byte var7 = -1;
            switch(capability.hashCode()) {
            case -2084710702:
               if (capability.equals("capacityreservation")) {
                  var7 = 69;
               }
               break;
            case -2036548965:
               if (capability.equals("whatifcapacity")) {
                  var7 = 24;
               }
               break;
            case -1899620047:
               if (capability.equals("diagnosticsfeedback")) {
                  var7 = 33;
               }
               break;
            case -1856124518:
               if (capability.equals("vsanmanagedpmem")) {
                  var7 = 72;
               }
               break;
            case -1855945735:
               if (capability.equals("vsanmanagedvmfs")) {
                  var7 = 68;
               }
               break;
            case -1854461844:
               if (capability.equals("updatevumreleasecatalogoffline")) {
                  var7 = 31;
               }
               break;
            case -1806256184:
               if (capability.equals("perfsvcautoconfig")) {
                  var7 = 10;
               }
               break;
            case -1804936456:
               if (capability.equals("throttleresync")) {
                  var7 = 15;
               }
               break;
            case -1730152928:
               if (capability.equals("vsanfileanalytics")) {
                  var7 = 85;
               }
               break;
            case -1721901146:
               if (capability.equals("historicalhealth")) {
                  var7 = 60;
               }
               break;
            case -1682528408:
               if (capability.equals("vitonlineresize")) {
                  var7 = 40;
               }
               break;
            case -1544169576:
               if (capability.equals("netperftest")) {
                  var7 = 22;
               }
               break;
            case -1512632445:
               if (capability.equals("encryption")) {
                  var7 = 16;
               }
               break;
            case -1379946052:
               if (capability.equals("clusterconfig")) {
                  var7 = 3;
               }
               break;
            case -1331573079:
               if (capability.equals("dit4sw")) {
                  var7 = 82;
               }
               break;
            case -1195883754:
               if (capability.equals("performanceforsupport")) {
                  var7 = 34;
               }
               break;
            case -1162221165:
               if (capability.equals("dataefficiency")) {
                  var7 = 4;
               }
               break;
            case -1160008507:
               if (capability.equals("perfanalysis")) {
                  var7 = 14;
               }
               break;
            case -1026791641:
               if (capability.equals("dataintransitencryption")) {
                  var7 = 61;
               }
               break;
            case -983973403:
               if (capability.equals("vsanobjhealthv2")) {
                  var7 = 73;
               }
               break;
            case -938329615:
               if (capability.equals("remotedatastore")) {
                  var7 = 64;
               }
               break;
            case -936991535:
               if (capability.equals("cloudhealth")) {
                  var7 = 18;
               }
               break;
            case -903430425:
               if (capability.equals("vmlevelcapacity")) {
                  var7 = 47;
               }
               break;
            case -831219140:
               if (capability.equals("witnessmanagement")) {
                  var7 = 8;
               }
               break;
            case -783669992:
               if (capability.equals("capability")) {
                  var7 = 1;
               }
               break;
            case -713444186:
               if (capability.equals("gethcllastupdateonvc")) {
                  var7 = 35;
               }
               break;
            case -711177680:
               if (capability.equals("slackspacecapacity")) {
                  var7 = 48;
               }
               break;
            case -663719842:
               if (capability.equals("capacitycustomizablethresholds")) {
                  var7 = 75;
               }
               break;
            case -601809612:
               if (capability.equals("sharedwitness")) {
                  var7 = 54;
               }
               break;
            case -541370576:
               if (capability.equals("vsandirectdiskdecom")) {
                  var7 = 84;
               }
               break;
            case -525799692:
               if (capability.equals("repairtimerinresyncstats")) {
                  var7 = 27;
               }
               break;
            case -458757541:
               if (capability.equals("objectidentities")) {
                  var7 = 5;
               }
               break;
            case -335589684:
               if (capability.equals("genericnestedfd")) {
                  var7 = 26;
               }
               break;
            case -299262582:
               if (capability.equals("hostreservedcapacity")) {
                  var7 = 49;
               }
               break;
            case -295044483:
               if (capability.equals("fileservicesnapshot")) {
                  var7 = 51;
               }
               break;
            case -276865004:
               if (capability.equals("fileservicekerberos")) {
                  var7 = 53;
               }
               break;
            case -233924644:
               if (capability.equals("perfsvcvsancpumetrics")) {
                  var7 = 50;
               }
               break;
            case -231171556:
               if (capability.equals("upgrade")) {
                  var7 = 6;
               }
               break;
            case -213231445:
               if (capability.equals("capacityoversubscription")) {
                  var7 = 74;
               }
               break;
            case -144980092:
               if (capability.equals("historicalcapacity")) {
                  var7 = 25;
               }
               break;
            case -86618386:
               if (capability.equals("perfsvcverbosemode")) {
                  var7 = 9;
               }
               break;
            case -85300235:
               if (capability.equals("vsanclient")) {
                  var7 = 81;
               }
               break;
            case -62951102:
               if (capability.equals("capacityevaluationonvc")) {
                  var7 = 71;
               }
               break;
            case -39348687:
               if (capability.equals("cnsvolumes")) {
                  var7 = 21;
               }
               break;
            case -38088944:
               if (capability.equals("datapersistresourcecheck")) {
                  var7 = 87;
               }
               break;
            case 3593415:
               if (capability.equals("umap")) {
                  var7 = 39;
               }
               break;
            case 46516698:
               if (capability.equals("fileservices")) {
                  var7 = 20;
               }
               break;
            case 72832530:
               if (capability.equals("compressiononly")) {
                  var7 = 67;
               }
               break;
            case 84189054:
               if (capability.equals("automaticrebalance")) {
                  var7 = 36;
               }
               break;
            case 92063072:
               if (capability.equals("complianceprecheck")) {
                  var7 = 13;
               }
               break;
            case 151784217:
               if (capability.equals("nativelargeclustersupport")) {
                  var7 = 58;
               }
               break;
            case 289188182:
               if (capability.equals("enhancedresyncapi")) {
                  var7 = 19;
               }
               break;
            case 521103856:
               if (capability.equals("diskresourceprecheck")) {
                  var7 = 45;
               }
               break;
            case 560333555:
               if (capability.equals("recreatediskgroup")) {
                  var7 = 30;
               }
               break;
            case 599591979:
               if (capability.equals("configassist")) {
                  var7 = 11;
               }
               break;
            case 613526532:
               if (capability.equals("purgeinaccessiblevmswapobjects")) {
                  var7 = 29;
               }
               break;
            case 658350895:
               if (capability.equals("vmIoDiagnostics")) {
                  var7 = 77;
               }
               break;
            case 821282800:
               if (capability.equals("wcpappplatform")) {
                  var7 = 56;
               }
               break;
            case 866526109:
               if (capability.equals("filevolumes")) {
                  var7 = 57;
               }
               break;
            case 883094367:
               if (capability.equals("fileservicenfsv3")) {
                  var7 = 63;
               }
               break;
            case 1114122305:
               if (capability.equals("decomwhatif")) {
                  var7 = 17;
               }
               break;
            case 1148008019:
               if (capability.equals("resourceprecheck")) {
                  var7 = 44;
               }
               break;
            case 1223974857:
               if (capability.equals("perfsvctwoyaxisgraph")) {
                  var7 = 65;
               }
               break;
            case 1232468279:
               if (capability.equals("vitstretchedcluster")) {
                  var7 = 41;
               }
               break;
            case 1252691946:
               if (capability.equals("fullStackFw")) {
                  var7 = 23;
               }
               break;
            case 1265995522:
               if (capability.equals("clusteradvancedoptions")) {
                  var7 = 28;
               }
               break;
            case 1330664524:
               if (capability.equals("vumbaselinerecommendation")) {
                  var7 = 42;
               }
               break;
            case 1331509170:
               if (capability.equals("ioinsight")) {
                  var7 = 59;
               }
               break;
            case 1338309188:
               if (capability.equals("firmwareupdate")) {
                  var7 = 12;
               }
               break;
            case 1344181014:
               if (capability.equals("stretchedcluster")) {
                  var7 = 2;
               }
               break;
            case 1359008240:
               if (capability.equals("vsanrdma")) {
                  var7 = 37;
               }
               break;
            case 1372746639:
               if (capability.equals("diskmgmtredesign")) {
                  var7 = 80;
               }
               break;
            case 1374495949:
               if (capability.equals("topcontributors")) {
                  var7 = 83;
               }
               break;
            case 1442017737:
               if (capability.equals("fileservicesc")) {
                  var7 = 76;
               }
               break;
            case 1481948761:
               if (capability.equals("pspairgap")) {
                  var7 = 70;
               }
               break;
            case 1487189745:
               if (capability.equals("verbosemodeconfiguration")) {
                  var7 = 46;
               }
               break;
            case 1534447273:
               if (capability.equals("hardwaremgmt")) {
                  var7 = 66;
               }
               break;
            case 1567155603:
               if (capability.equals("iscsitargets")) {
                  var7 = 7;
               }
               break;
            case 1612496557:
               if (capability.equals("hcimeshpolicy")) {
                  var7 = 86;
               }
               break;
            case 1671277506:
               if (capability.equals("vsandiagnostics")) {
                  var7 = 78;
               }
               break;
            case 1752873764:
               if (capability.equals("fileserviceowe")) {
                  var7 = 52;
               }
               break;
            case 1752877295:
               if (capability.equals("fileservicesmb")) {
                  var7 = 62;
               }
               break;
            case 1767670471:
               if (capability.equals("ensuredurability")) {
                  var7 = 79;
               }
               break;
            case 1804489455:
               if (capability.equals("allflash")) {
                  var7 = 0;
               }
               break;
            case 1851019434:
               if (capability.equals("pmanintegration")) {
                  var7 = 55;
               }
               break;
            case 1930300114:
               if (capability.equals("resyncetaimprovement")) {
                  var7 = 38;
               }
               break;
            case 1934435561:
               if (capability.equals("supportinsight")) {
                  var7 = 43;
               }
               break;
            case 2101078474:
               if (capability.equals("diagnosticmode")) {
                  var7 = 32;
               }
            }

            switch(var7) {
            case 0:
               result.isAllFlashSupported = true;
               break;
            case 1:
               result.isCapabilitiesSupported = true;
               break;
            case 2:
               result.isStretchedClusterSupported = true;
               break;
            case 3:
               result.isClusterConfigSupported = true;
               break;
            case 4:
               result.isDeduplicationAndCompressionSupported = true;
               break;
            case 5:
               result.isObjectIdentitiesSupported = true;
               break;
            case 6:
               result.isUpgradeSupported = true;
               break;
            case 7:
               result.isIscsiTargetsSupported = true;
               break;
            case 8:
               result.isWitnessManagementSupported = true;
               break;
            case 9:
               result.isPerfVerboseModeSupported = true;
               break;
            case 10:
               result.isPerfSvcAutoConfigSupported = true;
               break;
            case 11:
               result.isConfigAssistSupported = true;
               break;
            case 12:
               result.isUpdatesMgmtSupported = true;
               break;
            case 13:
               result.isWhatIfComplianceSupported = true;
               break;
            case 14:
               result.isPerfAnalysisSupported = true;
               break;
            case 15:
               result.isResyncThrottlingSupported = true;
               break;
            case 16:
               result.isEncryptionSupported = true;
               break;
            case 17:
               result.isWhatIfSupported = true;
               break;
            case 18:
               result.isCloudHealthSupported = true;
               break;
            case 19:
               result.isResyncEnhancedApiSupported = true;
               break;
            case 20:
               result.isFileServiceSupported = true;
               result.isRealTimePhysicalDiskHealthSupported = true;
               result.isAdaptiveResyncOnlySupported = true;
               break;
            case 21:
               result.isCnsVolumesSupported = true;
               break;
            case 22:
               result.isNetworkPerfTestSupported = true;
               break;
            case 23:
               result.isVsanVumIntegrationSupported = true;
               break;
            case 24:
               result.isWhatIfCapacitySupported = true;
               break;
            case 25:
               result.isHistoricalCapacitySupported = true;
               break;
            case 26:
               result.isNestedFdsSupported = true;
               break;
            case 27:
               result.isRepairTimerInResyncStatsSupported = true;
               break;
            case 28:
               result.isAdvancedClusterOptionsSupported = true;
               break;
            case 29:
               result.isPurgeInaccessibleVmSwapObjectsSupported = true;
               break;
            case 30:
               result.isRecreateDiskGroupSupported = true;
               break;
            case 31:
               result.isUpdateVumReleaseCatalogOfflineSupported = true;
               break;
            case 32:
               result.isPerfDiagnosticModeSupported = true;
               break;
            case 33:
               result.isPerfDiagnosticsFeedbackSupported = true;
               break;
            case 34:
               result.isAdvancedPerformanceSupported = true;
               break;
            case 35:
               result.isGetHclLastUpdateOnVcSupported = true;
               break;
            case 36:
               result.isAutomaticRebalanceSupported = true;
               break;
            case 37:
               result.isRdmaSupported = true;
               break;
            case 38:
               result.isResyncETAImprovementSupported = true;
            case 39:
            default:
               break;
            case 40:
               result.isIscsiOnlineResizeSupported = true;
               break;
            case 41:
               result.isIscsiStretchedClusterSupported = true;
               break;
            case 42:
               result.isVumBaselineRecommendationSupported = true;
               break;
            case 43:
               result.isSupportInsightSupported = true;
               break;
            case 44:
               result.isHostResourcePrecheckSupported = true;
               break;
            case 45:
               result.isDiskResourcePrecheckSupported = true;
               break;
            case 46:
               result.isVerboseModeInClusterConfigurationSupported = true;
               break;
            case 47:
               result.isVmLevelCapacityMonitoringSupported = true;
               break;
            case 48:
               result.isSlackSpaceCapacitySupported = true;
               break;
            case 49:
               result.isHostReservedCapacitySupported = true;
               break;
            case 50:
               result.isVsanCpuMetricsSupported = true;
               break;
            case 51:
               result.isFileServiceSnapshotSupported = true;
               break;
            case 52:
               result.isFileServiceOweSupported = true;
               result.isSmbPerformanceSupported = true;
               break;
            case 53:
               result.isFileServiceKerberosSupported = true;
               result.isFileSharePaginationSupported = true;
               result.isIpRemovalInEditModeSupported = true;
               break;
            case 54:
               result.isSharedWitnessSupported = true;
               break;
            case 55:
               result.isPmanIntegrationSupported = true;
               break;
            case 56:
               result.isPersistenceServiceSupported = true;
               break;
            case 57:
               result.isFileVolumesSupported = true;
               break;
            case 58:
               result.isNativeLargeClusterSupported = true;
               break;
            case 59:
               result.isIoInsightSupported = true;
               break;
            case 60:
               result.isHistoricalHealthSupported = true;
               break;
            case 61:
               result.isDataInTransitEncryptionSupported = true;
               break;
            case 62:
               result.isSmbProtocolSupported = true;
               break;
            case 63:
               result.isNfsv3ProtocolSupported = true;
               break;
            case 64:
               result.isCsdSupported = true;
               result.isDefaultGatewaySupported = true;
               break;
            case 65:
               result.isMultiVmPerfSupported = true;
               break;
            case 66:
               result.isHardwareManagementSupported = true;
               break;
            case 67:
               result.isCompressionOnlySupported = true;
               break;
            case 68:
               result.isManagedVmfsSupported = true;
               break;
            case 69:
               result.isSlackSpaceReservationSupported = true;
               break;
            case 70:
               result.isPersistenceServiceAirGapSupported = true;
               break;
            case 71:
               result.isPolicySatisfiabilitySupported = true;
               break;
            case 72:
               result.isManagedPMemSupported = true;
               break;
            case 73:
               result.isObjectsHealthV2Supported = true;
               break;
            case 74:
               result.isCapacityOversubscriptionSupported = true;
               break;
            case 75:
               result.isCapacityCustomizableThresholdsSupported = true;
               break;
            case 76:
               result.isFileServiceStretchedClusterSupported = true;
               break;
            case 77:
               result.isVmIoDiagnosticsSupported = true;
               break;
            case 78:
               result.isNetworkDiagnosticsSupported = true;
               break;
            case 79:
               result.isEnsureDurabilitySupported = true;
               break;
            case 80:
               result.isDiskMgmtRedesignSupported = true;
               break;
            case 81:
               result.isComputeOnlySupported = true;
               break;
            case 82:
               result.isDitSharedWitnessInteroperabilitySupported = true;
               break;
            case 83:
               result.isTopContributorsSupported = true;
               break;
            case 84:
               result.isDecomModeForVsanDirectDisksSupported = true;
               break;
            case 85:
               result.isFileAnalyticsSupported = true;
               break;
            case 86:
               result.isVsanHciMeshPolicySupported = true;
               break;
            case 87:
               result.isPersistenceResourceCheckSupported = true;
            }
         }
      }

      return result;
   }

   public String toString() {
      return "VsanCapabilityData(isDisconnected=" + this.isDisconnected + ", isCapabilitiesSupported=" + this.isCapabilitiesSupported + ", isAllFlashSupported=" + this.isAllFlashSupported + ", isStretchedClusterSupported=" + this.isStretchedClusterSupported + ", isClusterConfigSupported=" + this.isClusterConfigSupported + ", isDeduplicationAndCompressionSupported=" + this.isDeduplicationAndCompressionSupported + ", isUpgradeSupported=" + this.isUpgradeSupported + ", isObjectIdentitiesSupported=" + this.isObjectIdentitiesSupported + ", isObjectsHealthV2Supported=" + this.isObjectsHealthV2Supported + ", isIscsiTargetsSupported=" + this.isIscsiTargetsSupported + ", isWitnessManagementSupported=" + this.isWitnessManagementSupported + ", isPerfVerboseModeSupported=" + this.isPerfVerboseModeSupported + ", isPerfSvcAutoConfigSupported=" + this.isPerfSvcAutoConfigSupported + ", isConfigAssistSupported=" + this.isConfigAssistSupported + ", isUpdatesMgmtSupported=" + this.isUpdatesMgmtSupported + ", isWhatIfComplianceSupported=" + this.isWhatIfComplianceSupported + ", isPerfAnalysisSupported=" + this.isPerfAnalysisSupported + ", isResyncThrottlingSupported=" + this.isResyncThrottlingSupported + ", isEncryptionSupported=" + this.isEncryptionSupported + ", isWhatIfSupported=" + this.isWhatIfSupported + ", isCloudHealthSupported=" + this.isCloudHealthSupported + ", isResyncEnhancedApiSupported=" + this.isResyncEnhancedApiSupported + ", isFileServiceSupported=" + this.isFileServiceSupported + ", isCnsVolumesSupported=" + this.isCnsVolumesSupported + ", isNetworkPerfTestSupported=" + this.isNetworkPerfTestSupported + ", isVsanVumIntegrationSupported=" + this.isVsanVumIntegrationSupported + ", isWhatIfCapacitySupported=" + this.isWhatIfCapacitySupported + ", isHistoricalCapacitySupported=" + this.isHistoricalCapacitySupported + ", isNestedFdsSupported=" + this.isNestedFdsSupported + ", isRepairTimerInResyncStatsSupported=" + this.isRepairTimerInResyncStatsSupported + ", isPurgeInaccessibleVmSwapObjectsSupported=" + this.isPurgeInaccessibleVmSwapObjectsSupported + ", isRecreateDiskGroupSupported=" + this.isRecreateDiskGroupSupported + ", isUpdateVumReleaseCatalogOfflineSupported=" + this.isUpdateVumReleaseCatalogOfflineSupported + ", isAdvancedClusterOptionsSupported=" + this.isAdvancedClusterOptionsSupported + ", isPerfDiagnosticModeSupported=" + this.isPerfDiagnosticModeSupported + ", isPerfDiagnosticsFeedbackSupported=" + this.isPerfDiagnosticsFeedbackSupported + ", isAdvancedPerformanceSupported=" + this.isAdvancedPerformanceSupported + ", isGetHclLastUpdateOnVcSupported=" + this.isGetHclLastUpdateOnVcSupported + ", isAutomaticRebalanceSupported=" + this.isAutomaticRebalanceSupported + ", isRdmaSupported=" + this.isRdmaSupported + ", isResyncETAImprovementSupported=" + this.isResyncETAImprovementSupported + ", isGuestTrimUnmapSupported=" + this.isGuestTrimUnmapSupported + ", isIscsiOnlineResizeSupported=" + this.isIscsiOnlineResizeSupported + ", isIscsiStretchedClusterSupported=" + this.isIscsiStretchedClusterSupported + ", isVumBaselineRecommendationSupported=" + this.isVumBaselineRecommendationSupported + ", isSupportInsightSupported=" + this.isSupportInsightSupported + ", isHostResourcePrecheckSupported=" + this.isHostResourcePrecheckSupported + ", isDiskResourcePrecheckSupported=" + this.isDiskResourcePrecheckSupported + ", isVerboseModeInClusterConfigurationSupported=" + this.isVerboseModeInClusterConfigurationSupported + ", isVmLevelCapacityMonitoringSupported=" + this.isVmLevelCapacityMonitoringSupported + ", isSlackSpaceCapacitySupported=" + this.isSlackSpaceCapacitySupported + ", isHostReservedCapacitySupported=" + this.isHostReservedCapacitySupported + ", isVsanCpuMetricsSupported=" + this.isVsanCpuMetricsSupported + ", isFileServiceKerberosSupported=" + this.isFileServiceKerberosSupported + ", isUnmountWithMaintenanceModeSupported=" + this.isUnmountWithMaintenanceModeSupported + ", isSharedWitnessSupported=" + this.isSharedWitnessSupported + ", isPmanIntegrationSupported=" + this.isPmanIntegrationSupported + ", isPersistenceServiceSupported=" + this.isPersistenceServiceSupported + ", isFileVolumesSupported=" + this.isFileVolumesSupported + ", isNativeLargeClusterSupported=" + this.isNativeLargeClusterSupported + ", isIoInsightSupported=" + this.isIoInsightSupported + ", isHistoricalHealthSupported=" + this.isHistoricalHealthSupported + ", isDataInTransitEncryptionSupported=" + this.isDataInTransitEncryptionSupported + ", isSmbProtocolSupported=" + this.isSmbProtocolSupported + ", isSmbPerformanceSupported=" + this.isSmbPerformanceSupported + ", isNfsv3ProtocolSupported=" + this.isNfsv3ProtocolSupported + ", isCsdSupported=" + this.isCsdSupported + ", isMultiVmPerfSupported=" + this.isMultiVmPerfSupported + ", isHardwareManagementSupported=" + this.isHardwareManagementSupported + ", isCompressionOnlySupported=" + this.isCompressionOnlySupported + ", isRealTimePhysicalDiskHealthSupported=" + this.isRealTimePhysicalDiskHealthSupported + ", isDefaultGatewaySupported=" + this.isDefaultGatewaySupported + ", isManagedVmfsSupported=" + this.isManagedVmfsSupported + ", isSlackSpaceReservationSupported=" + this.isSlackSpaceReservationSupported + ", isIpRemovalInEditModeSupported=" + this.isIpRemovalInEditModeSupported + ", isFileSharePaginationSupported=" + this.isFileSharePaginationSupported + ", isPersistenceServiceAirGapSupported=" + this.isPersistenceServiceAirGapSupported + ", isPolicySatisfiabilitySupported=" + this.isPolicySatisfiabilitySupported + ", isManagedPMemSupported=" + this.isManagedPMemSupported + ", isCapacityOversubscriptionSupported=" + this.isCapacityOversubscriptionSupported + ", isCapacityCustomizableThresholdsSupported=" + this.isCapacityCustomizableThresholdsSupported + ", isFileServiceStretchedClusterSupported=" + this.isFileServiceStretchedClusterSupported + ", isFileServiceOweSupported=" + this.isFileServiceOweSupported + ", isFileServiceSnapshotSupported=" + this.isFileServiceSnapshotSupported + ", isVmIoDiagnosticsSupported=" + this.isVmIoDiagnosticsSupported + ", isNetworkDiagnosticsSupported=" + this.isNetworkDiagnosticsSupported + ", isEnsureDurabilitySupported=" + this.isEnsureDurabilitySupported + ", isDiskMgmtRedesignSupported=" + this.isDiskMgmtRedesignSupported + ", isComputeOnlySupported=" + this.isComputeOnlySupported + ", isDitSharedWitnessInteroperabilitySupported=" + this.isDitSharedWitnessInteroperabilitySupported + ", isTopContributorsSupported=" + this.isTopContributorsSupported + ", isDecomModeForVsanDirectDisksSupported=" + this.isDecomModeForVsanDirectDisksSupported + ", isFileAnalyticsSupported=" + this.isFileAnalyticsSupported + ", isVsanHciMeshPolicySupported=" + this.isVsanHciMeshPolicySupported + ", isPersistenceResourceCheckSupported=" + this.isPersistenceResourceCheckSupported + ", isAdaptiveResyncOnlySupported=" + this.isAdaptiveResyncOnlySupported + ")";
   }
}
