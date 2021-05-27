package com.vmware.vsan.client.services.async;

public class SessionAwareThread extends Thread {
   public ThreadLocal userSession = new ThreadLocal();
   public ThreadLocal requestId = new ThreadLocal();

   public SessionAwareThread(Runnable runnable) {
      super(runnable);
   }
}
