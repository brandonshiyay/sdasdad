package com.vmware.vsan.client.sessionmanager.vlsi.client;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class NotAccessibleException extends RuntimeException {
   public NotAccessibleException() {
   }

   public NotAccessibleException(String message) {
      super(message);
   }

   public NotAccessibleException(Throwable cause) {
      super(cause);
   }

   public NotAccessibleException(String message, Throwable cause) {
      super(message, cause);
   }
}
