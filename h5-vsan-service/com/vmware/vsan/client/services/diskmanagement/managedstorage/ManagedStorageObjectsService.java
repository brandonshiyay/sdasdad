package com.vmware.vsan.client.services.diskmanagement.managedstorage;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.QueryVsanManagedHostObjectUuidsSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanManagedHostObjectUuids;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ManagedStorageObjectsService {
   @Autowired
   private VsanClient vsanClient;

   @Async
   public CompletableFuture getHostToStorageObjUuidMapping(ManagedObjectReference clusterRef, List hosts) {
      if (!CollectionUtils.isEmpty(hosts) && VsanCapabilityUtils.isManagedPMemSupportedOnVC(clusterRef)) {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         Map hostToStorageObjUuidMapping;
         try {
            VsanObjectSystem objectSystem = conn.getVsanObjectSystem();
            Measure measure = new Measure("VsanObjectSystem.queryVsanManagedObjectUuids");
            Throwable var8 = null;

            try {
               hostToStorageObjUuidMapping = this.groupStorageObjUuidMappingByHost(clusterRef, objectSystem.queryVsanManagedObjectUuids(clusterRef, this.getSpec(hosts)));
            } catch (Throwable var31) {
               var8 = var31;
               throw var31;
            } finally {
               if (measure != null) {
                  if (var8 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var30) {
                        var8.addSuppressed(var30);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var33) {
            var5 = var33;
            throw var33;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var29) {
                     var5.addSuppressed(var29);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return CompletableFuture.completedFuture(hostToStorageObjUuidMapping);
      } else {
         return CompletableFuture.completedFuture(new HashMap());
      }
   }

   private QueryVsanManagedHostObjectUuidsSpec getSpec(List hosts) {
      QueryVsanManagedHostObjectUuidsSpec spec = new QueryVsanManagedHostObjectUuidsSpec();
      spec.setHosts((ManagedObjectReference[])hosts.toArray(new ManagedObjectReference[0]));
      return spec;
   }

   private Map groupStorageObjUuidMappingByHost(ManagedObjectReference clusterRef, VsanManagedHostObjectUuids[] hostToStorageObjUuidMapping) {
      return (Map)(ArrayUtils.isEmpty(hostToStorageObjUuidMapping) ? new HashMap() : (Map)Arrays.stream(hostToStorageObjUuidMapping).collect(Collectors.toMap((storageObjUuidMapping) -> {
         return this.getHost(clusterRef.getServerGuid(), storageObjUuidMapping);
      }, (storageObjUuidMapping) -> {
         return storageObjUuidMapping.storageObjUuidMapping;
      })));
   }

   private ManagedObjectReference getHost(String serverGuid, VsanManagedHostObjectUuids storageObjUuidMapping) {
      if (StringUtils.isEmpty(storageObjUuidMapping.host.getServerGuid())) {
         storageObjUuidMapping.host.setServerGuid(serverGuid);
      }

      return storageObjUuidMapping.host;
   }
}
