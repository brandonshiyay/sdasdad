package com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore;

public class TokenStoreException extends RuntimeException {
   public TokenStoreException(String message) {
      super(message);
   }

   public TokenStoreException(String message, Throwable cause) {
      super(message, cause);
   }

   public TokenStoreException(Throwable cause) {
      super(cause);
   }

   public TokenStoreException() {
   }
}
