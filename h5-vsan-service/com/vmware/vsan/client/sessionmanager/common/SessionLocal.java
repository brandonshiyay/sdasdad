package com.vmware.vsan.client.sessionmanager.common;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SessionLocal {
   private static final Logger logger = LoggerFactory.getLogger(SessionLocal.class);
   private ConcurrentHashMap sessionContext = new ConcurrentHashMap();

   protected Object get(String objectId) {
      String key = this.sessionKey() + objectId;
      Object result = this.sessionContext.get(key);
      if (result == null) {
         Object newEntity = this.create(objectId);
         result = this.sessionContext.putIfAbsent(key, newEntity);
         if (result == null) {
            logger.debug("Session entry created: {} => {}", key, newEntity);
            result = newEntity;
         } else {
            this.destroy(newEntity);
         }
      }

      return result;
   }

   protected Object get() {
      return this.get("");
   }

   protected void remove(String clientId) {
      try {
         String sessionKey = clientId != null ? clientId : this.sessionKey();
         Iterator iterator = this.sessionContext.entrySet().iterator();

         while(iterator.hasNext()) {
            Entry entry = (Entry)iterator.next();
            String key = (String)entry.getKey();
            if (key.startsWith(sessionKey)) {
               iterator.remove();
               Object value = entry.getValue();
               if (value != null) {
                  this.destroy(value);
                  logger.debug("Session entry dropped: {} => {}", key, value);
               }
            }
         }
      } catch (Exception var7) {
         logger.error("Failed to clear client's session context: {}", this, var7);
      }

   }

   protected void clear() {
      logger.debug("Dropping all session entries: {}", this.sessionContext.size());
      Iterator iterator = this.sessionContext.entrySet().iterator();

      while(iterator.hasNext()) {
         Entry entry = (Entry)iterator.next();
         iterator.remove();
         this.destroy(entry.getValue());
         logger.debug("Session entry dropped: {} => {}", entry.getKey(), entry.getValue());
      }

   }

   protected abstract String sessionKey();

   protected abstract Object create(String var1);

   protected abstract void destroy(Object var1);
}
