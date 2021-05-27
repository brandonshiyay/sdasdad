package com.vmware.vsan.client.util.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.vim.binding.vim.host.VsanInternalSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementUtil;
import com.vmware.vsan.client.services.diskmanagement.DiskStatus;
import com.vmware.vsan.client.services.diskmanagement.DiskStatusUtil;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.util.Constants;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LegacyPhysicalDisksStatusDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(LegacyPhysicalDisksStatusDataRetriever.class);
   private Map hostDisksStatusesFuture = new HashMap();
   private final VcClient vcClient;
   private final List hosts;

   public LegacyPhysicalDisksStatusDataRetriever(ManagedObjectReference clusterRef, Measure measure, VcClient vcClient, List hosts) {
      super(clusterRef, measure);
      this.hosts = hosts;
      this.vcClient = vcClient;
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
               vsanInternalSystem.queryPhysicalVsanDisks(Constants.PHYSICAL_DISK_HEALTH_AND_VERSION_PROPERTIES, physicalDisksFuture);
               this.hostDisksStatusesFuture.put(host, physicalDisksFuture);
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
            logger.error("Unable to query health and version properties of disks on host: " + host);
         }
      }

   }

   public Map prepareResult() {
      Map hostToDisksStatusMap = new HashMap();
      Iterator var2 = this.hostDisksStatusesFuture.keySet().iterator();

      while(var2.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();
         Future disksStatusesFuture = (Future)this.hostDisksStatusesFuture.get(hostRef);

         try {
            String json = (String)disksStatusesFuture.get();
            if (StringUtils.isNotEmpty(json)) {
               hostToDisksStatusMap.put(hostRef, this.extractDisksStatuses(json));
            } else {
               hostToDisksStatusMap.put(hostRef, new HashMap());
            }
         } catch (Exception var6) {
            logger.error("Failed to query disks status on host: " + hostRef, var6);
         }
      }

      return hostToDisksStatusMap;
   }

   private Map extractDisksStatuses(String json) throws IOException {
      Map disksStatuses = new HashMap();
      Iterator iter = this.getIterator(json);

      while(iter.hasNext()) {
         Entry entry = (Entry)iter.next();
         disksStatuses.put(entry.getKey(), this.fromVmodl((JsonNode)entry.getValue()));
      }

      return disksStatuses;
   }

   private Iterator getIterator(String json) throws IOException {
      return (new ObjectMapper()).readTree(json).fields();
   }

   private DiskStatus fromVmodl(JsonNode node) {
      return DiskStatusUtil.getVsanDiskStatus(this.findHealthFlag(node), this.findFormatValue(node));
   }

   private Integer findHealthFlag(JsonNode node) {
      JsonNode health = node.get("disk_health");
      return health != null ? health.get("healthFlags").intValue() : null;
   }

   private Integer findFormatValue(JsonNode node) {
      Integer formatVersion = null;
      JsonNode publicFormatVersionNode = node.get("publicFormatVersion");
      JsonNode formatVersionNode = node.get("formatVersion");
      if (publicFormatVersionNode != null) {
         formatVersion = publicFormatVersionNode.intValue();
      } else if (formatVersionNode != null) {
         formatVersion = formatVersionNode.intValue();
      }

      return DiskManagementUtil.normalizeDiskFormatVersion(formatVersion);
   }
}
