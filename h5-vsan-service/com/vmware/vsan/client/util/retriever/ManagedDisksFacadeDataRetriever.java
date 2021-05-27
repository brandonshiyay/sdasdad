package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanDirectStorage;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedDisksInfo;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedPMemInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.util.Measure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ManagedDisksFacadeDataRetriever extends AbstractAsyncDataRetriever {
   private ManagedDisksDataRetriever managedDisksDataRetriever;
   private DiskMappingsDataRetriever diskMappingsDataRetriever;
   private final List hosts;
   private final List hostsInVCWithVsanDirect = new ArrayList();
   private final List hostsInVCWithoutVsanDirect = new ArrayList();
   private final boolean processPartialResults;

   public ManagedDisksFacadeDataRetriever(ManagedObjectReference clusterRef, Measure measure, VsanClient vsanClient, List hosts, boolean processPartialResults) {
      super(clusterRef, measure, vsanClient);
      this.hosts = hosts;
      this.processPartialResults = processPartialResults;
   }

   public void start() {
      this.hosts.forEach((host) -> {
         if (VsanCapabilityUtils.isManagedVmfsSupportedOnVC(host) && VsanCapabilityUtils.isManagedVmfsSupported(host)) {
            this.hostsInVCWithVsanDirect.add(host);
         } else {
            this.hostsInVCWithoutVsanDirect.add(host);
         }

      });
      if (!this.hostsInVCWithVsanDirect.isEmpty()) {
         this.managedDisksDataRetriever = new ManagedDisksDataRetriever(this.clusterRef, this.measure, this.vsanClient, this.hostsInVCWithVsanDirect, this.processPartialResults);
         this.managedDisksDataRetriever.start();
      }

      if (!this.hostsInVCWithoutVsanDirect.isEmpty()) {
         this.diskMappingsDataRetriever = new DiskMappingsDataRetriever(this.clusterRef, this.measure, this.vsanClient, this.hostsInVCWithoutVsanDirect, this.processPartialResults);
         this.diskMappingsDataRetriever.start();
      }

   }

   public Map prepareResult() throws InterruptedException, ExecutionException {
      Map managedDisks = new HashMap();
      if (!this.hostsInVCWithVsanDirect.isEmpty()) {
         managedDisks.putAll(this.managedDisksDataRetriever.prepareResult());
      }

      if (!this.hostsInVCWithoutVsanDirect.isEmpty()) {
         managedDisks.putAll((Map)this.diskMappingsDataRetriever.prepareResult().entrySet().stream().collect(Collectors.toMap(Entry::getKey, (vsanDiskMapInfos) -> {
            return new VsanManagedDisksInfo((VsanDirectStorage[])null, (DiskMapInfoEx[])vsanDiskMapInfos.getValue(), (VsanManagedPMemInfo)null);
         })));
      }

      return managedDisks;
   }
}
