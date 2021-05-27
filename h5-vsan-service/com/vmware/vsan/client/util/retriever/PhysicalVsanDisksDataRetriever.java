package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.host.VsanInternalSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PhysicalVsanDisksDataRetriever extends AbstractAsyncDataRetriever {
   public static final String DISKS_VIRTUAL_MAPPING_QUERY = "DISKS_VIRTUAL_MAPPING_QUERY";
   private static final Log logger = LogFactory.getLog(PhysicalVsanDisksDataRetriever.class);
   private Map hostTasks = new HashMap();
   private final VcClient vcClient;
   private final List hosts;
   private final String[] propertiesToQuery;

   public PhysicalVsanDisksDataRetriever(ManagedObjectReference clusterRef, Measure measure, VcClient vcClient, List hosts, String[] propertiesToQuery) {
      super(clusterRef, measure);
      this.hosts = hosts;
      this.vcClient = vcClient;
      this.propertiesToQuery = propertiesToQuery;
   }

   public void start() {
      Iterator var1 = this.hosts.iterator();

      while(var1.hasNext()) {
         ManagedObjectReference host = (ManagedObjectReference)var1.next();

         try {
            VcConnection vcConnection = this.vcClient.getConnection(host.getServerGuid());
            Throwable var4 = null;

            try {
               VsanInternalSystem vsanInternalSystem = vcConnection.getVsanInternalSystem(host);
               Future physicalDisksFuture = this.measure.newFuture("VsanInternalSystem.queryPhysicalVsanDisks[" + host + "]");
               vsanInternalSystem.queryPhysicalVsanDisks(this.propertiesToQuery, physicalDisksFuture);
               this.hostTasks.put(host, physicalDisksFuture);
            } catch (Throwable var15) {
               var4 = var15;
               throw var15;
            } finally {
               if (vcConnection != null) {
                  if (var4 != null) {
                     try {
                        vcConnection.close();
                     } catch (Throwable var14) {
                        var4.addSuppressed(var14);
                     }
                  } else {
                     vcConnection.close();
                  }
               }

            }
         } catch (Exception var17) {
            logger.warn("Unable to query health and version properties of disks on host: " + host);
         }
      }

   }

   public Map prepareResult() {
      Map hostToDisksPropertiesMap = new HashMap();
      Iterator var2 = this.hostTasks.keySet().iterator();

      while(var2.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();
         Future task = (Future)this.hostTasks.get(hostRef);

         try {
            hostToDisksPropertiesMap.put(hostRef, task.get());
         } catch (Exception var6) {
            logger.error("Failed to query properties " + Arrays.toString(this.propertiesToQuery) + " of disks on host: " + hostRef, var6);
         }
      }

      return hostToDisksPropertiesMap;
   }
}
