package com.vmware.vsan.client.services.capacity;

import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsage;
import com.vmware.vim.vsan.binding.vim.vsan.DataEfficiencyCapacityState;
import com.vmware.vsan.client.services.capacity.model.CapacityData;
import com.vmware.vsan.client.services.capacity.model.SpaceEfficiencyCapacityData;
import com.vmware.vsan.client.services.config.SpaceEfficiencyConfig;
import com.vmware.vsan.client.util.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CapacityOverviewCalculator {
   private static final Log logger = LogFactory.getLog(CapacityOverviewCalculator.class);
   private VsanSpaceUsage spaceUsage;
   private SpaceEfficiencyConfig spaceEfficiencyConfig;
   private DataEfficiencyCapacityState spaceEfficiencyCapacity;

   public CapacityOverviewCalculator(VsanSpaceUsage spaceUsage, DataEfficiencyCapacityState spaceEfficiencyCapacity, SpaceEfficiencyConfig spaceEfficiencyConfig) {
      this.spaceUsage = spaceUsage;
      this.spaceEfficiencyCapacity = spaceEfficiencyCapacity;
      this.spaceEfficiencyConfig = spaceEfficiencyConfig;
   }

   public CapacityOverviewCalculator(VsanSpaceUsage spaceUsage) {
      this.spaceUsage = spaceUsage;
      this.spaceEfficiencyConfig = SpaceEfficiencyConfig.NONE;
   }

   public CapacityData calculate() {
      CapacityData capacity = new CapacityData();
      if (this.spaceUsage != null && this.spaceUsage.spaceOverview != null) {
         capacity.totalSpace = this.getTotalSpace();
         capacity.usedSpace = this.getUsedSpace();
         capacity.freeSpace = this.getFreeSpace();
         capacity.actuallyWrittenSpace = this.getActuallyWrittenSpace();
         capacity.overReservedSpace = this.getOverReservedSpace();
         capacity.spaceEfficiencyCapacity = this.getSpaceEfficiencyCapacity();
         return capacity;
      } else {
         return capacity;
      }
   }

   private long getTotalSpace() {
      return NumberUtils.toLong(this.spaceUsage.totalCapacityB);
   }

   private long getUsedSpace() {
      return NumberUtils.toLong(this.spaceUsage.totalCapacityB) - NumberUtils.toLong(this.spaceUsage.freeCapacityB);
   }

   private long getFreeSpace() {
      return NumberUtils.toLong(this.spaceUsage.freeCapacityB);
   }

   private long getActuallyWrittenSpace() {
      return this.spaceEfficiencyConfig.isEnabled() ? this.getUsedSpace() : NumberUtils.toLong(this.spaceUsage.totalCapacityB) - NumberUtils.toLong(this.spaceUsage.spaceOverview.overReservedB) - NumberUtils.toLong(this.spaceUsage.freeCapacityB);
   }

   private long getOverReservedSpace() {
      return this.spaceEfficiencyConfig.isEnabled() ? 0L : NumberUtils.toLong(this.spaceUsage.spaceOverview.overReservedB);
   }

   private SpaceEfficiencyCapacityData getSpaceEfficiencyCapacity() {
      if (this.spaceEfficiencyConfig.isEnabled() && this.spaceEfficiencyCapacity != null && this.spaceEfficiencyCapacity.physicalCapacityUsed != null && this.spaceEfficiencyCapacity.logicalCapacityUsed != null && this.spaceEfficiencyCapacity.physicalCapacityUsed != 0L) {
         if (this.spaceEfficiencyCapacity.logicalCapacityUsed <= this.spaceEfficiencyCapacity.physicalCapacityUsed) {
            logger.error("Invalid space efficiency data. Used before space efficiency: " + this.spaceEfficiencyCapacity.logicalCapacityUsed + " is less than used after space efficiency: " + this.spaceEfficiencyCapacity.physicalCapacityUsed);
         }

         long usedSpaceBefore = Math.max(this.spaceEfficiencyCapacity.logicalCapacityUsed, this.spaceEfficiencyCapacity.physicalCapacityUsed);
         long usedSpaceAfter = this.spaceEfficiencyCapacity.physicalCapacityUsed;
         SpaceEfficiencyCapacityData spaceEfficiencyCapacityData = new SpaceEfficiencyCapacityData();
         spaceEfficiencyCapacityData.usedSpaceBefore = usedSpaceBefore;
         spaceEfficiencyCapacityData.usedSpaceAfter = usedSpaceAfter;
         spaceEfficiencyCapacityData.savings = usedSpaceBefore - usedSpaceAfter;
         spaceEfficiencyCapacityData.ratio = (double)usedSpaceBefore / (double)usedSpaceAfter;
         spaceEfficiencyCapacityData.spaceEfficiencyConfig = this.spaceEfficiencyConfig;
         return spaceEfficiencyCapacityData;
      } else {
         return null;
      }
   }
}
