package com.vmware.vsan.client.sessionmanager.vlsi.client.vc;

import com.vmware.vim.vsan.binding.vsan.version.versions;
import com.vmware.vsan.client.sessionmanager.common.VersionService;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanVcExploratoryFactory extends VcExploratoryFactory {
   @Autowired
   private VersionService versionService;

   public VsanVcExploratoryFactory(ResourceFactory vcFactory) {
      super(vcFactory);
   }

   protected Class getVmodlVerion(URI vcAddress) {
      Class result;
      try {
         result = this.versionService.getVmodlVersion(vcAddress.toString(), "/vsanServiceVersions.xml");
      } catch (Exception var4) {
         result = versions.VSAN_VERSION_LTS;
      }

      return result;
   }
}
