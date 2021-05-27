package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping.VsanDiskGroupCreationType;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import java.util.Map;

public class PmemDatastoreClaimer implements DatastoreClaimer {
   public boolean canClaim(Map claimOptionToDisks) {
      return claimOptionToDisks.containsKey(ClaimOption.PMEM);
   }

   public void claim(ManagedObjectReference hostRef, Map claimOptionToDisks, ClaimedDisksPerDatastore claimedDisksPerDatastore) {
      VsanHostDiskMapping pmemStorageMapping = new VsanHostDiskMapping();
      pmemStorageMapping.host = hostRef;
      pmemStorageMapping.cacheDisks = new ScsiDisk[0];
      pmemStorageMapping.capacityDisks = new ScsiDisk[0];
      pmemStorageMapping.type = VsanDiskGroupCreationType.pmem.toString();
      if (claimedDisksPerDatastore.pmemDatastoreDiskMappings.isEmpty()) {
         claimedDisksPerDatastore.pmemDatastoreDiskMappings.add(pmemStorageMapping);
      }

   }
}
