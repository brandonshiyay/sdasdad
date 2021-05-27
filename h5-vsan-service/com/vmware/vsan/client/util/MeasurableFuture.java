package com.vmware.vsan.client.util;

import com.vmware.vim.vmomi.core.impl.BlockingFuture;

public class MeasurableFuture extends BlockingFuture {
   private final Measure closeHandler;

   public MeasurableFuture(Measure measure, String task) {
      this.closeHandler = measure.start(task);
   }

   public void set(Object ret) {
      try {
         this.closeHandler.close();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      super.set(ret);
   }

   public void setException(Exception fault) {
      try {
         this.closeHandler.close();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      super.setException(fault);
   }
}
