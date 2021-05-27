package com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore;

public class TokenExpiredException extends TokenStoreException {
   public TokenExpiredException(String message) {
      super(message);
   }

   public TokenExpiredException(String message, Throwable cause) {
      super(message, cause);
   }

   public TokenExpiredException(Throwable cause) {
      super(cause);
   }

   public TokenExpiredException() {
   }
}
