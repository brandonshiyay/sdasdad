package com.vmware.vsan.client.util.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import com.vmware.vim.binding.vim.host.VsanInternalSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.Constants;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ClusterUuidsDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(ClusterUuidsDataRetriever.class);
   private Map hostTasks;
   private final VcClient vcClient;
   private final VmodlHelper vmodlHelper;

   public ClusterUuidsDataRetriever(ManagedObjectReference clusterRef, Measure measure, VcClient vcClient, VmodlHelper vmodlHelper) {
      super(clusterRef, measure);
      this.vcClient = vcClient;
      this.vmodlHelper = vmodlHelper;
   }

   public void start() {
      ManagedObjectReference[] hosts;
      Throwable var3;
      try {
         Measure collectHosts = this.measure.start("hosts(" + this.clusterRef.getValue() + ")");
         var3 = null;

         try {
            hosts = (ManagedObjectReference[])QueryUtil.getProperty(this.clusterRef, "host", (Object)null);
         } catch (Throwable var32) {
            var3 = var32;
            throw var32;
         } finally {
            if (collectHosts != null) {
               if (var3 != null) {
                  try {
                     collectHosts.close();
                  } catch (Throwable var31) {
                     var3.addSuppressed(var31);
                  }
               } else {
                  collectHosts.close();
               }
            }

         }
      } catch (Exception var34) {
         logger.warn("Failed to obtain cluster hosts: " + this.clusterRef, var34);
         this.result = Collections.EMPTY_SET;
         return;
      }

      if (ArrayUtils.isEmpty(hosts)) {
         this.hostTasks = Collections.EMPTY_MAP;
      } else {
         this.hostTasks = new HashMap(hosts.length);
         VcConnection vcConnection = this.vcClient.getConnection(this.clusterRef.getServerGuid());
         var3 = null;

         try {
            ManagedObjectReference[] var4 = hosts;
            int var5 = hosts.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               ManagedObjectReference host = var4[var6];
               ManagedObjectReference internalSystemRef = this.vmodlHelper.getVsanInternalSystem(host);
               VsanInternalSystem internalSystem = (VsanInternalSystem)vcConnection.createStub(VsanInternalSystem.class, internalSystemRef);
               Future compositeUuidsFuture = this.measure.newFuture("vsanUuids(" + host.getValue() + ")");
               internalSystem.queryPhysicalVsanDisks(Constants.PHYSICAL_DISK_VIRTUAL_MAPPING_PROPERTIES, compositeUuidsFuture);
               this.hostTasks.put(host, compositeUuidsFuture);
            }
         } catch (Throwable var35) {
            var3 = var35;
            throw var35;
         } finally {
            if (vcConnection != null) {
               if (var3 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var30) {
                     var3.addSuppressed(var30);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      }

   }

   public Set prepareResult() {
      Set vsanUuids = new HashSet();
      Iterator var2 = this.hostTasks.keySet().iterator();

      while(var2.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();
         Future task = (Future)this.hostTasks.get(hostRef);
         JsonNode hostJsonData = null;

         try {
            String json = (String)task.get();
            hostJsonData = Utils.getJsonRootNode(json);
         } catch (Exception var7) {
            logger.error("vSAN UUIDs omitted for disconnected host: " + hostRef, var7);
         }

         if (hostJsonData != null) {
            List hostVsanUuids = hostJsonData.findValuesAsText("compositeUuid");
            vsanUuids.addAll(hostVsanUuids);
         }
      }

      return vsanUuids;
   }
}
