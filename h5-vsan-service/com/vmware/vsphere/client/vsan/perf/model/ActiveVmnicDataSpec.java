package com.vmware.vsphere.client.vsan.perf.model;

import com.google.common.collect.Multimap;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ActiveVmnicDataSpec {
   public List switches;
   public Map uuidSwitchMap;
   public Multimap switchNetworkMap;
   public Map networkUplinksMap;

   public Set getUplinksBySwitchUuid(String uuid, String portgroupKey) {
      Set uplinks = new HashSet();
      ManagedObjectReference switchRef = (ManagedObjectReference)this.uuidSwitchMap.get(uuid);
      if (switchRef != null) {
         Iterator var5 = this.switchNetworkMap.get(switchRef).iterator();

         while(var5.hasNext()) {
            ManagedObjectReference networkRef = (ManagedObjectReference)var5.next();
            if (networkRef.getValue() != null && StringUtils.equals(portgroupKey, networkRef.getValue())) {
               String[] activeUplink = (String[])this.networkUplinksMap.get(networkRef);
               if (!ArrayUtils.isEmpty(activeUplink)) {
                  uplinks.addAll(Arrays.asList(activeUplink));
               }
            }
         }
      }

      return uplinks;
   }
}
