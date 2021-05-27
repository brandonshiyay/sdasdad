package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

@TsModel
public class UserObjectsCapacityData {
   public long totalUserObjectsUsage;
   public long blockContainerVolumes;
   public long otherFcd;
   public long fileContainerVolumes;
   public long nativeFileShares;
   public long iSCSI;
   public long vrTargetConfigsUsage;
   public long vrTargetDisksUsage;
   public long other;
   public SortedMap extensions = new TreeMap();

   public String toString() {
      StringBuilder sb = new StringBuilder("totalUserObjectsUsage=" + this.totalUserObjectsUsage + ",\nblockContainerVolumes=" + this.blockContainerVolumes + ",\notherFcd=" + this.otherFcd + ",\nfileContainerVolumes=" + this.fileContainerVolumes + ",\nnativeFileShares=" + this.nativeFileShares + ",\niSCSI=" + this.iSCSI + ",\nvrTargetConfigsUsage=" + this.vrTargetConfigsUsage + ",\nvrTargetDisksUsage=" + this.vrTargetDisksUsage + ",\nother=" + this.other + ",\nextensions=[");
      Iterator iterator = this.extensions.entrySet().iterator();

      while(iterator.hasNext()) {
         Entry extention = (Entry)iterator.next();
         sb.append(String.format("   %s=%d", extention.getKey(), extention.getValue()));
         if (iterator.hasNext()) {
            sb.append(",\n");
         }
      }

      sb.append("]");
      return sb.toString();
   }
}
