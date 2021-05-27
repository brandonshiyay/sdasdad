package com.vmware.vsan.client.util.retriever;

import com.vmware.vsan.client.util.Measure;
import java.util.concurrent.Future;
import org.apache.commons.lang3.Validate;

class ClosureDataRetriever implements DataRetriever {
   private String name;
   private ClosureDataRetriever.Closure closure;
   private Future future;
   private Measure measure;
   private Exception exception;

   public ClosureDataRetriever(String name, Measure measure, ClosureDataRetriever.Closure closure) {
      Validate.notNull(name);
      Validate.notNull(measure);
      Validate.notNull(closure);
      this.name = name;
      this.measure = measure;
      this.closure = closure;
   }

   public void start() {
      try {
         if (this.closure instanceof ClosureDataRetriever.ClosureWithFuture) {
            ClosureDataRetriever.ClosureWithFuture closureWithFuture = (ClosureDataRetriever.ClosureWithFuture)this.closure;
            this.future = this.measure.newFuture("ClosureDataRetriever.loadData[" + this.name + "]");
            closureWithFuture.execute((com.vmware.vim.vmomi.core.Future)this.future);
         } else if (this.closure instanceof ClosureDataRetriever.ClosureWithoutFuture) {
            ClosureDataRetriever.ClosureWithoutFuture closureWithoutFuture = (ClosureDataRetriever.ClosureWithoutFuture)this.closure;
            Future future = closureWithoutFuture.execute();
            if (future == null) {
               throw new NullPointerException("The closure must return a non-null future instance.");
            }

            this.future = future;
         }
      } catch (Exception var3) {
         this.exception = var3;
      }

   }

   public Object getResult() {
      if (this.exception != null) {
         throw new RuntimeException(this.exception);
      } else {
         try {
            return this.future.get();
         } catch (Exception var2) {
            throw new RuntimeException(var2);
         }
      }
   }

   @FunctionalInterface
   public interface ClosureWithoutFuture extends ClosureDataRetriever.Closure {
      Future execute() throws Exception;
   }

   @FunctionalInterface
   public interface ClosureWithFuture extends ClosureDataRetriever.Closure {
      void execute(com.vmware.vim.vmomi.core.Future var1) throws Exception;
   }

   interface Closure {
   }
}
