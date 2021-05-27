package com.vmware.vsan.client.services.thumbprint;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.util.net.ssl.SslUtils;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

@Component
public class SslThumbprintService {
   @TsService
   public String getThumbprint(String downloadUrl) throws Exception {
      URL url = new URL(downloadUrl);
      String host = url.getHost();
      int port = url.getPort();
      SSLContext sc = SslUtils.createTrustAllSslContext();
      SSLSocketFactory factory = sc.getSocketFactory();

      try {
         SSLSocket socket = (SSLSocket)factory.createSocket(host, port == -1 ? 443 : port);
         Throwable var8 = null;

         String var29;
         try {
            socket.startHandshake();
            SSLSession session = socket.getSession();
            Certificate[] serverCerts = session.getPeerCertificates();
            String thumbprint = null;
            if (ArrayUtils.isNotEmpty(serverCerts)) {
               StringBuilder stringBuilder = new StringBuilder();
               MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
               messageDigest.update(serverCerts[0].getEncoded());
               byte[] var14 = messageDigest.digest();
               int var15 = var14.length;

               for(int var16 = 0; var16 < var15; ++var16) {
                  byte b = var14[var16];
                  stringBuilder.append(String.format("%02x", b & 255)).append(':');
               }

               thumbprint = stringBuilder.toString().toUpperCase();
               thumbprint = thumbprint.substring(0, thumbprint.length() - 1);
            }

            var29 = thumbprint;
         } catch (Throwable var26) {
            var8 = var26;
            throw var26;
         } finally {
            if (socket != null) {
               if (var8 != null) {
                  try {
                     socket.close();
                  } catch (Throwable var25) {
                     var8.addSuppressed(var25);
                  }
               } else {
                  socket.close();
               }
            }

         }

         return var29;
      } catch (Exception var28) {
         throw new VsanUiLocalizableException("vsan.ssl.certificate.error", "Failed to get the ssl certificate for host: " + host, var28, new Object[]{host});
      }
   }
}
