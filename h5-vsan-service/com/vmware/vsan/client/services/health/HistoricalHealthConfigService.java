package com.vmware.vsan.client.services.health;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthConfigSpec;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.services.health.model.HistoricalHealthConfig;
import com.vmware.vsphere.client.vsan.impl.ConfigureVsanClusterMutationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HistoricalHealthConfigService {
   @Autowired
   private VsanConfigService vsanConfigService;
   @Autowired
   private ConfigureVsanClusterMutationProvider configureClusterService;

   @TsService
   public HistoricalHealthConfig getConfig(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = this.vsanConfigService.getConfigInfoEx(clusterRef);
      return this.vsanConfigService.getHistoricalHealthConfig(clusterRef, configInfoEx);
   }

   @TsService
   public ManagedObjectReference setConfig(ManagedObjectReference clusterRef, HistoricalHealthConfig config) {
      ReconfigSpec reconfigSpec = new ReconfigSpec();
      reconfigSpec.modify = true;
      reconfigSpec.setVsanHealthConfig(this.createHealthConfigSpec(config));
      ManagedObjectReference taskRef = this.configureClusterService.startReconfigureTask(clusterRef, reconfigSpec);
      return taskRef;
   }

   private VsanHealthConfigSpec createHealthConfigSpec(HistoricalHealthConfig historicalHealthConfig) {
      VsanHealthConfigSpec result = new VsanHealthConfigSpec();
      result.setHistoricalHealthConfig(historicalHealthConfig.toVmodl());
      return result;
   }
}
