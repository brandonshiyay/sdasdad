package com.vmware.vsphere.client.vsan.stretched;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.ProxygenSerializer;
import java.util.List;

@TsModel
public class VsanStretchedClusterConfig {
   public ManagedObjectReference witnessHost;
   public DiskMapping witnessHostDiskMapping;
   public String preferredSiteName;
   @ProxygenSerializer.ElementType(ManagedObjectReference.class)
   public List preferredSiteHosts;
   public String secondarySiteName;
   @ProxygenSerializer.ElementType(ManagedObjectReference.class)
   public List secondarySiteHosts;
   public boolean isFaultDomainConfigurationChanged;
}
