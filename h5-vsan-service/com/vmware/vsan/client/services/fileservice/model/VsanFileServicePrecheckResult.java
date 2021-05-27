package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServicePreflightCheckResult;
import java.util.Calendar;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class VsanFileServicePrecheckResult {
   private static String AD_SUPPORTED_API_VERSION = "1.1.0";
   private static String AD_SUPPORTED_OVF_VERSION = "7.0.1";
   private static String EARLY_OVF_VERSION = "7.0.0";
   private static String AFFINITY_SITE_SUPPORTED_API_VERSION = "1.2.0";
   private static String AFFINITY_SITE_SUPPORTED_OVF_VERSION = "7.0.2";
   private static String LARGE_SCALE_SUPPORTED_API_VERSION = "1.2.0";
   public String hostVersion;
   public String mixedModeIssue;
   public String networkPartitionIssue;
   public String ovfInstalled;
   public String vsanDatastoreIssue;
   public String fsvmVersion;
   public String ovfMixedModeIssue;
   public String domainConfigIssue;
   public String dvsConfigIssue;
   public Calendar lastUpgrade;
   public String fileServiceVersion;
   public String vcVersion;
   public boolean activeDirectorySupported;
   public boolean affinitySiteSupported;
   public boolean paginationSupported;
   public boolean smbSupported;
   public boolean largeScaleClusterSupported;

   public static VsanFileServicePrecheckResult fromVmodl(VsanFileServicePreflightCheckResult vmodl) {
      if (vmodl == null) {
         return null;
      } else {
         VsanFileServicePrecheckResult result = new VsanFileServicePrecheckResult();
         result.hostVersion = vmodl.hostVersion;
         result.mixedModeIssue = vmodl.mixedModeIssue;
         result.networkPartitionIssue = vmodl.networkPartitionIssue;
         result.ovfInstalled = vmodl.ovfInstalled;
         result.vsanDatastoreIssue = vmodl.vsanDatastoreIssue;
         result.fsvmVersion = vmodl.fsvmVersion;
         result.ovfMixedModeIssue = vmodl.ovfMixedModeIssue;
         result.lastUpgrade = vmodl.lastUpgradeDate;
         result.domainConfigIssue = vmodl.domainConfigIssue;
         result.dvsConfigIssue = vmodl.dvsConfigIssue;
         result.fileServiceVersion = vmodl.fileServiceVersion;
         if (result.fileServiceVersion == null) {
            result.activeDirectorySupported = false;
            result.affinitySiteSupported = false;
            result.largeScaleClusterSupported = false;
         } else {
            String ovfVersion = result.fsvmVersion == null ? result.ovfInstalled : result.fsvmVersion;
            result.activeDirectorySupported = isActiveDirectorySupported(ovfVersion, result);
            result.affinitySiteSupported = isAffinitySiteSupported(ovfVersion, result);
            result.largeScaleClusterSupported = isLargeScaleClusterSupported(result);
         }

         result.paginationSupported = result.activeDirectorySupported;
         result.smbSupported = result.activeDirectorySupported;
         return result;
      }
   }

   private static boolean isActiveDirectorySupported(String ovfVersion, VsanFileServicePrecheckResult precheckResult) {
      if (AD_SUPPORTED_API_VERSION.compareTo(precheckResult.fileServiceVersion) <= 0) {
         if (ovfVersion == null) {
            if (precheckResult.ovfMixedModeIssue == null) {
               return true;
            } else {
               return !StringUtils.contains(precheckResult.ovfMixedModeIssue, EARLY_OVF_VERSION);
            }
         } else {
            return AD_SUPPORTED_OVF_VERSION.compareTo(ovfVersion) <= 0;
         }
      } else {
         return false;
      }
   }

   private static boolean isAffinitySiteSupported(String ovfVersion, VsanFileServicePrecheckResult precheckResult) {
      if (AFFINITY_SITE_SUPPORTED_API_VERSION.compareTo(precheckResult.fileServiceVersion) > 0) {
         return false;
      } else if (ovfVersion == null) {
         if (precheckResult.ovfMixedModeIssue == null) {
            return true;
         } else {
            return !StringUtils.contains(precheckResult.ovfMixedModeIssue, EARLY_OVF_VERSION) && !StringUtils.contains(precheckResult.ovfMixedModeIssue, AD_SUPPORTED_OVF_VERSION);
         }
      } else {
         return AFFINITY_SITE_SUPPORTED_OVF_VERSION.compareTo(ovfVersion) <= 0;
      }
   }

   private static boolean isLargeScaleClusterSupported(VsanFileServicePrecheckResult precheckResult) {
      return LARGE_SCALE_SUPPORTED_API_VERSION.compareTo(precheckResult.fileServiceVersion) <= 0;
   }
}
