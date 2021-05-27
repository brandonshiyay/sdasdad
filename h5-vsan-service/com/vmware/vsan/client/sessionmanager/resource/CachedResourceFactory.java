package com.vmware.vsan.client.sessionmanager.resource;

import com.vmware.vsan.client.sessionmanager.common.util.EqualityLock;
import com.vmware.vsan.client.sessionmanager.common.util.RequestUtil;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanVlsiSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedResourceFactory implements ResourceFactory {
   protected static final long GC_TIMEOUT = 30000L;
   private static final Logger logger = LoggerFactory.getLogger(CachedResourceFactory.class);
   protected final Map cache = new HashMap();
   protected final ResourceFactory factory;
   protected volatile boolean isShutdown = false;
   protected final EqualityLock locks = new EqualityLock();

   public CachedResourceFactory(ResourceFactory factory) {
      this.factory = factory;
   }

   public Resource acquire(final Object settings) {
      CacheEntry entry = null;
      Object entryLock = null;
      synchronized(this) {
         if (this.isShutdown) {
            throw new IllegalStateException("Attempt to acquire resource from a shutdown factory");
         }

         entry = (CacheEntry)this.cache.get(settings);
         if (entry != null) {
            entry.incRefCount();
            logger.debug("Acquired cached connection (RC={}): {}", entry.getRefCount(), entry.getResource());
            return (Resource)entry.getResource();
         }

         entryLock = this.locks.getLock(settings);
      }

      synchronized(entryLock) {
         label52: {
            Resource var10000;
            synchronized(this) {
               entry = (CacheEntry)this.cache.get(settings);
               if (entry == null) {
                  break label52;
               }

               entry.incRefCount();
               logger.debug("Acquired cached connection (RC={}): {}", entry.getRefCount(), entry.getResource());
               var10000 = (Resource)entry.getResource();
            }

            return var10000;
         }

         Resource resource = this.factory.acquire(settings);
         Runnable parentCloseHandler = resource.getCloseHandler();
         resource.setCloseHandler(new Runnable() {
            public void run() {
               CachedResourceFactory.this.release(settings);
            }
         });
         entry = new CacheEntry(resource, parentCloseHandler);
         logger.debug("Acquired connection from factory (RC={}): {}", entry.getRefCount(), entry.getResource());
         synchronized(this) {
            this.cache.put(settings, entry);
            this.notify();
         }

         return resource;
      }
   }

   protected synchronized void release(Object settings) {
      CacheEntry entry = (CacheEntry)this.cache.get(settings);
      if (entry == null) {
         logger.warn("Not found in cache: " + settings);
      } else {
         entry.decRefCount();
         this.notify();
         logger.debug("Released connection (RC={}): {}", entry.getRefCount(), entry.getResource());
      }
   }

   public Resource evict(Object settings) {
      CacheEntry entry = null;
      synchronized(this) {
         entry = (CacheEntry)this.cache.get(settings);
         if (entry == null) {
            throw new IllegalStateException("Evicting a resource which is not in the cache!");
         }

         if (entry.getRefCount() > 0) {
            logger.debug("Connection won't be evicted, ref-count is {}: {}", entry.getRefCount(), entry.getResource());
            return null;
         }

         this.cache.remove(settings);
         this.locks.evict(settings);
         logger.debug("Evicted connection {} ref-count 0 reached at {}", entry.getResource(), new Date(entry.getLastReleaseTime()));
      }

      if (entry.getParentCloseHandler() != null) {
         logger.trace("Invoking original close handler to close the connection: {}", entry.getResource());

         try {
            entry.getParentCloseHandler().run();
         } catch (Exception var5) {
            logger.warn("Ignoring unsuccessful connection close: {}", entry.getResource(), var5);
         }
      } else {
         logger.trace("No original close handler to invoke: {}", entry.getResource());
      }

      return (Resource)entry.getResource();
   }

   public List removeRequestEntries() {
      String requestId = RequestUtil.getVsanRequestIdKey();
      logger.debug("Removing all entries that have 0 ref-count for request " + requestId);
      if (StringUtils.isEmpty(requestId)) {
         logger.warn("Skipping removal of entries for missing requestId.");
         return Collections.EMPTY_LIST;
      } else {
         List evictedEntries = new ArrayList();
         synchronized(this) {
            Iterator it = this.cache.keySet().iterator();

            while(true) {
               if (!it.hasNext()) {
                  break;
               }

               Object settings = it.next();
               CacheEntry entry = (CacheEntry)this.cache.get(settings);
               if (entry.getRefCount() <= 0 && this.belongsToRequest(requestId, settings)) {
                  logger.trace("Evicting entry: {}", entry);
                  evictedEntries.add(entry);
                  it.remove();
                  this.locks.evict(settings);
               } else {
                  logger.trace("Not evicting entry, not applicable: {}", entry);
               }
            }
         }

         logger.debug("Remaining cached connections count: " + this.cache.size());
         return this.closeEntries(evictedEntries);
      }
   }

   private boolean belongsToRequest(String requestId, Object settings) {
      return settings instanceof VsanVlsiSettings && requestId.equals(((VsanVlsiSettings)settings).getClientRequestId());
   }

   public List evictAll(long releasedBefore) {
      logger.debug("Evicting all entries that have 0 ref-count since " + new Date(releasedBefore));
      List evictedEntries = new ArrayList();
      synchronized(this) {
         Iterator it = this.cache.keySet().iterator();

         while(it.hasNext()) {
            Object settings = it.next();
            CacheEntry entry = (CacheEntry)this.cache.get(settings);
            if (entry.getRefCount() <= 0 && entry.getLastReleaseTime() <= releasedBefore) {
               logger.trace("Evicting entry: {}", entry);
               evictedEntries.add(entry);
               it.remove();
               this.locks.evict(settings);
            } else {
               logger.trace("Not evicting entry, not applicable: {}", entry);
            }
         }

         return this.closeEntries(evictedEntries);
      }
   }

   private List closeEntries(List evictedEntries) {
      List result = new ArrayList();

      CacheEntry entry;
      for(Iterator var3 = evictedEntries.iterator(); var3.hasNext(); result.add(entry.getResource())) {
         entry = (CacheEntry)var3.next();
         logger.trace("Closing evicted entity: {}", entry);

         try {
            if (entry.getParentCloseHandler() != null) {
               entry.getParentCloseHandler().run();
            }
         } catch (RuntimeException var6) {
            logger.warn("Ignoring unsuccessful resource close: {}", entry, var6);
         }
      }

      logger.debug("Evicted {} entries.", evictedEntries.size());
      return result;
   }

   public synchronized void gc() {
      this.gcImpl();
   }

   protected void gcImpl() {
      long now = System.currentTimeMillis();

      for(long deadline = now + 30000L; now <= deadline; now = System.currentTimeMillis()) {
         List settings = new ArrayList(this.cache.keySet());
         Iterator var6 = settings.iterator();

         while(var6.hasNext()) {
            Object setting = var6.next();
            this.evict(setting);
         }

         if (this.cache.isEmpty()) {
            return;
         }

         try {
            this.wait(deadline - now);
         } catch (InterruptedException var8) {
            logger.warn("Garbage collector interrupted while waiting!");
         }
      }

      logger.warn("Garbage collector was unable to collect " + this.cache.size() + " entries, which are still in use");
   }

   public synchronized void shutdown() {
      logger.debug("Shut-down initiated, evicting all cached entities.");
      this.isShutdown = true;
      this.gcImpl();
   }
}
