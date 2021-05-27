package com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore;

import com.vmware.vim.binding.lookup.ServiceRegistration;
import com.vmware.vsan.client.sessionmanager.vlsi.client.explorer.VcLsExplorer;
import com.vmware.vsan.client.sessionmanager.vlsi.client.explorer.VcRegistration;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenStore {
   private static final Logger logger = LoggerFactory.getLogger(TokenStore.class);
   private final Map settings = new ConcurrentHashMap();
   private volatile boolean running = true;
   private final TokenRetriever ssoTokenRetriever;
   private final LookupSvcClient lsClient;

   public TokenStore(TokenRetriever ssoTokenRetriever, LookupSvcClient lsClient) {
      this.ssoTokenRetriever = ssoTokenRetriever;
      this.lsClient = lsClient;
      this.exploreAndAuthenticateLocalSites();
   }

   public TokenInfo retrieveTokenInfo(String siteId) {
      String serviceId = siteId.toLowerCase();
      this.exploreAndAuthenticateLocalSiteIfNotFound(serviceId);
      TokenRetriever retriever = (TokenRetriever)this.settings.get(serviceId);
      if (retriever == null) {
         throw new NoTokenException(serviceId);
      } else {
         return retriever.retrieveToken();
      }
   }

   public TokenInfo retrieveDelegatedTokenInfo(String siteId, String delegateTo) {
      String serviceId = siteId.toLowerCase();
      this.exploreAndAuthenticateLocalSiteIfNotFound(serviceId);
      TokenRetriever retriever = (TokenRetriever)this.settings.get(serviceId);
      if (retriever == null) {
         throw new NoTokenException(serviceId);
      } else {
         return retriever.retrieveDelegatedToken(delegateTo);
      }
   }

   public TokenRetriever getRetriever(String siteId) {
      String serviceId = siteId.toLowerCase();
      this.exploreAndAuthenticateLocalSiteIfNotFound(serviceId);
      return (TokenRetriever)this.settings.get(serviceId);
   }

   public void addSite(String siteId, TokenRetriever tokenRetriever) {
      String serviceId = siteId.toLowerCase();
      if (!this.running) {
         throw new IllegalStateException("Cannot add site " + serviceId + " because TokenStore is shutdown");
      } else {
         TokenRetriever oldRetriever = (TokenRetriever)this.settings.put(serviceId, tokenRetriever);
         logger.debug("Registered a token for site {}: {}", serviceId, tokenRetriever);
         if (oldRetriever != null) {
            logger.debug("Releasing overridden token for site {}: {}", serviceId, oldRetriever);
            oldRetriever.shutdown();
         }

      }
   }

   public boolean containsTokenFor(String siteId) {
      String serviceId = siteId.toLowerCase();
      this.exploreAndAuthenticateLocalSiteIfNotFound(serviceId);
      return this.settings.get(serviceId) != null;
   }

   public void shutdown() {
      logger.debug("TokenStore shutdown initiated.");
      this.running = false;
      this.clear();
   }

   public void clear() {
      logger.debug("Releasing all tokens.");
      int releasedTokens = 0;

      while(!this.settings.isEmpty()) {
         TokenRetriever oldRetriever = (TokenRetriever)this.settings.remove(BaseUtils.getMapNextKey(this.settings));
         if (oldRetriever != null) {
            ++releasedTokens;
            oldRetriever.shutdown();
         }
      }

      logger.debug("Released {} tokens.", releasedTokens);
   }

   private void exploreAndAuthenticateLocalSites() {
      LookupSvcConnection connection = this.lsClient.getConnection();
      Throwable var2 = null;

      try {
         ServiceRegistration ls = connection.getServiceRegistration();
         Iterator var4 = (new VcLsExplorer(ls)).list().iterator();

         while(var4.hasNext()) {
            VcRegistration vcReg = (VcRegistration)var4.next();
            this.addSite(vcReg.getUuid().toString().toLowerCase(), this.ssoTokenRetriever);
            logger.debug("Registered token for local VC: {}, retriever: {}", vcReg.getUuid().toString().toLowerCase(), this.ssoTokenRetriever);
         }
      } catch (Throwable var13) {
         var2 = var13;
         throw var13;
      } finally {
         if (connection != null) {
            if (var2 != null) {
               try {
                  connection.close();
               } catch (Throwable var12) {
                  var2.addSuppressed(var12);
               }
            } else {
               connection.close();
            }
         }

      }

   }

   private void exploreAndAuthenticateLocalSiteIfNotFound(String vcUuid) {
      UUID key = UUID.fromString(vcUuid);
      if (this.settings.get(key.toString()) == null) {
         logger.warn("No token for site: {}. Will explore the local SSO for VC registration with that UUID.", key);
         LookupSvcConnection lsConnection = this.lsClient.getConnection();
         Throwable var4 = null;

         try {
            ServiceRegistration serviceRegistry = lsConnection.getServiceRegistration();
            Map vcMap = (new VcLsExplorer(serviceRegistry)).map();
            if (vcMap.containsKey(key)) {
               VcRegistration vcRegistration = (VcRegistration)vcMap.get(key);
               this.addSite(vcRegistration.getUuid().toString(), this.ssoTokenRetriever);
               logger.debug("Exploration found new VC site in the local SSO: {}", key);
               return;
            }
         } catch (Throwable var17) {
            var4 = var17;
            throw var17;
         } finally {
            if (lsConnection != null) {
               if (var4 != null) {
                  try {
                     lsConnection.close();
                  } catch (Throwable var16) {
                     var4.addSuppressed(var16);
                  }
               } else {
                  lsConnection.close();
               }
            }

         }

         logger.warn("Exploration failed to locate local site with UUID: {}", key);
      }
   }
}
