package com.vmware.vsan.client.sessionmanager.vlsi.client.ls;

import com.vmware.vsan.client.sessionmanager.common.util.CertificateUtils;
import com.vmware.vsan.client.sessionmanager.common.util.ClientCertificate;
import com.vmware.vsan.client.util.config.ConfigurationUtil;
import java.net.URI;
import java.security.KeyStore;
import java.security.PrivateKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FallbackLookupSvcLocator implements LookupSvcLocator {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private String cmAddress;
   private String cmThumbprint;
   private KeyStore cmKeystore;
   private String keystorePass;
   private String localDomainId;

   private void retrieveCmProperties() throws Exception {
      String cmAddress = ConfigurationUtil.getCmUrl();
      if (cmAddress == null) {
         throw new IllegalStateException("Cannot find 'cm.url' in the local client configuration.");
      } else {
         this.logger.debug("Configured CM address is: {}", cmAddress);
         this.cmThumbprint = CertificateUtils.getServerThumbprint(cmAddress);
         this.logger.debug("Configured CM thumbprint is: {}", this.cmThumbprint);
         String keystorePath = ConfigurationUtil.getKeystorePath();
         String keystorePass = ConfigurationUtil.getKeystorePassword();
         if (keystorePath != null) {
            this.cmKeystore = (new ClientCertificate(keystorePath, keystorePass, "", KeyStore.getDefaultType(), "")).getKeystore();
         }

         this.cmAddress = cmAddress;
         this.keystorePass = keystorePass;
      }
   }

   public LookupSvcInfo getInfo() {
      try {
         if (this.cmAddress == null) {
            this.retrieveCmProperties();
         }

         URI cmAddress = new URI(this.cmAddress);
         URI lsAddress = new URI("https", cmAddress.getHost(), "/lookupservice/sdk", (String)null);
         LookupSvcInfo result = (new LookupSvcInfo(lsAddress, this.cmThumbprint)).copyWithKeyStore(this.cmKeystore);
         this.logger.trace("Current LS is: {}", result);
         return result;
      } catch (Exception var4) {
         throw new IllegalStateException("Failed to retrieve current LookupSvcInfo.", var4);
      }
   }

   public String getNodeId() {
      if (this.localDomainId == null) {
         this.localDomainId = ConfigurationUtil.getLocalDomainId();
      }

      return this.localDomainId;
   }

   public PrivateKey getPrivateKey() {
      try {
         return (PrivateKey)this.getInfo().getKeyStore().getKey("vsphere-webclient", this.keystorePass.toCharArray());
      } catch (Exception var2) {
         throw new IllegalStateException("Failed to extract private key from key store.", var2);
      }
   }

   public KeyStore getH5Keystore() {
      return this.getInfo().getKeyStore();
   }
}
