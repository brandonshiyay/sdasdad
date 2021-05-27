package com.vmware.vsphere.client.vsan.base.util;

import com.vmware.vsan.client.util.net.ssl.SslUtils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;

public class NetUtils {
   public static final String HTTP_GET = "GET";
   public static final String CONTEXT_SSL = "SSL";

   public static HttpClient createTrustAllHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
      SSLContext sc = SslUtils.createTrustAllSslContext();
      return HttpClients.custom().setSSLContext(sc).build();
   }

   public static SSLSocketFactory getDisableSSLCertificateCheckingSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
      return SslUtils.createTrustAllSslContext().getSocketFactory();
   }

   public static HostnameVerifier createAllTrustingHostnameVerifier() {
      return new HostnameVerifier() {
         public boolean verify(String hostname, SSLSession session) {
            return true;
         }
      };
   }

   public static HttpsURLConnection createUntrustedConnection(String address) throws KeyManagementException, NoSuchAlgorithmException, MalformedURLException, IOException {
      return createUntrustedConnection(new URL(address));
   }

   public static HttpsURLConnection createUntrustedConnection(URL url) throws KeyManagementException, NoSuchAlgorithmException, IOException {
      HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
      conn.setSSLSocketFactory(getDisableSSLCertificateCheckingSocketFactory());
      conn.setHostnameVerifier(createAllTrustingHostnameVerifier());
      return conn;
   }

   public static boolean isSuccess(int responseCode) {
      return responseCode >= 200 && responseCode < 300;
   }
}
