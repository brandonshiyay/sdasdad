package com.vmware.vsan.client.services.capacity;

import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSpaceSummary;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsage;
import com.vmware.vsan.client.services.capacity.model.AlertThreshold;
import com.vmware.vsan.client.services.capacity.model.SystemUsageCapacityData;
import com.vmware.vsan.client.services.capacity.model.UserObjectsCapacityData;
import com.vmware.vsan.client.services.capacity.model.VmCapacityData;
import com.vmware.vsan.client.services.capacity.userobjects.UserObjectsCapacityCalculator;
import com.vmware.vsan.client.services.capacity.vm.VmCapacityCalculator;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;

public abstract class CapacityBreakdownCalculator {
   private final int DEFAULT_WARNING_THRESHOLD_PERCENTAGE;
   private final int DEFAULT_ERROR_THRESHOLD_PERCENTAGE;
   private UserObjectsCapacityCalculator userObjCapacityCalculator;
   protected VmCapacityCalculator vmCapacityCalculator;
   protected CapacityObjectsAccessor capacityObjectsAccessor;
   protected VsanSpaceUsage spaceUsage;

   public CapacityBreakdownCalculator(VsanSpaceUsage spaceUsage) {
      this(spaceUsage.spaceDetail.spaceUsageByObjectType);
      this.spaceUsage = spaceUsage;
   }

   public CapacityBreakdownCalculator(VsanObjectSpaceSummary[] spaceUsageByObjectType) {
      this.DEFAULT_WARNING_THRESHOLD_PERCENTAGE = 70;
      this.DEFAULT_ERROR_THRESHOLD_PERCENTAGE = 90;
      this.capacityObjectsAccessor = new CapacityObjectsAccessor(spaceUsageByObjectType);
      this.userObjCapacityCalculator = new UserObjectsCapacityCalculator(this.capacityObjectsAccessor);
      this.vmCapacityCalculator = new VmCapacityCalculator(this.capacityObjectsAccessor);
   }

   protected UserObjectsCapacityData calculateUserObjectsCapacityData() {
      return this.userObjCapacityCalculator.calculate();
   }

   protected SystemUsageCapacityData calculateSystemUsageCapacityData() {
      SystemUsageCapacityData result = new SystemUsageCapacityData();
      result.performanceMgmtObjects = this.getObjectUsedCapacity(this.getAny(VsanObjectType.statsdb));
      result.fileServiceOverhead = this.getObjectUsedCapacity(this.getAny(VsanObjectType.fileSystemOverhead));
      result.checksumOverhead = this.getObjectUsedCapacity(this.getAny(VsanObjectType.checksumOverhead));
      result.transientSpace = this.getObjectUsedCapacity(this.getAny(VsanObjectType.transientSpace));
      result.haMetadataObject = this.getObjectUsedCapacity(this.getAny(VsanObjectType.haMetadataObject));
      result.totalSystemUsage = this.getTotalSystemCapacityUsage(result);
      return result;
   }

   protected AlertThreshold getCapacityThresholds() {
      AlertThreshold result = new AlertThreshold();
      result.isDefault = true;
      result.isEnabled = true;
      this.getClass();
      result.errorThreshold = 90.0D;
      this.getClass();
      result.warningThreshold = 70.0D;
      return result;
   }

   protected VmCapacityData calculateVmCapacityData() {
      return this.vmCapacityCalculator.calculate();
   }

   protected long getObjectUsedCapacity(VsanObjectSpaceSummary objectSpaceSummary) {
      return this.capacityObjectsAccessor.getObjectUsedCapacity(objectSpaceSummary);
   }

   protected VsanObjectSpaceSummary getAny(VsanObjectType type) {
      return this.capacityObjectsAccessor.getAny(type);
   }

   private long getTotalSystemCapacityUsage(SystemUsageCapacityData systemUsageCapacityData) {
      long result = 0L;
      result += systemUsageCapacityData.performanceMgmtObjects;
      result += systemUsageCapacityData.fileServiceOverhead;
      result += systemUsageCapacityData.haMetadataObject;
      result += systemUsageCapacityData.checksumOverhead;
      result += systemUsageCapacityData.transientSpace;
      return result;
   }
}
