package com.vmware.vsan.client.sessionmanager.vlsi.client.ls;

import com.vmware.vim.binding.lookup.version.version4_0;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vsan.client.sessionmanager.common.util.ClientCertificate;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.NotAccessibleException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LookupSvcClient {
   private static final Logger logger = LoggerFactory.getLogger(LookupSvcClient.class);
   public static final String HTTPS = "https";
   public static final String LS_URI_DEFAULT_PATH = "/lookupservice/sdk";
   @Autowired
   private VlsiSettings vlsiSettingsTemplate;
   @Autowired
   @Qualifier("lsFactory")
   private ResourceFactory lsFactory;
   @Autowired
   private LookupSvcLocator lsLocator;

   public LookupSvcConnection getConnection() {
      return this.getConnection(this.lsLocator.getInfo());
   }

   public LookupSvcConnection getConnection(LookupSvcInfo lsInfo) {
      long ts = System.currentTimeMillis();

      try {
         LookupSvcConnection connection = (LookupSvcConnection)this.lsFactory.acquire(this.getSettings(lsInfo));
         logger.debug("Connection acquired: {} ({} ms)", connection, System.currentTimeMillis() - ts);
         return connection;
      } catch (Exception var5) {
         throw new NotAccessibleException(var5);
      }
   }

   public LookupSvcInfo getLocalLsInfo() {
      return this.lsLocator.getInfo();
   }

   public VlsiSettings getSettings(LookupSvcInfo lsInfo) {
      if (lsInfo == null) {
         lsInfo = this.getLocalLsInfo();
      }

      ThumbprintVerifier thumbprintVerifier = lsInfo.getThumbprintVerifier();
      ClientCertificate trustStore = null;
      if (lsInfo.getKeyStore() != null) {
         trustStore = new ClientCertificate(lsInfo.getAddress().getHost(), lsInfo.getKeyStore(), "", "", lsInfo.getAddress().getHost());
      }

      VlsiSettings settings = this.vlsiSettingsTemplate.setServiceInfo(lsInfo.getAddress(), version4_0.class).setSslContext(trustStore, thumbprintVerifier);
      return settings;
   }

   public ResourceFactory getProducerFactory() {
      return this.lsFactory;
   }

   public static URI createServiceUri(String host, Integer port) throws URISyntaxException {
      return new URI("https", (String)null, host, port, "/lookupservice/sdk", (String)null, (String)null);
   }
}
