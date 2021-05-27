package com.vmware.vsan.client.sessionmanager.vlsi.client.http;

import com.vmware.vim.vmomi.client.Client;
import com.vmware.vim.vmomi.client.http.HttpClientConfiguration;
import com.vmware.vim.vmomi.client.http.HttpConfiguration;
import com.vmware.vim.vmomi.client.http.HttpConfiguration.Factory;
import com.vmware.vim.vmomi.core.types.VmodlVersion;
import com.vmware.vsan.client.sessionmanager.common.util.ClientCertificate;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpFactory implements ResourceFactory {
   private static Logger logger = LoggerFactory.getLogger(HttpFactory.class);

   public ClientCfg acquire(HttpSettings httpSettings) {
      HttpConfiguration result = Factory.newInstance();
      if (httpSettings.isViaProxy()) {
         result.setDefaultProxy(httpSettings.getProxyHost(), httpSettings.getProxyPort(), httpSettings.getProxyProto());
      }

      if (httpSettings.getMaxConn() > 0) {
         result.setMaxConnections(httpSettings.getMaxConn());
         result.setDefaultMaxConnectionsPerRoute(httpSettings.getMaxConn());
      }

      if (httpSettings.getTimeout() > 0) {
         result.setTimeoutMs(httpSettings.getTimeout());
         result.setConnectTimeoutMs(httpSettings.getTimeout());
      }

      if (httpSettings.getTrustStore() != null) {
         result.getKeyStoreConfig().setTrustStorePassword(httpSettings.getTrustStore().getKeystorePass());
         result.getKeyStoreConfig().setKeyStorePath(httpSettings.getTrustStore().getKeystorePath());
         result.setTrustStore(httpSettings.getTrustStore().getKeystore());
      }

      if (httpSettings.getClientCert() != null) {
         ClientCertificate cert = httpSettings.getClientCert();
         result.getKeyStoreConfig().setKeyAlias(cert.getKeystoreAlias());
         result.getKeyStoreConfig().setKeyPassword(cert.getKeyPass());
         result.setKeyStore(cert.getKeystore());
      }

      if (httpSettings.getThumbprintVerifier() != null) {
         result.setThumbprintVerifier(httpSettings.getThumbprintVerifier());
      }

      HttpClientConfiguration httpClientConfiguration = com.vmware.vim.vmomi.client.http.HttpClientConfiguration.Factory.newInstance();
      httpClientConfiguration.setHttpConfiguration(result);
      httpClientConfiguration.setExecutor(httpSettings.getExecutor());
      if (httpSettings.getRequestProperties() != null) {
         httpClientConfiguration.setRequestContextProvider(new HttpRequestContextProvider(httpSettings.getRequestProperties()));
      }

      VmodlVersion vmodlVersion = httpSettings.getVmodlContext().getVmodlVersionMap().getVersion(httpSettings.getVersion());
      Client cl = com.vmware.vim.vmomi.client.Client.Factory.createClient(httpSettings.makeUri(), vmodlVersion.getVersionClass(), httpSettings.getVmodlContext(), httpClientConfiguration);
      ClientCfg res = new ClientCfg(httpClientConfiguration, cl);
      res.setCloseHandler(() -> {
         this.release(res);
      });
      return res;
   }

   private void release(ClientCfg resource) {
      resource.getExtraClient().shutdown();
   }
}
