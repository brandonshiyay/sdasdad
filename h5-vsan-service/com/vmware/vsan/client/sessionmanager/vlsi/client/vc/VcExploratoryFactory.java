package com.vmware.vsan.client.sessionmanager.vlsi.client.vc;

import com.vmware.vim.binding.lookup.ServiceRegistration;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vsan.client.sessionmanager.common.VersionService;
import com.vmware.vsan.client.sessionmanager.common.util.ClientCertificate;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiExploratorySettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.explorer.VcLsExplorer;
import com.vmware.vsan.client.sessionmanager.vlsi.client.explorer.VcRegistration;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;

public class VcExploratoryFactory implements ResourceFactory {
   private final ResourceFactory vcFactory;
   @Autowired
   private VersionService versionService;
   @Autowired
   LookupSvcClient lsClient;

   public VcExploratoryFactory(ResourceFactory vcFactory) {
      this.vcFactory = vcFactory;
   }

   public VcConnection acquire(VlsiExploratorySettings exploratorySettings) {
      LookupSvcConnection lsConnection = this.lsClient.getConnection();
      Throwable var4 = null;

      VlsiSettings vlsiSettings;
      try {
         ServiceRegistration svcReg = lsConnection.getServiceRegistration();
         VcRegistration vcReg = (VcRegistration)(new VcLsExplorer(svcReg)).get(exploratorySettings.getServiceUuid());
         ClientCertificate keyStore = new ClientCertificate(vcReg.getUuid().toString(), vcReg.getSslTrust(), "", "", vcReg.getUuid().toString());
         vlsiSettings = exploratorySettings.getSettings().setServiceInfo(vcReg.getServiceUrl(), this.getVmodlVerion(vcReg.getServiceUrl())).setSslContext(keyStore, (ThumbprintVerifier)null);
      } catch (Throwable var15) {
         var4 = var15;
         throw var15;
      } finally {
         if (lsConnection != null) {
            if (var4 != null) {
               try {
                  lsConnection.close();
               } catch (Throwable var14) {
                  var4.addSuppressed(var14);
               }
            } else {
               lsConnection.close();
            }
         }

      }

      return (VcConnection)this.vcFactory.acquire(vlsiSettings);
   }

   protected Class getVmodlVerion(URI vcAddress) {
      return this.versionService.getVimVmodlVersion(vcAddress.toString());
   }
}
