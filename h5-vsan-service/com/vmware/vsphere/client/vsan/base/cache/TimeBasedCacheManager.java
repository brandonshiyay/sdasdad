package com.vmware.vsphere.client.vsan.base.cache;

import com.vmware.vise.security.ClientSessionEndListener;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class TimeBasedCacheManager implements ClientSessionEndListener {
   private static final Log _logger = LogFactory.getLog(TimeBasedCacheManager.class);
   private final int expirationTimeMin;
   private final int expirationTimeMax;
   private final int trustPeriod;
   private final int cleanThreshold;
   private ConcurrentHashMap _sessionMap = new ConcurrentHashMap();

   public TimeBasedCacheManager(int expirationTimeMin, int expirationTimeMax, int trustPeriod, int cleanThreshold) {
      Validate.isTrue(expirationTimeMin > 0);
      Validate.isTrue(expirationTimeMax > expirationTimeMin);
      Validate.isTrue(trustPeriod >= 0);
      Validate.isTrue(cleanThreshold > 0);
      this.expirationTimeMin = expirationTimeMin;
      this.expirationTimeMax = expirationTimeMax;
      this.trustPeriod = trustPeriod;
      this.cleanThreshold = cleanThreshold;
   }

   protected Cacheable get(Object params) {
      ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

      Cacheable var5;
      try {
         Thread.currentThread().setContextClassLoader(TimeBasedCacheManager.class.getClassLoader());
         this.clean();
         String key = this.getKey(params);
         TimeBasedCacheEntry result;
         if (StringUtils.isEmpty(key)) {
            _logger.warn("Cannot generate a key from params: " + params);
            result = null;
            return result;
         }

         result = this.getCacheEntry(key, params);
         if (result != null) {
            var5 = result.get();
            return var5;
         }

         var5 = null;
      } finally {
         Thread.currentThread().setContextClassLoader(oldClassLoader);
      }

      return var5;
   }

   protected abstract String sessionKey();

   protected abstract String getKey(Object var1);

   protected abstract TimeBasedCacheEntry createEntry(Object var1);

   private TimeBasedCacheEntry getCacheEntry(String key, Object params) {
      ConcurrentHashMap cache = this.getSessionCache();
      TimeBasedCacheEntry result = (TimeBasedCacheEntry)cache.get(key);
      if (result == null) {
         TimeBasedCacheEntry newEntity = this.createEntry(params);
         if (newEntity == null) {
            _logger.warn("Cannot create a cache entry for params: " + params);
            return null;
         }

         int expirationTime = this.calculateExpirationTime();
         newEntity.setExpirationTime(expirationTime);
         newEntity.setTrustPeriod(this.trustPeriod);
         result = (TimeBasedCacheEntry)cache.putIfAbsent(key, newEntity);
         if (result == null) {
            _logger.debug("Cache entry created: {" + key + "} => {" + newEntity + "}");
            result = newEntity;
         }
      }

      return result;
   }

   private ConcurrentHashMap getSessionCache() {
      ConcurrentHashMap cache = (ConcurrentHashMap)this._sessionMap.get(this.sessionKey());
      if (cache == null) {
         ConcurrentHashMap newCache = new ConcurrentHashMap();
         cache = (ConcurrentHashMap)this._sessionMap.putIfAbsent(this.sessionKey(), newCache);
         if (cache == null) {
            _logger.debug("Session entry created: {" + this.sessionKey() + "} => {" + newCache + "}");
            cache = newCache;
         }
      }

      return cache;
   }

   private int calculateExpirationTime() {
      return ThreadLocalRandom.current().nextInt(this.expirationTimeMin, this.expirationTimeMax);
   }

   public void sessionEnded(String clientId) {
      if (this._sessionMap.containsKey(clientId)) {
         this._sessionMap.remove(clientId);
      }

   }

   public void shutdown() {
      this._sessionMap.clear();
   }

   private void clean() {
      ConcurrentHashMap cache = (ConcurrentHashMap)this._sessionMap.get(this.sessionKey());
      if (cache != null && cache.size() >= this.cleanThreshold) {
         synchronized(cache) {
            Iterator var3 = cache.entrySet().iterator();

            while(var3.hasNext()) {
               Entry entry = (Entry)var3.next();
               String key = (String)entry.getKey();
               TimeBasedCacheEntry value = (TimeBasedCacheEntry)entry.getValue();
               if (value.isExpired()) {
                  cache.remove(key);
               }
            }

         }
      }
   }
}
