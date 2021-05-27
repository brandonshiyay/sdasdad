package com.vmware.vsan.client.sessionmanager.vlsi.client;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class NotAuthenticatedException extends RuntimeException {
   public NotAuthenticatedException() {
   }

   public NotAuthenticatedException(String message) {
      super(message);
   }

   public NotAuthenticatedException(Throwable cause) {
      super(cause);
   }

   public NotAuthenticatedException(String message, Throwable cause) {
      super(message, cause);
   }
}
