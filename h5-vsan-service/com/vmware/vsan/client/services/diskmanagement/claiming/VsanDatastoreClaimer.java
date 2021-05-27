package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping.VsanDiskGroupCreationType;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VsanDatastoreClaimer implements DatastoreClaimer {
   public boolean canClaim(Map claimOptionToDisks) {
      return claimOptionToDisks.containsKey(ClaimOption.ClaimForStorage);
   }

   public void claim(ManagedObjectReference hostRef, Map claimOptionToDisks, ClaimedDisksPerDatastore claimedDisksPerDatastore) {
      VsanHostDiskMapping vsanDiskMapping = new VsanHostDiskMapping();
      vsanDiskMapping.host = hostRef;
      if (claimOptionToDisks.containsKey(ClaimOption.ClaimForCache)) {
         vsanDiskMapping.cacheDisks = (ScsiDisk[])((List)claimOptionToDisks.get(ClaimOption.ClaimForCache)).stream().map((diskSpec) -> {
            return diskSpec.disk;
         }).toArray((x$0) -> {
            return new ScsiDisk[x$0];
         });
      } else {
         vsanDiskMapping.cacheDisks = new ScsiDisk[0];
      }

      vsanDiskMapping.capacityDisks = (ScsiDisk[])((List)claimOptionToDisks.get(ClaimOption.ClaimForStorage)).stream().map((diskSpec) -> {
         return diskSpec.disk;
      }).toArray((x$0) -> {
         return new ScsiDisk[x$0];
      });
      vsanDiskMapping.type = this.chooseDiskGroupType(((List)claimOptionToDisks.get(ClaimOption.ClaimForStorage)).stream().allMatch((diskSpec) -> {
         return diskSpec.markedAsFlash;
      }));
      claimedDisksPerDatastore.vsanDatastoreDiskMappings.add(vsanDiskMapping);
   }

   public VsanHostDiskMapping toVsanHostDiskMappingVmodl(ManagedObjectReference hostRef, DiskMapping diskMapping) {
      VsanHostDiskMapping vsanDiskMapping = new VsanHostDiskMapping();
      vsanDiskMapping.host = hostRef;
      vsanDiskMapping.cacheDisks = new ScsiDisk[]{diskMapping.ssd};
      vsanDiskMapping.capacityDisks = diskMapping.nonSsd;
      vsanDiskMapping.type = this.chooseDiskGroupType(Arrays.stream(diskMapping.nonSsd).allMatch((disk) -> {
         return disk.ssd;
      }));
      return vsanDiskMapping;
   }

   private String chooseDiskGroupType(boolean allCapacityDisksAreFlash) {
      return allCapacityDisksAreFlash ? VsanDiskGroupCreationType.allflash.toString() : VsanDiskGroupCreationType.hybrid.toString();
   }
}
