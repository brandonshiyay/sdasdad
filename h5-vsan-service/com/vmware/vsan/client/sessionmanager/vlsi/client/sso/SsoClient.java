package com.vmware.vsan.client.sessionmanager.vlsi.client.sso;

import com.vmware.vim.sso.client.TokenSpec;
import com.vmware.vim.sso.client.TokenSpec.Builder;
import com.vmware.vim.sso.client.TokenSpec.DelegationSpec;
import com.vmware.vise.security.ClientSessionEndListener;
import com.vmware.vise.usersession.ServerInfo;
import com.vmware.vise.usersession.UserSession;
import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import com.vmware.vsan.client.sessionmanager.common.SessionLocal;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.explorer.VcLsExplorer;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcInfo;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcLocator;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore.NgcRemoteTokenRetriever;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore.NgcTokenRetriever;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore.RenewingTokenRetriever;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore.TokenRetriever;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore.TokenStore;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SsoClient extends SessionLocal implements ClientSessionEndListener {
   private static final long DEFAULT_INITIAL_TOKEN_LIFETIME = 3600L;
   protected final Logger logger = LoggerFactory.getLogger(this.getClass());
   @Autowired
   protected LookupSvcClient localLsClient;
   @Autowired
   LookupSvcLocator lsLocator;
   @Autowired
   protected ScheduledExecutorService scheduler;
   @Autowired
   protected SsoAdminFactory ssoFactory;
   @Autowired
   protected AsyncUserSessionService sessionService;

   public TokenStore getTokenStore(String serverGuid) {
      return (TokenStore)this.get(serverGuid);
   }

   public TokenRetriever newRemoteTokenRetriever(LookupSvcInfo lsInfo, String username, String password, TokenSpec spec) throws Exception {
      return new RenewingTokenRetriever(this.lsLocator.getPrivateKey(), (X509Certificate)this.lsLocator.getH5Keystore().getCertificate("vsphere-webclient"), this.localLsClient.getSettings(lsInfo), this.localLsClient.getProducerFactory(), this.ssoFactory, this.scheduler, username, password, spec);
   }

   public TokenRetriever newRemoteTokenRetriever(LookupSvcInfo lsInfo, String username, String password) throws Exception {
      TokenSpec tokenSpec = (new Builder(3600L)).renewable(true).delegationSpec(new DelegationSpec(true, (String)null)).createTokenSpec();
      return this.newRemoteTokenRetriever(lsInfo, username, password, tokenSpec);
   }

   public TokenRetriever newLocalTokenRetriever(String serverGuid) throws Exception {
      VlsiSettings vlsiSettings = this.localLsClient.getSettings(this.localLsClient.getLocalLsInfo());
      PrivateKey privateKey = this.lsLocator.getPrivateKey();
      X509Certificate cert = (X509Certificate)this.lsLocator.getH5Keystore().getCertificate("vsphere-webclient");
      ResourceFactory lsFactory = this.localLsClient.getProducerFactory();
      if (this.isLocalDomain(serverGuid)) {
         return new NgcTokenRetriever(privateKey, cert, vlsiSettings, lsFactory, this.ssoFactory, this.sessionService);
      } else {
         String remoteHostname = this.getRemoteHostname(serverGuid);
         return new NgcRemoteTokenRetriever(privateKey, cert, vlsiSettings, lsFactory, this.ssoFactory, this.sessionService, remoteHostname);
      }
   }

   private boolean isLocalDomain(String serverGuid) {
      LookupSvcConnection conn = this.localLsClient.getConnection();
      Throwable var3 = null;

      boolean var6;
      try {
         String localDomainId = conn.getDomainId(this.lsLocator.getNodeId());
         String serverDomainId = this.getServerDomainId(serverGuid, conn);
         var6 = localDomainId == null || serverDomainId == null || localDomainId.equalsIgnoreCase(serverDomainId);
      } catch (Throwable var15) {
         var3 = var15;
         throw var15;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var14) {
                  var3.addSuppressed(var14);
               }
            } else {
               conn.close();
            }
         }

      }

      return var6;
   }

   private String getServerDomainId(String serverGuid, LookupSvcConnection conn) {
      VcLsExplorer vcLsExplorer = new VcLsExplorer(conn.getServiceRegistration());
      Map serverGuidToDomainIdMap = vcLsExplorer.getServerGuidToDomainIdMap();
      return (String)serverGuidToDomainIdMap.get(serverGuid);
   }

   private String getRemoteHostname(String uuid) {
      UserSession userSession = this.sessionService.getUserSession();
      return (String)Stream.of(userSession.serversInfo).filter((info) -> {
         return info.serviceGuid.equals(uuid);
      }).findAny().map(this::extractHostname).orElseThrow(IllegalStateException::new);
   }

   private String extractHostname(ServerInfo serverInfo) {
      try {
         return (new URI(serverInfo.serviceUrl)).getHost();
      } catch (URISyntaxException var3) {
         throw new RuntimeException(var3);
      }
   }

   public void sessionEnded(String clientId) {
      if (this.logger.isTraceEnabled()) {
         this.logger.trace("Session ended: {}", this.sessionKey());
      }

      this.remove(clientId);
   }

   protected String sessionKey() {
      return this.sessionService.getUserSession().clientId;
   }

   protected TokenStore create(String serverGuid) {
      try {
         return new TokenStore(this.newLocalTokenRetriever(serverGuid), this.localLsClient);
      } catch (Exception var3) {
         throw new RuntimeException(var3);
      }
   }

   protected void destroy(TokenStore entity) {
      entity.shutdown();
   }

   @PreDestroy
   protected void clear() {
      super.clear();
   }

   public String toString() {
      return this.getClass().getSimpleName();
   }
}
