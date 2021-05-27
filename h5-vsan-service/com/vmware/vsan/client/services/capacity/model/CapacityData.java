package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.Utils;

@TsModel
public class CapacityData {
   public long totalSpace;
   public long freeSpace;
   public long usedSpace;
   public long actuallyWrittenSpace;
   public long overReservedSpace;
   public long transientSpace;
   public AlertThreshold thresholds;
   public SlackSpaceCapacityData slackSpaceCapacityData;
   public SpaceEfficiencyCapacityData spaceEfficiencyCapacity;
   public VmCapacityData vmCapacity;
   public UserObjectsCapacityData userObjectsCapacity;
   public SystemUsageCapacityData systemUsageCapacity;
   public String[] reducedCapacityMessages;

   public String toString() {
      return Utils.toString(this);
   }
}
