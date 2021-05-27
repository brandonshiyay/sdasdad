package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.Map;

public interface DatastoreClaimer {
   boolean canClaim(Map var1);

   void claim(ManagedObjectReference var1, Map var2, ClaimedDisksPerDatastore var3);
}
