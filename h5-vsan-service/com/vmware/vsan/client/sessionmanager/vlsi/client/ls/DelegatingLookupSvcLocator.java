package com.vmware.vsan.client.sessionmanager.vlsi.client.ls;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DelegatingLookupSvcLocator implements LookupSvcLocator {
   private static final Log logger = LogFactory.getLog(DelegatingLookupSvcLocator.class);
   private final LookupSvcLocator[] locators;

   public DelegatingLookupSvcLocator(LookupSvcLocator[] locators) {
      this.locators = locators;
   }

   public LookupSvcInfo getInfo() {
      return (LookupSvcInfo)this.callLocatorsChain("LookupSvcInfo", (locator) -> {
         return locator.getInfo();
      });
   }

   public KeyStore getH5Keystore() {
      return (KeyStore)this.callLocatorsChain("H5 Keystore", (locator) -> {
         return locator.getH5Keystore();
      });
   }

   public PrivateKey getPrivateKey() {
      return (PrivateKey)this.callLocatorsChain("Private Key", (locator) -> {
         return (PrivateKey)Optional.of(locator.getPrivateKey()).get();
      });
   }

   public String getNodeId() {
      return (String)this.callLocatorsChain("Node ID", (locator) -> {
         return locator.getNodeId();
      });
   }

   private Object callLocatorsChain(String dataName, DelegatingLookupSvcLocator.LocatorProcessor processor) {
      LookupSvcLocator[] var3 = this.locators;
      int var4 = var3.length;
      int var5 = 0;

      while(var5 < var4) {
         LookupSvcLocator locator = var3[var5];

         try {
            return processor.process(locator);
         } catch (Throwable var8) {
            logger.trace("Failed to obtain " + dataName + " from " + locator, var8);
            ++var5;
         }
      }

      throw new IllegalStateException("Unable to obtain " + dataName + " from any locator.");
   }

   @FunctionalInterface
   interface LocatorProcessor {
      Object process(LookupSvcLocator var1);
   }
}
