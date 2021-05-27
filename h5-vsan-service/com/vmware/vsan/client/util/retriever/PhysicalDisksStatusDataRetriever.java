package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanPhysicalDiskHealth;
import com.vmware.vim.vsan.binding.vim.host.VsanPhysicalDiskHealthSummary;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementUtil;
import com.vmware.vsan.client.services.diskmanagement.DiskStatus;
import com.vmware.vsan.client.services.diskmanagement.DiskStatusUtil;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.CMMDSHealthFlags;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.health.VsanHealthStatus;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PhysicalDisksStatusDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(PhysicalDisksStatusDataRetriever.class);
   private final ManagedObjectReference[] hosts;
   private Future hostDisksStatusesFuture;

   public PhysicalDisksStatusDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient, List hosts) {
      super(clusterRef, measure, vsanClient);
      this.hosts = (ManagedObjectReference[])hosts.toArray(new ManagedObjectReference[0]);
   }

   public void start() {
      try {
         VsanConnection vsanConnection = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
         Throwable var2 = null;

         try {
            VsanVcClusterHealthSystem vsanVcClusterHealthSystem = vsanConnection.getVsanVcClusterHealthSystem();
            this.hostDisksStatusesFuture = this.measure.newFuture("VsanVcClusterHealthSystem.queryPhysicalDiskHealthSummary");
            vsanVcClusterHealthSystem.queryPhysicalDiskHealthSummary(this.clusterRef, this.hostDisksStatusesFuture);
         } catch (Throwable var12) {
            var2 = var12;
            throw var12;
         } finally {
            if (vsanConnection != null) {
               if (var2 != null) {
                  try {
                     vsanConnection.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  vsanConnection.close();
               }
            }

         }
      } catch (Exception var14) {
         logger.error("Unable to query health and version properties of disks");
         this.hostDisksStatusesFuture.setException(var14);
      }

   }

   public Map prepareResult() throws ExecutionException {
      DataServiceResponse hostNames = this.queryNamesOfRequestedHosts();

      VsanPhysicalDiskHealthSummary[] pulledHosts;
      try {
         pulledHosts = (VsanPhysicalDiskHealthSummary[])this.hostDisksStatusesFuture.get();
      } catch (Exception var4) {
         logger.error("Failed to query disk statuses for the requested hosts" + var4);
         pulledHosts = new VsanPhysicalDiskHealthSummary[0];
      }

      return this.extractDiskStatusesPerHost(pulledHosts, hostNames);
   }

   private DataServiceResponse queryNamesOfRequestedHosts() throws ExecutionException {
      try {
         DataServiceResponse hostNames = QueryUtil.getProperties(this.hosts, new String[]{"name"});
         return hostNames;
      } catch (Exception var3) {
         logger.error("Failed to query host names" + var3);
         throw new ExecutionException(var3.getMessage(), var3.getCause());
      }
   }

   private Map extractDiskStatusesPerHost(VsanPhysicalDiskHealthSummary[] pulledHosts, DataServiceResponse requestedHostNames) {
      Map hostToDiskStatuses = new HashMap();
      ManagedObjectReference[] var4 = this.hosts;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference host = var4[var6];
         if (requestedHostNames.hasProperty(host, "name")) {
            String hostName = (String)requestedHostNames.getProperty(host, "name");
            hostToDiskStatuses.put(host, this.extractHostDisksStatuses(pulledHosts, hostName));
         }
      }

      return hostToDiskStatuses;
   }

   private Map extractHostDisksStatuses(VsanPhysicalDiskHealthSummary[] hosts, String hostName) {
      if (hosts == null) {
         return new HashMap();
      } else {
         VsanPhysicalDiskHealthSummary host = (VsanPhysicalDiskHealthSummary)Stream.of(hosts).filter((h) -> {
            return hostName.equals(h.hostname);
         }).findFirst().orElse((Object)null);
         return (Map)(host != null && host.disks != null ? (Map)Stream.of(host.disks).collect(Collectors.toMap((d) -> {
            return d.uuid;
         }, this::fromVmodl)) : new HashMap());
      }
   }

   private DiskStatus fromVmodl(VsanPhysicalDiskHealth disk) {
      return DiskStatusUtil.getVsanDiskStatus(this.isHealthy(disk.operationalHealth), this.isHealthy(disk.metadataHealth), disk.isInCmmds(), disk.isInVsi(), this.parseToHealthFlag(disk.operationalHealthDescription), this.findFormatVersion(disk));
   }

   private Integer parseToHealthFlag(String operationalHealthDescription) {
      CMMDSHealthFlags healthFlag = CMMDSHealthFlags.fromString(operationalHealthDescription);
      return healthFlag != null ? healthFlag.value : null;
   }

   private boolean isHealthy(String healthStatus) {
      return healthStatus != null && healthStatus.toLowerCase().equals(VsanHealthStatus.green.toString());
   }

   private Integer findFormatVersion(VsanPhysicalDiskHealth disk) {
      Integer formatVersion = null;
      if (disk.scsiDisk != null && disk.scsiDisk.vsanDiskInfo != null) {
         formatVersion = disk.scsiDisk.vsanDiskInfo.formatVersion;
      } else if (disk.formatVersion != null) {
         formatVersion = disk.formatVersion;
      }

      return DiskManagementUtil.normalizeDiskFormatVersion(formatVersion);
   }
}
