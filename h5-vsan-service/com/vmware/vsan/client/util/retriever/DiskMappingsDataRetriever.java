package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcDiskManagementSystem;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class DiskMappingsDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(DiskMappingsDataRetriever.class);
   private Map hostTasks = new HashMap();
   private final List hosts;
   private final boolean processPartialResults;

   public DiskMappingsDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient, List hosts, boolean processPartialResults) {
      super(clusterRef, measure, vsanClient);
      this.hosts = hosts;
      this.processPartialResults = processPartialResults;
   }

   public void start() {
      Iterator var1 = this.hosts.iterator();

      while(var1.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var1.next();
         VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanVcDiskManagementSystem diskSystem = conn.getVsanDiskManagementSystem();
            Future future = this.measure.newFuture("DiskMapInfoEx[" + hostRef + "]");
            diskSystem.queryDiskMappings(hostRef, future);
            this.hostTasks.put(hostRef, future);
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

   public Map prepareResult() throws ExecutionException, InterruptedException {
      Map hostToDiskMappings = new HashMap();

      ManagedObjectReference hostRef;
      DiskMapInfoEx[] diskMappings;
      for(Iterator var2 = this.hostTasks.keySet().iterator(); var2.hasNext(); hostToDiskMappings.put(hostRef, diskMappings)) {
         hostRef = (ManagedObjectReference)var2.next();
         diskMappings = null;

         try {
            diskMappings = (DiskMapInfoEx[])((Future)this.hostTasks.get(hostRef)).get();
         } catch (Exception var6) {
            logger.warn("Cannot query host's disk mappings from VsanVcDiskManagementSystem: ", var6);
            if (!this.processPartialResults) {
               throw var6;
            }
         }
      }

      return hostToDiskMappings;
   }
}
