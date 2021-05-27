package com.vmware.vsan.client.sessionmanager.health;

import com.vmware.vsan.client.sessionmanager.common.util.CheckedRunnable;
import com.vmware.vsan.client.sessionmanager.resource.CacheEntry;
import com.vmware.vsan.client.sessionmanager.resource.CachedResourceFactory;
import com.vmware.vsan.client.sessionmanager.resource.Resource;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HealthCheckingFactory extends CachedResourceFactory {
   private static final Log logger = LogFactory.getLog(HealthCheckingFactory.class);
   protected final ScheduledExecutorService scheduler;
   protected final long timeout;
   protected final IHealthMonitor monitor;
   protected final ScheduledFuture pinger;
   protected final ScheduledFuture evictor;

   public HealthCheckingFactory(ResourceFactory parentFactory, IHealthMonitor monitor, ScheduledExecutorService scheduler, long delay, long timeout, final long retention) {
      super((settings) -> {
         Resource resource;
         Exception e;
         try {
            resource = parentFactory.acquire(settings);
         } catch (Exception var9) {
            e = var9;

            try {
               monitor.onError((Resource)null, settings, e);
            } catch (Exception var7) {
               logger.warn("onError failure", var7);
            }

            throw var9;
         }

         try {
            monitor.onCreated(resource, settings);
         } catch (Exception var8) {
            e = var8;

            try {
               logger.warn("Closing resource due to an onCreated handler failure", e);
               resource.close();
            } catch (Exception var6) {
               logger.warn("Could not dispose of resource", var6);
            }

            throw var8;
         }

         Runnable originalCloseHandler = resource.getCloseHandler();
         resource.setCloseHandler(() -> {
            if (originalCloseHandler != null) {
               originalCloseHandler.run();
            }

            monitor.onDisposed(resource, settings);
         });
         return resource;
      });
      this.scheduler = scheduler;
      this.timeout = timeout;
      this.monitor = monitor;
      this.pinger = scheduler.scheduleWithFixedDelay(new Runnable() {
         public void run() {
            HealthCheckingFactory.this.checkEntries();
         }
      }, delay, delay, TimeUnit.MILLISECONDS);
      if (retention > 0L) {
         this.evictor = scheduler.scheduleWithFixedDelay(new Runnable() {
            public void run() {
               HealthCheckingFactory.this.evictAll(System.currentTimeMillis() - retention);
            }
         }, retention, retention, TimeUnit.MILLISECONDS);
      } else {
         this.evictor = null;
      }

   }

   public void checkEntries() {
      try {
         this.checkEntriesImpl();
      } catch (Exception var2) {
         CheckedRunnable.handle(var2);
      }

   }

   protected void checkEntriesImpl() {
      List checks = new ArrayList();
      Map snapshot = new HashMap();
      synchronized(this) {
         snapshot.putAll(this.cache);
      }

      Iterator var3 = snapshot.entrySet().iterator();

      while(var3.hasNext()) {
         Entry entry = (Entry)var3.next();

         try {
            final Object settings = entry.getKey();
            final Resource resource = (Resource)((CacheEntry)entry.getValue()).getResource();
            checks.add(new HealthCheckingFactory.CheckInProgress(settings, this.scheduler.submit(new Runnable() {
               public void run() {
                  try {
                     HealthCheckingFactory.this.monitor.check(resource, settings);
                  } catch (RuntimeException var2) {
                     throw var2;
                  } catch (Exception var3) {
                     throw new RuntimeException("Health-check failed.", var3);
                  }
               }
            })));
         } catch (RejectedExecutionException var28) {
            logger.warn("Could not schedule check for entry " + entry.getKey(), var28);
            return;
         }
      }

      List brokenEntries = new ArrayList();
      long timeout = 30000L;
      long deadline = System.currentTimeMillis() + timeout;
      Iterator var8 = checks.iterator();

      while(var8.hasNext()) {
         HealthCheckingFactory.CheckInProgress check = (HealthCheckingFactory.CheckInProgress)var8.next();

         try {
            logger.trace(String.format("Awaiting check for %s, with timeout %s", check.getSettings(), timeout));
            check.getFuture().get(timeout, TimeUnit.MILLISECONDS);
            logger.trace("Resource is healthy " + check.getSettings());
         } catch (InterruptedException var25) {
            logger.debug("Interrupted while checking resource: " + check.getSettings(), var25.getCause());
            brokenEntries.add(new HealthCheckingFactory.BrokenEntry(check.getSettings(), var25.getCause()));
         } catch (ExecutionException var26) {
            logger.debug("Resource is broken: " + check.getSettings(), var26.getCause());
            brokenEntries.add(new HealthCheckingFactory.BrokenEntry(check.getSettings(), var26.getCause()));
         } catch (TimeoutException var27) {
            logger.debug("Timeout while waiting for health check for resource: " + check.getSettings());
            brokenEntries.add(new HealthCheckingFactory.BrokenEntry(check.getSettings(), var27.getCause()));
         } finally {
            timeout = deadline - System.currentTimeMillis();
            if (timeout < 0L) {
               timeout = 0L;
            }

         }
      }

      var8 = brokenEntries.iterator();

      while(var8.hasNext()) {
         HealthCheckingFactory.BrokenEntry brokenEntry = (HealthCheckingFactory.BrokenEntry)var8.next();
         CacheEntry entry = (CacheEntry)this.cache.get(brokenEntry.settings);

         try {
            this.monitor.onError(entry == null ? null : (Resource)entry.getResource(), brokenEntry.settings, brokenEntry.error);
         } catch (Exception var24) {
            logger.warn("onError failure", var24);
         }
      }

      List closeHandlers = new ArrayList();
      synchronized(this) {
         Iterator var38 = brokenEntries.iterator();

         while(true) {
            if (!var38.hasNext()) {
               this.notify();
               break;
            }

            HealthCheckingFactory.BrokenEntry brokenEntry = (HealthCheckingFactory.BrokenEntry)var38.next();
            CacheEntry entry = (CacheEntry)this.cache.get(brokenEntry.settings);
            if (entry != null) {
               if (entry.getRefCount() > 0) {
                  logger.debug(String.format("Evicting broken resource: %s, with non-zero refcount: %s", entry.getResource(), entry.getRefCount()));
               }

               this.cache.remove(brokenEntry.settings);
               this.locks.evict(brokenEntry.settings);
               if (entry.getParentCloseHandler() != null) {
                  closeHandlers.add(entry.getParentCloseHandler());
               }

               logger.debug("Evicted broken resource " + entry.getResource());
            }
         }
      }

      Iterator var37 = closeHandlers.iterator();

      while(var37.hasNext()) {
         Runnable closeHandler = (Runnable)var37.next();

         try {
            closeHandler.run();
         } catch (Exception var23) {
            logger.warn("Ignoring unsuccessful disposal: {}", var23);
         }
      }

   }

   public synchronized void shutdown() {
      this.pinger.cancel(false);
      if (this.evictor != null) {
         this.evictor.cancel(false);
      }

      super.shutdown();
   }

   protected class BrokenEntry {
      public final Object settings;
      public Throwable error;

      public BrokenEntry(Object settings, Throwable error) {
         this.settings = settings;
         this.error = error;
      }
   }

   protected class CheckInProgress {
      protected Object settings;
      protected Future future;

      public CheckInProgress(Object settings, Future future) {
         this.settings = settings;
         this.future = future;
      }

      public Object getSettings() {
         return this.settings;
      }

      public Future getFuture() {
         return this.future;
      }
   }
}
