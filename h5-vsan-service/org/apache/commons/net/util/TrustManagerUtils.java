package org.apache.commons.net.util;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public final class TrustManagerUtils {
   private static final X509Certificate[] EMPTY_X509CERTIFICATE_ARRAY = new X509Certificate[0];
   private static final X509TrustManager ACCEPT_ALL = new TrustManagerUtils.TrustManager(false);
   private static final X509TrustManager CHECK_SERVER_VALIDITY = new TrustManagerUtils.TrustManager(true);

   public static X509TrustManager getAcceptAllTrustManager() {
      return ACCEPT_ALL;
   }

   public static X509TrustManager getValidateServerCertificateTrustManager() {
      return CHECK_SERVER_VALIDITY;
   }

   public static X509TrustManager getDefaultTrustManager(KeyStore keyStore) throws GeneralSecurityException {
      String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory instance = TrustManagerFactory.getInstance(defaultAlgorithm);
      instance.init(keyStore);
      return (X509TrustManager)instance.getTrustManagers()[0];
   }

   private static class TrustManager implements X509TrustManager {
      private final boolean checkServerValidity;

      TrustManager(boolean checkServerValidity) {
         this.checkServerValidity = checkServerValidity;
      }

      public void checkClientTrusted(X509Certificate[] certificates, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
         if (this.checkServerValidity) {
            X509Certificate[] arr$ = certificates;
            int len$ = certificates.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               X509Certificate certificate = arr$[i$];
               certificate.checkValidity();
            }
         }

      }

      public X509Certificate[] getAcceptedIssuers() {
         return TrustManagerUtils.EMPTY_X509CERTIFICATE_ARRAY;
      }
   }
}
