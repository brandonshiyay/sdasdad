package com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore;

import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class NgcRemoteTokenRetriever extends NgcTokenRetriever {
   private final String hostname;

   public NgcRemoteTokenRetriever(PrivateKey privateKey, X509Certificate cert, VlsiSettings lsSettings, ResourceFactory lsFactory, ResourceFactory adminFactory, AsyncUserSessionService userSessionService, String hostname) {
      super(privateKey, cert, lsSettings, lsFactory, adminFactory, userSessionService);
      this.hostname = hostname;
   }

   public TokenInfo retrieveToken() {
      TokenInfo localTokenInfo = super.retrieveToken();
      return this.acquireTokenForRemoteDomain(localTokenInfo, this.hostname);
   }
}
