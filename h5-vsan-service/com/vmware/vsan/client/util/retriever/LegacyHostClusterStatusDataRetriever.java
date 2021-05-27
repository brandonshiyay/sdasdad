package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.host.VsanSystem;
import com.vmware.vim.binding.vim.vsan.host.ClusterStatus;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class LegacyHostClusterStatusDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(LegacyHostClusterStatusDataRetriever.class);
   private Map hostTasks = new HashMap();
   private final VcClient vcClient;
   private final List hosts;

   public LegacyHostClusterStatusDataRetriever(ManagedObjectReference clusterRef, Measure measure, VcClient vcClient, List hosts) {
      super(clusterRef, measure);
      this.hosts = hosts;
      this.vcClient = vcClient;
   }

   public void start() {
      Iterator var1 = this.hosts.iterator();

      while(var1.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var1.next();

         try {
            VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
            Throwable var4 = null;

            try {
               VsanSystem vsanSystem = conn.getHostVsanSystem(hostRef);
               Future future = this.measure.newFuture("ClusterStatus[" + hostRef + "]");
               vsanSystem.queryHostStatus(future);
               this.hostTasks.put(hostRef, future);
            } catch (Throwable var15) {
               var4 = var15;
               throw var15;
            } finally {
               if (conn != null) {
                  if (var4 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var14) {
                        var4.addSuppressed(var14);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         } catch (Exception var17) {
            logger.warn("Unable to extract disks data for host (probably witness): " + hostRef);
         }
      }

   }

   public Map prepareResult() {
      Map hostToClusterStatus = new HashMap();
      Iterator var2 = this.hostTasks.keySet().iterator();

      while(var2.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();

         try {
            final ClusterStatus clusterStatus = (ClusterStatus)((Future)this.hostTasks.get(hostRef)).get();
            hostToClusterStatus.put(hostRef, new ArrayList() {
               {
                  this.add(clusterStatus);
               }
            });
         } catch (Exception var5) {
            logger.error("Failed to get cluster status of host: " + hostRef, var5);
         }
      }

      return hostToClusterStatus;
   }
}
