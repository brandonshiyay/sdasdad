package com.vmware.vsphere.client.vsan.stretched;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VSANWitnessHostInfo;

@TsModel
public class WitnessHostData {
   public ManagedObjectReference witnessHost;
   public boolean isOutOfInventory;
   public String witnessHostName;
   public String witnessHostIcon;
   public boolean isMetadataWitnessHost;
   public boolean inMaintenanceMode;
   public String preferredFaultDomainName;
   public String unicastAgentAddress;

   public WitnessHostData() {
   }

   public WitnessHostData(VSANWitnessHostInfo witnessInfo, String serverGuid) {
      this.witnessHost = witnessInfo.host;
      this.witnessHost.setServerGuid(serverGuid);
      this.preferredFaultDomainName = witnessInfo.preferredFdName;
      this.unicastAgentAddress = witnessInfo.unicastAgentAddr;
      this.isMetadataWitnessHost = witnessInfo.metadataMode != null && witnessInfo.metadataMode;
   }
}
