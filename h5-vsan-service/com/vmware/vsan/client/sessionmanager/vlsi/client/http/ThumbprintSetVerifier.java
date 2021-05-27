package com.vmware.vsan.client.sessionmanager.vlsi.client.http;

import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier.Result;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;

public class ThumbprintSetVerifier implements ThumbprintVerifier {
   protected final Set thumbprints;

   public ThumbprintSetVerifier(Set thumbprints) {
      this.thumbprints = thumbprints;
   }

   public ThumbprintSetVerifier(String... strings) {
      this.thumbprints = new HashSet();
      String[] var2 = strings;
      int var3 = strings.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String thumbprint = var2[var4];
         this.thumbprints.add(thumbprint.toLowerCase());
      }

   }

   public Result verify(String thumbprint) {
      return this.thumbprints.contains(thumbprint.toLowerCase()) ? Result.MATCH : Result.MISMATCH;
   }

   public void onSuccess(X509Certificate[] chain, String thumbprint, Result verifyResult, boolean trustedChain, boolean verifiedAssertions) throws SSLException {
   }
}
