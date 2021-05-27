package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.host.VsanSystem;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class DisksDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(DisksDataRetriever.class);
   private Map hostTasks = new HashMap();
   private final VcClient vcClient;
   private final List hosts;
   private final boolean processPartialResults;

   public DisksDataRetriever(ManagedObjectReference clusterRef, Measure measure, VcClient vcClient, List hosts, boolean processPartialResults) {
      super(clusterRef, measure);
      this.hosts = hosts;
      this.vcClient = vcClient;
      this.processPartialResults = processPartialResults;
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
               Future future = this.measure.newFuture("DiskResult[" + hostRef + "]");
               vsanSystem.queryDisksForVsan((String[])null, future);
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

   public Map prepareResult() throws ExecutionException, InterruptedException {
      Map hostToDisksMap = new HashMap();
      Iterator var2 = this.hostTasks.keySet().iterator();

      while(var2.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();
         DiskResult[] diskData = null;

         try {
            diskData = (DiskResult[])((Future)this.hostTasks.get(hostRef)).get();
         } catch (Exception var6) {
            logger.error("Failed to list claimed disks for host: " + hostRef, var6);
            if (!this.processPartialResults) {
               throw var6;
            }
         }

         if (diskData != null) {
            hostToDisksMap.put(hostRef, diskData);
         }
      }

      return hostToDisksMap;
   }
}
