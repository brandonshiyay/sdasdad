package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping.VsanDiskGroupCreationType;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import java.util.List;
import java.util.Map;

public class VmfsDatastoreClaimer implements DatastoreClaimer {
   public boolean canClaim(Map claimOptionToDisks) {
      return claimOptionToDisks.containsKey(ClaimOption.VMFS);
   }

   public void claim(ManagedObjectReference hostRef, Map claimOptionToDisks, ClaimedDisksPerDatastore claimedDisksPerDatastore) {
      VsanHostDiskMapping vmfsDiskMapping = new VsanHostDiskMapping();
      vmfsDiskMapping.host = hostRef;
      vmfsDiskMapping.cacheDisks = new ScsiDisk[0];
      vmfsDiskMapping.capacityDisks = (ScsiDisk[])((List)claimOptionToDisks.get(ClaimOption.VMFS)).stream().map((diskSpec) -> {
         return diskSpec.disk;
      }).toArray((x$0) -> {
         return new ScsiDisk[x$0];
      });
      vmfsDiskMapping.type = VsanDiskGroupCreationType.vsandirect.toString();
      claimedDisksPerDatastore.vmfsDatastoreDiskMappings.add(vmfsDiskMapping);
   }
}
