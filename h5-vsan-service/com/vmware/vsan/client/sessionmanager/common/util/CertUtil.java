package com.vmware.vsan.client.sessionmanager.common.util;

import com.vmware.vim.sso.client.util.codec.Base64;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertUtil {
   public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
   public static final String END_CERT = "-----END CERTIFICATE-----";

   public static KeyStore create(String... certificates) {
      try {
         KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
         keystore.load((InputStream)null, (char[])null);

         for(int i = 0; i < certificates.length; ++i) {
            Certificate certificate = parseX509(certificates[i]);
            keystore.setEntry("Cert_" + i, new TrustedCertificateEntry(certificate), (ProtectionParameter)null);
         }

         return keystore;
      } catch (RuntimeException var4) {
         throw var4;
      } catch (Exception var5) {
         throw new RuntimeException(var5);
      }
   }

   public static String extractCert(String cert) {
      int idx = cert.indexOf("-----BEGIN CERTIFICATE-----");
      if (idx >= 0) {
         cert = cert.substring(idx + "-----BEGIN CERTIFICATE-----".length());
      }

      idx = cert.indexOf("-----END CERTIFICATE-----");
      if (idx >= 0) {
         cert = cert.substring(0, idx);
      }

      cert = cert.trim().replace("\n", "").replace("\r", "");
      return cert;
   }

   public static X509Certificate parseX509(String cert) {
      byte[] decoded = Base64.decodeBase64(extractCert(cert));

      try {
         ByteArrayInputStream in = new ByteArrayInputStream(decoded);
         Throwable var3 = null;

         X509Certificate var5;
         try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            var5 = (X509Certificate)factory.generateCertificate(in);
         } catch (Throwable var16) {
            var3 = var16;
            throw var16;
         } finally {
            if (in != null) {
               if (var3 != null) {
                  try {
                     in.close();
                  } catch (Throwable var15) {
                     var3.addSuppressed(var15);
                  }
               } else {
                  in.close();
               }
            }

         }

         return var5;
      } catch (RuntimeException var18) {
         throw var18;
      } catch (Exception var19) {
         throw new RuntimeException(var19);
      }
   }
}
