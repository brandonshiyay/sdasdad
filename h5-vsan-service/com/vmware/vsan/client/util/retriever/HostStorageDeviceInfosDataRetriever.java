package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.host.StorageSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class HostStorageDeviceInfosDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(HostStorageDeviceInfosDataRetriever.class);
   private Map hostTasks = new HashMap();
   private final VcClient vcClient;
   private final VmodlHelper vmodlHelper;
   private final List hosts;

   public HostStorageDeviceInfosDataRetriever(ManagedObjectReference clusterRef, Measure measure, VcClient vcClient, VmodlHelper vmodlHelper, List hosts) {
      super(clusterRef, measure);
      this.hosts = hosts;
      this.vcClient = vcClient;
      this.vmodlHelper = vmodlHelper;
   }

   public void start() {
      Iterator var1 = this.hosts.iterator();

      while(var1.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var1.next();
         VcConnection vcConnection = this.vcClient.getConnection(hostRef.getServerGuid());
         Throwable var4 = null;

         try {
            VmodlHelper var10002 = this.vmodlHelper;
            StorageSystem storageSystem = (StorageSystem)vcConnection.createStub(StorageSystem.class, VmodlHelper.getStorageSystem(hostRef));
            Future future = this.measure.newFuture("StorageDeviceInfo[" + hostRef + "]");
            storageSystem.getStorageDeviceInfo(future);
            this.hostTasks.put(hostRef, future);
         } catch (Throwable var14) {
            var4 = var14;
            throw var14;
         } finally {
            if (vcConnection != null) {
               if (var4 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var13) {
                     var4.addSuppressed(var13);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      }

   }

   public Map prepareResult() {
      Map hostToStorageDeviceInfosMap = new HashMap();
      Iterator var2 = this.hostTasks.keySet().iterator();

      while(var2.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();

         try {
            hostToStorageDeviceInfosMap.put(hostRef, ((Future)this.hostTasks.get(hostRef)).get());
         } catch (Exception var5) {
            logger.error("Failed to get storage device infos of host: " + hostRef, var5);
         }
      }

      return hostToStorageDeviceInfosMap;
   }
}
