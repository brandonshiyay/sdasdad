package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.vsan.host.ClusterStatus;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.host.VsanSystemEx;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class HostClusterStatusesDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(HostClusterStatusesDataRetriever.class);
   private Map hostTasks = new HashMap();
   private final List hosts;

   public HostClusterStatusesDataRetriever(Measure measure, VsanClient vsanClient, List hosts) {
      super((ManagedObjectReference)null, measure, vsanClient);
      this.hosts = hosts;
   }

   public void start() {
      Iterator var1 = this.hosts.iterator();

      while(var1.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var1.next();
         VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanSystemEx vsanSystem = conn.getVsanSystemEx(hostRef);
            Future hostFuture = this.measure.newFuture("vsanSystem.queryHostStatusEx [" + hostRef + "]");
            vsanSystem.queryHostStatusEx((String[])null, hostFuture);
            this.hostTasks.put(hostRef, hostFuture);
         } catch (Throwable var14) {
            var4 = var14;
            throw var14;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var13) {
                     var4.addSuppressed(var13);
                  }
               } else {
                  conn.close();
               }
            }

         }
      }

   }

   public Map prepareResult() {
      Map hostToClusterStatus = new HashMap();
      Iterator var2 = this.hostTasks.keySet().iterator();

      while(var2.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();

         try {
            ClusterStatus[] clustersStatus = (ClusterStatus[])((Future)this.hostTasks.get(hostRef)).get();
            hostToClusterStatus.put(hostRef, Arrays.stream(clustersStatus).collect(Collectors.toList()));
         } catch (Exception var5) {
            logger.error("Failed to get cluster status of host: " + hostRef, var5);
         }
      }

      return hostToClusterStatus;
   }
}
