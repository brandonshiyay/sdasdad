package com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore;

public class TokenAcquisitionException extends TokenStoreException {
   public TokenAcquisitionException(String message) {
      super(message);
   }

   public TokenAcquisitionException(String message, Throwable cause) {
      super(message, cause);
   }

   public TokenAcquisitionException(Throwable cause) {
      super(cause);
   }

   public TokenAcquisitionException() {
   }
}
