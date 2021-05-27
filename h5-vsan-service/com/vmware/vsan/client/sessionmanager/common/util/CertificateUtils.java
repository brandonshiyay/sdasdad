package com.vmware.vsan.client.sessionmanager.common.util;

import com.vmware.vim.vmomi.core.impl.SslUtil;
import com.vmware.vsan.client.util.net.ssl.SslUtils;
import java.net.URL;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class CertificateUtils {
   private CertificateUtils() {
   }

   public static String getServerThumbprint(String url) throws Exception {
      X509Certificate cert = getServerCert(url);
      return SslUtil.computeCertificateThumbprint(cert);
   }

   public static X509Certificate getServerCert(String url) throws Exception {
      URL urlAddr = new URL(url);
      SSLContext sslContext = SslUtils.createTrustAllSslContext();
      HttpsURLConnection con = (HttpsURLConnection)urlAddr.openConnection();
      con.setSSLSocketFactory(sslContext.getSocketFactory());
      con.setHostnameVerifier((hostname, session) -> {
         return true;
      });
      con.connect();

      X509Certificate var4;
      try {
         var4 = (X509Certificate)con.getServerCertificates()[0];
      } finally {
         con.disconnect();
      }

      return var4;
   }
}
