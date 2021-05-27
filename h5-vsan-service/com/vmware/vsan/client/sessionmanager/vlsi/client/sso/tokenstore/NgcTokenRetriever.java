package com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore;

import com.vmware.vim.sso.client.DefaultTokenFactory;
import com.vmware.vim.sso.client.SamlToken;
import com.vmware.vim.sso.client.exception.InvalidTokenException;
import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.ServiceEndpoint;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.SsoAdminConnection;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class NgcTokenRetriever extends AbstractTokenRetriever {
   private AsyncUserSessionService userSessionService;

   public NgcTokenRetriever(PrivateKey privateKey, X509Certificate cert, VlsiSettings lsSettings, ResourceFactory lsFactory, ResourceFactory adminFactory, AsyncUserSessionService userSessionService) {
      super(privateKey, cert, lsSettings, lsFactory, adminFactory);
      this.userSessionService = userSessionService;
   }

   public TokenInfo retrieveToken() {
      String samlTokenXml = this.userSessionService.getUserSession().samlTokenXml;
      SamlToken hokToken = null;

      try {
         LookupSvcConnection lsConnection = (LookupSvcConnection)this.lsFactory.acquire(this.lsSettings);
         Throwable var4 = null;

         TokenInfo var39;
         try {
            ServiceEndpoint ssoAdminEndpoint = lsConnection.getAdmin();
            VlsiSettings adminSettings = mkAdminSettings(ssoAdminEndpoint, this.lsSettings);
            SsoAdminConnection ssoAdmin = (SsoAdminConnection)this.adminFactory.acquire(adminSettings);
            Throwable var9 = null;

            X509Certificate[] stsCerts;
            try {
               stsCerts = ssoAdmin.getSigningCerts();
            } catch (Throwable var34) {
               var9 = var34;
               throw var34;
            } finally {
               if (ssoAdmin != null) {
                  if (var9 != null) {
                     try {
                        ssoAdmin.close();
                     } catch (Throwable var33) {
                        var9.addSuppressed(var33);
                     }
                  } else {
                     ssoAdmin.close();
                  }
               }

            }

            hokToken = DefaultTokenFactory.createToken(samlTokenXml, stsCerts);
            var39 = new TokenInfo(this.privateKey, hokToken);
         } catch (Throwable var36) {
            var4 = var36;
            throw var36;
         } finally {
            if (lsConnection != null) {
               if (var4 != null) {
                  try {
                     lsConnection.close();
                  } catch (Throwable var32) {
                     var4.addSuppressed(var32);
                  }
               } else {
                  lsConnection.close();
               }
            }

         }

         return var39;
      } catch (InvalidTokenException var38) {
         throw new IllegalStateException("Failed to deserialize token!", var38);
      }
   }

   public void shutdown() {
   }
}
