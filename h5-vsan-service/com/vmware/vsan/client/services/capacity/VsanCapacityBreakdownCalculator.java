package com.vmware.vsan.client.services.capacity;

import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSpaceSummary;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsage;
import com.vmware.vsan.client.services.capacity.model.AlertThreshold;
import com.vmware.vsan.client.services.capacity.model.CapacityData;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.capacity.model.SlackSpaceCapacityData;
import com.vmware.vsan.client.services.capacity.model.SystemUsageCapacityData;
import com.vmware.vsan.client.services.capacity.vm.VsanVmCapacityCalculator;
import com.vmware.vsan.client.services.config.CapacityReservationConfig;
import com.vmware.vsan.client.services.config.ReservationStatus;
import com.vmware.vsan.client.services.config.SpaceEfficiencyConfig;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;

public class VsanCapacityBreakdownCalculator extends CapacityBreakdownCalculator {
   private SpaceEfficiencyConfig spaceEfficiencyConfig;
   private CapacityReservationConfig capacityReservationConfig;
   private boolean simulateCapacityEnforce;
   private CapacityData capacityData;

   public VsanCapacityBreakdownCalculator(VsanSpaceUsage spaceUsage, SpaceEfficiencyConfig spaceEfficiencyConfig, CapacityReservationConfig capacityReservationConfig, CapacityData capacityData) {
      super(spaceUsage);
      this.simulateCapacityEnforce = false;
      this.capacityData = capacityData;
      this.spaceEfficiencyConfig = spaceEfficiencyConfig;
      this.capacityReservationConfig = capacityReservationConfig;
      this.vmCapacityCalculator = new VsanVmCapacityCalculator(this.capacityObjectsAccessor);
   }

   public VsanCapacityBreakdownCalculator(VsanSpaceUsage spaceUsage, SpaceEfficiencyConfig spaceEfficiencyConfig, CapacityReservationConfig capacityReservationConfig, CapacityData capacityData, boolean simulateCapacityEnforce) {
      this(spaceUsage, spaceEfficiencyConfig, capacityReservationConfig, capacityData);
      this.simulateCapacityEnforce = simulateCapacityEnforce;
   }

   public VsanCapacityBreakdownCalculator(VsanSpaceUsage spaceUsage) {
      super(spaceUsage);
      this.simulateCapacityEnforce = false;
      this.vmCapacityCalculator = new VsanVmCapacityCalculator(this.capacityObjectsAccessor);
   }

   public VsanCapacityBreakdownCalculator(VsanObjectSpaceSummary[] spaceUsageByObjectType) {
      super(spaceUsageByObjectType);
      this.simulateCapacityEnforce = false;
      this.vmCapacityCalculator = new VsanVmCapacityCalculator(this.capacityObjectsAccessor);
   }

   public SystemUsageCapacityData calculateSystemUsageCapacityData() {
      SystemUsageCapacityData result = super.calculateSystemUsageCapacityData();
      if (this.spaceEfficiencyConfig.isEnabled()) {
         result.spaceEfficiencyOverhead = this.getObjectUsedCapacity(this.getAny(VsanObjectType.dedupOverhead));
         result.totalSystemUsage += result.spaceEfficiencyOverhead;
      }

      return result;
   }

   public long getTransientCapacityUsage() {
      return this.getObjectUsedCapacity(this.getAny(VsanObjectType.physicalTransientSpace));
   }

   public SlackSpaceCapacityData getSlackSpaceCapacity() {
      SlackSpaceCapacityData slackSpace = new SlackSpaceCapacityData();
      long vSanOperationSpace = this.getVsanOperationalSpace();
      this.updateVsanOperationCapacity(slackSpace, vSanOperationSpace);
      this.updateHostRebuildCapacity(slackSpace, vSanOperationSpace);
      this.updateTransientCapacity(slackSpace);
      slackSpace.enforceReservationSupported = this.isEnforceCapacityReservationAvailable();
      return slackSpace;
   }

   private boolean isEnforceCapacityReservationAvailable() {
      return this.capacityReservationConfig != null && this.capacityReservationConfig.vsanOperationReservation.isSupported();
   }

   private void updateVsanOperationCapacity(SlackSpaceCapacityData slackSpace, long vSanOperationSpace) {
      if (vSanOperationSpace != 0L) {
         slackSpace.operationSpaceThreshold = this.spaceUsage.totalCapacityB - vSanOperationSpace;
         if (this.capacityReservationConfig != null && (this.capacityReservationConfig.vsanOperationReservation.isEnforced() || this.simulateCapacityEnforce)) {
            slackSpace.operationSpaceReservation = vSanOperationSpace;
            slackSpace.operationSpaceReservationAdjusted = this.getCorrectedReservedSpace(slackSpace.operationSpaceThreshold, vSanOperationSpace);
         }

      }
   }

   private void updateHostRebuildCapacity(SlackSpaceCapacityData slackSpace, long vSanOperationSpace) {
      long hostRebuildCapacity = this.getObjectUsedCapacity(this.getAny(VsanObjectType.hostRebuildCapacity));
      if (hostRebuildCapacity != 0L) {
         slackSpace.rebuildToleranceThreshold = this.spaceUsage.totalCapacityB - vSanOperationSpace - hostRebuildCapacity;
         if (this.capacityReservationConfig != null && (this.capacityReservationConfig.hostFailureReservation.isEnforced() || this.simulateCapacityEnforce)) {
            slackSpace.rebuildToleranceReservation = hostRebuildCapacity;
            slackSpace.rebuildToleranceReservationAdjusted = this.getCorrectedReservedSpace(slackSpace.rebuildToleranceThreshold, hostRebuildCapacity);
         }

      }
   }

   private long getCorrectedReservedSpace(long threshold, long reserved) {
      long used = this.spaceUsage.totalCapacityB - this.spaceUsage.freeCapacityB - this.getTransientCapacityUsage();
      return used > threshold ? Math.max(reserved - (used - threshold), 0L) : reserved;
   }

   private void updateTransientCapacity(SlackSpaceCapacityData slackSpace) {
      long transientCapacity = this.getTransientCapacityUsage();
      if (transientCapacity > 0L && slackSpace.operationSpaceReservation > 0L) {
         if (slackSpace.operationSpaceReservation > transientCapacity) {
            slackSpace.operationSpaceReservationAdjusted -= transientCapacity;
         } else {
            transientCapacity -= slackSpace.operationSpaceReservationAdjusted;
            slackSpace.operationSpaceReservationAdjusted = 0L;
            if (slackSpace.rebuildToleranceReservationAdjusted > 0L) {
               slackSpace.rebuildToleranceReservationAdjusted -= transientCapacity;
               if (slackSpace.rebuildToleranceReservationAdjusted < 0L) {
                  slackSpace.rebuildToleranceReservationAdjusted = 0L;
               }
            }
         }
      }

   }

   public long getVsanOperationalSpace() {
      return this.getObjectUsedCapacity(this.getAny(VsanObjectType.minSpaceRequiredForVsanOp));
   }

   public static long getAvailableSpace(CapacityData capacityData, CapacityReservationConfig capacityReservationConfig) {
      long availableSpace = capacityData.totalSpace;
      if (capacityData.slackSpaceCapacityData != null && capacityReservationConfig != null) {
         if (capacityReservationConfig.hostFailureReservation == ReservationStatus.ENFORCED) {
            availableSpace -= capacityData.slackSpaceCapacityData.rebuildToleranceReservation;
         }

         if (capacityReservationConfig.vsanOperationReservation == ReservationStatus.ENFORCED) {
            availableSpace -= capacityData.slackSpaceCapacityData.operationSpaceReservation;
         }
      }

      return availableSpace;
   }

   public AlertThreshold getCapacityThresholds() {
      long availableSpace = getAvailableSpace(this.capacityData, this.capacityReservationConfig);
      AlertThreshold threshold;
      if (this.spaceUsage.capacityHealthThreshold != null) {
         threshold = AlertThreshold.fromVmodlInBytes(this.spaceUsage.capacityHealthThreshold, availableSpace);
      } else {
         threshold = super.getCapacityThresholds();
         long hostRebuildCapacity = this.getObjectUsedCapacity(this.getAny(VsanObjectType.hostRebuildCapacity));
         long vSanOperationSpace = this.getObjectUsedCapacity(this.getAny(VsanObjectType.minSpaceRequiredForVsanOp));
         if (hostRebuildCapacity > 0L) {
            threshold.warningThreshold = (double)(this.spaceUsage.totalCapacityB - hostRebuildCapacity);
         }

         if (vSanOperationSpace > 0L) {
            threshold.errorThreshold = (double)(this.spaceUsage.totalCapacityB - vSanOperationSpace);
         }
      }

      threshold.datastoreType = DatastoreType.VSAN;
      return threshold;
   }
}
