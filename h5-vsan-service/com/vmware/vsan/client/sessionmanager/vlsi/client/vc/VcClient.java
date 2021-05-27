package com.vmware.vsan.client.sessionmanager.vlsi.client.vc;

import com.vmware.vise.usersession.UserSession;
import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.NotAccessibleException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.NotAuthenticatedException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiExploratorySettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.authenticator.TokenStoreVcAuth;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcInfo;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.SsoClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore.TokenStoreException;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class VcClient {
   @Autowired
   @Qualifier("vcFactory")
   private ResourceFactory vcFactory;
   @Autowired
   @Qualifier("vsanVcFactory")
   private ResourceFactory vsanVcFactory;
   @Autowired
   private VlsiSettings vlsiSettingsTemplate;
   @Autowired
   private SsoClient ssoClient;
   @Autowired
   private AsyncUserSessionService sessionService;

   public VcConnection getConnection(String uuid) {
      return this.getConnection(uuid, (LookupSvcInfo)null);
   }

   public VcConnection getConnection(String uuid, LookupSvcInfo lsInfo) {
      return this.getConnection(uuid, (LookupSvcInfo)null, this.vcFactory);
   }

   public VcConnection getVsanVmodlVersionConnection(String uuid) {
      return this.getConnection(uuid, (LookupSvcInfo)null, this.vsanVcFactory);
   }

   private VcConnection getConnection(String serverGuid, LookupSvcInfo lsInfo, ResourceFactory factory) {
      UserSession userSession = this.sessionService.getUserSession();
      String sessionLocale = userSession != null ? userSession.locale : null;
      VlsiExploratorySettings exploratorySettings = new VlsiExploratorySettings(this.vlsiSettingsTemplate.setAuthenticator(new TokenStoreVcAuth(sessionLocale, this.ssoClient.getTokenStore(serverGuid), serverGuid)), UUID.fromString(serverGuid));

      try {
         return (VcConnection)factory.acquire(exploratorySettings);
      } catch (TokenStoreException var8) {
         throw new NotAuthenticatedException(Utils.getLocalizedString("vsan.sessionmanager.siteNotAuthenticated"), var8);
      } catch (Exception var9) {
         throw new NotAccessibleException(Utils.getLocalizedString("vsan.sessionmanager.siteNotAccessible"), var9);
      }
   }
}
