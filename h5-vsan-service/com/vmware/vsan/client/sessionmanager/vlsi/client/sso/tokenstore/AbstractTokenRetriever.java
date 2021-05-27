package com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore;

import com.vmware.vim.binding.lookup.version.version4_0;
import com.vmware.vim.binding.sso.version.version1;
import com.vmware.vim.sso.client.SamlToken;
import com.vmware.vim.sso.client.TokenSpec;
import com.vmware.vim.sso.client.TokenSpec.Builder;
import com.vmware.vim.sso.client.TokenSpec.DelegationSpec;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vsan.client.sessionmanager.common.util.CertificateUtils;
import com.vmware.vsan.client.sessionmanager.common.util.ClientCertificate;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.authenticator.Authenticator;
import com.vmware.vsan.client.sessionmanager.vlsi.client.http.HttpSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.http.SingleThumbprintVerifier;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcInfo;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.HokStsService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.ServiceEndpoint;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.SsoAdminConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.StsService;
import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

public abstract class AbstractTokenRetriever implements TokenRetriever {
   protected final PrivateKey privateKey;
   protected final X509Certificate certificate;
   protected final VlsiSettings lsSettings;
   protected final ResourceFactory lsFactory;
   protected final ResourceFactory adminFactory;

   public AbstractTokenRetriever(PrivateKey privateKey, X509Certificate cert, VlsiSettings lsSettings, ResourceFactory lsFactory, ResourceFactory adminFactory) {
      this.privateKey = privateKey;
      this.certificate = cert;
      this.lsSettings = lsSettings;
      this.lsFactory = lsFactory;
      this.adminFactory = adminFactory;
   }

   public TokenInfo retrieveDelegatedToken(String delegateTo) {
      TokenInfo token = this.retrieveToken();

      try {
         LookupSvcConnection conn = (LookupSvcConnection)this.lsFactory.acquire(this.lsSettings);
         Throwable var4 = null;

         TokenInfo var8;
         try {
            StsService localSts = getSts(this.privateKey, this.certificate, conn, this.adminFactory, this.lsSettings);
            TokenSpec spec = (new Builder(600L)).renewable(token.getToken().isRenewable()).delegationSpec(new DelegationSpec(true, delegateTo)).createTokenSpec();
            SamlToken localDelegatedToken = localSts.getStsClient().acquireTokenByToken(token.getToken(), spec);
            var8 = new TokenInfo((PrivateKey)null, localDelegatedToken);
         } catch (Throwable var18) {
            var4 = var18;
            throw var18;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var17) {
                     var4.addSuppressed(var17);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var8;
      } catch (Exception var20) {
         throw new TokenAcquisitionException("Failed to delegate token to: " + delegateTo, var20);
      }
   }

   protected TokenInfo acquireTokenForRemoteDomain(TokenInfo hokTokenInfo, String host) {
      LookupSvcInfo lsInfo = this.createLookupSvcInfo(host);
      VlsiSettings remoteLsSettings = createRemoteLsSettings(this.lsSettings, lsInfo);

      try {
         LookupSvcConnection conn = (LookupSvcConnection)this.lsFactory.acquire(remoteLsSettings);
         Throwable var6 = null;

         TokenInfo var10;
         try {
            StsService remoteSts = getSts(this.privateKey, this.certificate, conn, this.adminFactory, remoteLsSettings);
            TokenSpec remoteSpec = (new Builder(600L)).renewable(hokTokenInfo.getToken().isRenewable()).delegationSpec(new DelegationSpec(hokTokenInfo.getToken(), true)).createTokenSpec();
            SamlToken remoteDelegateToken = remoteSts.getStsExClient().acquireTokenByExternalToken(hokTokenInfo.getToken(), remoteSpec);
            var10 = new TokenInfo(this.privateKey, remoteDelegateToken);
         } catch (Throwable var20) {
            var6 = var20;
            throw var20;
         } finally {
            if (conn != null) {
               if (var6 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var19) {
                     var6.addSuppressed(var19);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var10;
      } catch (Exception var22) {
         throw new TokenAcquisitionException("Failed to delegate token to: ", var22);
      }
   }

   private LookupSvcInfo createLookupSvcInfo(String hostname) {
      try {
         URI serviceUri = LookupSvcClient.createServiceUri(hostname, -1);
         String serverThumbprint = CertificateUtils.getServerThumbprint("https://" + hostname);
         return new LookupSvcInfo(serviceUri, serverThumbprint);
      } catch (Exception var4) {
         throw new IllegalArgumentException("Cannot create LookupSvcInfo", var4);
      }
   }

   private static VlsiSettings createRemoteLsSettings(VlsiSettings vlsiSettingsTemplate, LookupSvcInfo lsInfo) {
      if (lsInfo == null) {
         throw new IllegalArgumentException();
      } else {
         ThumbprintVerifier thumbprintVerifier = lsInfo.getThumbprintVerifier();
         ClientCertificate trustStore = null;
         if (lsInfo.getKeyStore() != null) {
            trustStore = new ClientCertificate(lsInfo.getAddress().getHost(), lsInfo.getKeyStore(), "", "", lsInfo.getAddress().getHost());
         }

         VlsiSettings settings = vlsiSettingsTemplate.setServiceInfo(lsInfo.getAddress(), version4_0.class).setSslContext(trustStore, thumbprintVerifier);
         return settings;
      }
   }

   protected static StsService getSts(PrivateKey privateKey, X509Certificate cert, LookupSvcConnection conn, ResourceFactory adminFactory, VlsiSettings lsSettings) {
      ServiceEndpoint stsEndpoint = conn.getSts();
      ServiceEndpoint ssoAdminEndpoint = conn.getAdmin();
      VlsiSettings adminSettings = mkAdminSettings(ssoAdminEndpoint, lsSettings);
      SsoAdminConnection ssoAdmin = (SsoAdminConnection)adminFactory.acquire(adminSettings);
      Throwable var10 = null;

      X509Certificate[] stsCerts;
      try {
         stsCerts = ssoAdmin.getSigningCerts();
      } catch (Throwable var19) {
         var10 = var19;
         throw var19;
      } finally {
         if (ssoAdmin != null) {
            if (var10 != null) {
               try {
                  ssoAdmin.close();
               } catch (Throwable var18) {
                  var10.addSuppressed(var18);
               }
            } else {
               ssoAdmin.close();
            }
         }

      }

      return (StsService)(privateKey == null ? new StsService(stsEndpoint, stsCerts) : new HokStsService(stsEndpoint, stsCerts, privateKey, cert));
   }

   protected static VlsiSettings mkAdminSettings(ServiceEndpoint ssoAdminEndpoint, VlsiSettings lsSettings) {
      URI uri = ssoAdminEndpoint.getUri();
      HttpSettings lsHttpSettings = lsSettings.getHttpSettings();
      HttpSettings httpSettings = new HttpSettings(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), (String)null, (String)null, -1, lsHttpSettings.getMaxConn(), lsHttpSettings.getTimeout(), (ClientCertificate)null, (ClientCertificate)null, new SingleThumbprintVerifier(ssoAdminEndpoint.getThumbprint()), lsHttpSettings.getExecutor(), version1.class, lsSettings.getHttpSettings().getVmodlContext(), Collections.emptyMap());
      return new VlsiSettings(lsSettings.getHttpFactory(), httpSettings, new Authenticator(), (String)null);
   }
}
