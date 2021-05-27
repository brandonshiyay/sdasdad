package com.vmware.vsan.client.sessionmanager.resource;

import java.io.Closeable;

public class Resource implements Closeable {
   private Runnable onClose;

   public void setCloseHandler(Runnable onClose) {
      this.onClose = onClose;
   }

   public Runnable getCloseHandler() {
      return this.onClose;
   }

   public void close() {
      if (this.onClose != null) {
         this.onClose.run();
      }

   }
}
