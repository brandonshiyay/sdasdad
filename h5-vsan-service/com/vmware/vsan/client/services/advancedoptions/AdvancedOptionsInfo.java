package com.vmware.vsan.client.services.advancedoptions;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.CapacityReservationInfo;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.ObjectScrubConfig;
import com.vmware.vim.vsan.binding.vim.vsan.ProactiveRebalanceInfo;
import com.vmware.vim.vsan.binding.vim.vsan.VsanExtendedConfig;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.CapacityReservationConfig;
import com.vmware.vsan.client.util.NumberUtils;
import org.apache.commons.lang3.BooleanUtils;

@TsModel
public class AdvancedOptionsInfo {
   public static final long DEFAULT_OBJECT_REPAIR_TIMER = 60L;
   public static final boolean DEFAULT_READ_SITE_LOCALITY = false;
   public static final boolean DEFAULT_ENABLE_CUSTOMIZED_SWAP_OBJECT = true;
   public static final boolean DEFAULT_GUEST_TRIM_UNMAP_ENABLED = false;
   public static final boolean DEFAULT_AUTOMATIC_REBALANCE_ENABLED = false;
   public static final int DEFAULT_REBALANCING_THRESHOLD = 30;
   public long objectRepairTimer;
   public boolean isSiteReadLocalityEnabled;
   public boolean isCustomizedSwapObjectEnabled;
   public boolean largeClusterSupportEnabled;
   public boolean isGuestTrimUnmapEnabled;
   public boolean isAutomaticRebalanceEnabled;
   public int rebalancingThreshold;
   public CapacityReservationConfig capacityReservationConfig;

   public static AdvancedOptionsInfo fromVmodl(ConfigInfoEx configInfoEx, ManagedObjectReference clusterRef) {
      AdvancedOptionsInfo result = new AdvancedOptionsInfo();
      if (configInfoEx != null && configInfoEx.extendedConfig != null) {
         VsanExtendedConfig extendedConfig = configInfoEx.extendedConfig;
         result.objectRepairTimer = NumberUtils.toLong(extendedConfig.objectRepairTimer);
         result.isSiteReadLocalityEnabled = !BooleanUtils.toBoolean(extendedConfig.disableSiteReadLocality);
         result.isCustomizedSwapObjectEnabled = BooleanUtils.toBoolean(extendedConfig.enableCustomizedSwapObject);
         result.largeClusterSupportEnabled = BooleanUtils.toBoolean(extendedConfig.largeScaleClusterSupport);
         if (extendedConfig.proactiveRebalanceInfo != null && VsanCapabilityUtils.isAutomaticRebalanceSupported(clusterRef)) {
            result.isAutomaticRebalanceEnabled = BooleanUtils.toBoolean(extendedConfig.proactiveRebalanceInfo.enabled);
            result.rebalancingThreshold = NumberUtils.toInt(extendedConfig.proactiveRebalanceInfo.threshold, 30);
         }

         if (VsanCapabilityUtils.isSlackSpaceReservationSupported(clusterRef)) {
            result.capacityReservationConfig = CapacityReservationConfig.fromVmodl(extendedConfig.capacityReservationInfo);
         }

         if (configInfoEx.unmapConfig != null) {
            result.isGuestTrimUnmapEnabled = configInfoEx.unmapConfig.enable;
         }

         return result;
      } else {
         return result;
      }
   }

   public static VsanExtendedConfig toVmodl(AdvancedOptionsInfo advancedOptionsInfo, ManagedObjectReference clusterRef) {
      return new VsanExtendedConfig(advancedOptionsInfo.objectRepairTimer, !advancedOptionsInfo.isSiteReadLocalityEnabled, advancedOptionsInfo.isCustomizedSwapObjectEnabled, advancedOptionsInfo.largeClusterSupportEnabled, getRebalanceConfig(clusterRef, advancedOptionsInfo), getCapacityReservationConfig(clusterRef, advancedOptionsInfo.capacityReservationConfig), (ObjectScrubConfig)null);
   }

   private static ProactiveRebalanceInfo getRebalanceConfig(ManagedObjectReference clusterRef, AdvancedOptionsInfo advancedOptionsInfo) {
      if (!VsanCapabilityUtils.isAutomaticRebalanceSupported(clusterRef)) {
         return null;
      } else {
         ProactiveRebalanceInfo rebalanceConfig = new ProactiveRebalanceInfo();
         rebalanceConfig.enabled = advancedOptionsInfo.isAutomaticRebalanceEnabled;
         if (rebalanceConfig.enabled) {
            rebalanceConfig.threshold = advancedOptionsInfo.rebalancingThreshold;
         }

         return rebalanceConfig;
      }
   }

   private static CapacityReservationInfo getCapacityReservationConfig(ManagedObjectReference clusterRef, CapacityReservationConfig capacityReservationConfig) {
      return VsanCapabilityUtils.isSlackSpaceReservationSupported(clusterRef) && capacityReservationConfig != null ? CapacityReservationConfig.toVmodl(capacityReservationConfig) : null;
   }
}
