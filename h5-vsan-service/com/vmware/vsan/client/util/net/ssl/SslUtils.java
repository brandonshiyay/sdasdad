package com.vmware.vsan.client.util.net.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.commons.net.util.TrustManagerUtils;

public class SslUtils {
   private static final String TLS = "TLS";

   public static SSLContext createTrustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
      TrustManager acceptAllTrustManager = TrustManagerUtils.getAcceptAllTrustManager();
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init((KeyManager[])null, new TrustManager[]{acceptAllTrustManager}, (SecureRandom)null);
      return sslContext;
   }
}
