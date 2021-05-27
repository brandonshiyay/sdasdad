package com.vmware.vsan.client.sessionmanager.common.util;

import java.util.HashMap;
import java.util.Map;

public class EqualityLock {
   protected final Map locks = new HashMap();

   public Object getLock(Object object) {
      Object lock = this.locks.get(object);
      if (lock != null) {
         return lock;
      } else {
         Object lock = new EqualityLock.Idlock();
         this.locks.put(object, lock);
         return lock;
      }
   }

   public void evict(Object object) {
      this.locks.remove(object);
   }

   protected static class Idlock {
   }
}
