package com.vmware.vsan.client.services.capacity.userobjects;

import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSpaceSummary;
import com.vmware.vsan.client.services.capacity.CapacityObjectsAccessor;
import com.vmware.vsan.client.services.capacity.model.UserObjectsCapacityData;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import java.util.Collection;
import java.util.Iterator;

public class UserObjectsCapacityCalculator {
   private CapacityObjectsAccessor capacityObjectsAccessor;

   public UserObjectsCapacityCalculator(CapacityObjectsAccessor capacityObjectsAccessor) {
      this.capacityObjectsAccessor = capacityObjectsAccessor;
   }

   public UserObjectsCapacityCalculator(VsanObjectSpaceSummary[] objectSpaceSummaries) {
      this.capacityObjectsAccessor = new CapacityObjectsAccessor(objectSpaceSummaries);
   }

   public UserObjectsCapacityData calculate() {
      UserObjectsCapacityData result = new UserObjectsCapacityData();
      result.otherFcd = this.getObjectUsedCapacity(this.getAny(VsanObjectType.improvedVirtualDisk));
      result.blockContainerVolumes = this.getObjectUsedCapacity(this.getAny(VsanObjectType.detachedCnsVolBlock));
      result.nativeFileShares = this.getObjectUsedCapacity(this.getAny(VsanObjectType.fileShare));
      result.fileContainerVolumes = this.getObjectUsedCapacity(this.getAny(VsanObjectType.cnsVolFile));
      result.vrTargetConfigsUsage = this.getObjectUsedCapacity(this.getAny(VsanObjectType.hbrCfg));
      result.vrTargetDisksUsage = this.getObjectUsedCapacity(this.getAny(VsanObjectType.hbrDisk));
      result.iSCSI = this.getObjectUsedCapacity(this.getAny(VsanObjectType.iscsiTarget));
      result.iSCSI += this.getObjectUsedCapacity(this.getAny(VsanObjectType.iscsiLun));
      result.other = this.getObjectUsedCapacity(this.getAny(VsanObjectType.other));
      Collection extensionsSummary = this.getAll(VsanObjectType.extension);
      Iterator var3 = extensionsSummary.iterator();

      while(var3.hasNext()) {
         VsanObjectSpaceSummary extensionSummary = (VsanObjectSpaceSummary)var3.next();
         long extensionCapacity = this.getObjectUsedCapacity(extensionSummary);
         if (extensionCapacity != 0L) {
            result.extensions.put(extensionSummary.objTypeExtDesc, extensionCapacity);
         }
      }

      result.totalUserObjectsUsage = this.getTotalUserObjectsCapacityUsage(result);
      return result;
   }

   private long getObjectUsedCapacity(VsanObjectSpaceSummary objectSpaceSummary) {
      return this.capacityObjectsAccessor.getObjectUsedCapacity(objectSpaceSummary);
   }

   private VsanObjectSpaceSummary getAny(VsanObjectType type) {
      return this.capacityObjectsAccessor.getAny(type);
   }

   private Collection getAll(VsanObjectType type) {
      return this.capacityObjectsAccessor.getAll(type);
   }

   private long getTotalUserObjectsCapacityUsage(UserObjectsCapacityData userObjectsCapacityData) {
      long result = 0L;
      result += userObjectsCapacityData.blockContainerVolumes;
      result += userObjectsCapacityData.otherFcd;
      result += userObjectsCapacityData.fileContainerVolumes;
      result += userObjectsCapacityData.nativeFileShares;
      result += userObjectsCapacityData.iSCSI;
      result += userObjectsCapacityData.vrTargetConfigsUsage;
      result += userObjectsCapacityData.vrTargetDisksUsage;
      result += userObjectsCapacityData.other;

      Long extensionUsage;
      for(Iterator var4 = userObjectsCapacityData.extensions.values().iterator(); var4.hasNext(); result += extensionUsage) {
         extensionUsage = (Long)var4.next();
      }

      return result;
   }
}
