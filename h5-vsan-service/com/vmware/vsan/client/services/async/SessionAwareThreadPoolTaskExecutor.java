package com.vmware.vsan.client.services.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.concurrent.BasicThreadFactory.Builder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class SessionAwareThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
   protected ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
      BasicThreadFactory t = (new Builder()).wrappedFactory(new SessionAwareThreadPoolTaskExecutor.SessionAwareThreadFactory()).namingPattern("vsan-async-thread-%d").build();
      return super.initializeExecutor(t, rejectedExecutionHandler);
   }

   public static class SessionAwareThreadFactory implements ThreadFactory {
      public Thread newThread(Runnable r) {
         return new SessionAwareThread(r);
      }
   }
}
