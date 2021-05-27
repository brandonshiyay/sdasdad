package com.vmware.vsan.client.sessionmanager.resource;

import java.io.Closeable;
import java.util.Date;

public class CacheEntry {
   protected final Closeable resource;
   protected final Runnable parentCloseHandler;
   protected int refCount = 1;
   protected long lastReleaseTime = -1L;

   public Closeable getResource() {
      return this.resource;
   }

   public CacheEntry(Closeable resource, Runnable parentCloseHandler) {
      this.resource = resource;
      this.parentCloseHandler = parentCloseHandler;
   }

   public int getRefCount() {
      return this.refCount;
   }

   public void incRefCount() {
      ++this.refCount;
   }

   public void decRefCount() {
      if (this.refCount <= 0) {
         throw new IllegalStateException("Releasing an entry with zero refCount");
      } else {
         --this.refCount;
         this.lastReleaseTime = System.currentTimeMillis();
      }
   }

   public Runnable getParentCloseHandler() {
      return this.parentCloseHandler;
   }

   public long getLastReleaseTime() {
      return this.lastReleaseTime;
   }

   public String toString() {
      return String.format("CacheEntry [resource=%s, refCount=%s, lastReleaseTime=%s]", this.resource, this.refCount, new Date(this.lastReleaseTime));
   }
}
