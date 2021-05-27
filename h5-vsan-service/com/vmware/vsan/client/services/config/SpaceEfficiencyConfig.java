package com.vmware.vsan.client.services.config;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.DataEfficiencyConfig;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import org.apache.commons.lang3.BooleanUtils;

@TsModel
public enum SpaceEfficiencyConfig {
   NONE,
   COMPRESSION,
   DEDUPLICATION_AND_COMPRESSION;

   public boolean isEnabled() {
      return this != NONE;
   }

   public static DataEfficiencyConfig toVmodl(SpaceEfficiencyConfig spaceEfficiency, ManagedObjectReference clusterRef) {
      DataEfficiencyConfig config = null;
      switch(spaceEfficiency) {
      case COMPRESSION:
         if (VsanCapabilityUtils.isCompressionOnlySupportedOnVc(clusterRef)) {
            config = new DataEfficiencyConfig(false, true);
         }
         break;
      case DEDUPLICATION_AND_COMPRESSION:
         if (VsanCapabilityUtils.isDeduplicationAndCompressionSupportedOnVc(clusterRef)) {
            config = new DataEfficiencyConfig(true, true);
         }
         break;
      case NONE:
         config = new DataEfficiencyConfig(false, false);
         break;
      default:
         throw new IllegalArgumentException("Invalid space efficiency strategy");
      }

      return config;
   }

   public static SpaceEfficiencyConfig fromVmodl(DataEfficiencyConfig config) {
      if (config == null) {
         return NONE;
      } else {
         boolean isDedupEnabled = config.dedupEnabled;
         boolean isCompressionEnabled = BooleanUtils.isTrue(config.compressionEnabled);
         if (!isDedupEnabled && !isCompressionEnabled) {
            return NONE;
         } else {
            return !isDedupEnabled ? COMPRESSION : DEDUPLICATION_AND_COMPRESSION;
         }
      }
   }
}
