package com.vmware.vsan.client.util;

import com.vmware.vim.binding.vim.vsan.host.ClusterStatus;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.MapUtils;

public class HostClusterStatusUtils {
   public static Map aggregateClusterStatus(Map hostToClusterStatuses) {
      Map hostToAggregatedClusterStatus = new HashMap();
      if (MapUtils.isEmpty(hostToClusterStatuses)) {
         return hostToAggregatedClusterStatus;
      } else {
         String currentClusterUuid = (String)hostToClusterStatuses.values().stream().filter((clustersStatusesx) -> {
            return clustersStatusesx.size() == 1;
         }).findFirst().map((clustersStatusesx) -> {
            return ((ClusterStatus)clustersStatusesx.get(0)).uuid;
         }).get();
         Iterator var3 = hostToClusterStatuses.entrySet().iterator();

         while(var3.hasNext()) {
            Entry entry = (Entry)var3.next();
            List clustersStatuses = (List)entry.getValue();
            ManagedObjectReference hostRef = (ManagedObjectReference)entry.getKey();
            if (clustersStatuses.size() > 1) {
               ClusterStatus clusterStatus = (ClusterStatus)clustersStatuses.stream().filter((clsStatus) -> {
                  return clsStatus.uuid.equals(currentClusterUuid);
               }).findFirst().orElse(clustersStatuses.get(0));
               hostToAggregatedClusterStatus.put(hostRef, clusterStatus);
            } else {
               hostToAggregatedClusterStatus.put(hostRef, clustersStatuses.get(0));
            }
         }

         return hostToAggregatedClusterStatus;
      }
   }
}
