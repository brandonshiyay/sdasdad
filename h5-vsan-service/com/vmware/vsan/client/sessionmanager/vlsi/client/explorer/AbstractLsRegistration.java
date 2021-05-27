package com.vmware.vsan.client.sessionmanager.vlsi.client.explorer;

import com.vmware.vim.binding.lookup.ServiceRegistration.Attribute;
import com.vmware.vim.binding.lookup.ServiceRegistration.Endpoint;
import com.vmware.vim.binding.lookup.ServiceRegistration.Info;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractLsRegistration {
   private static final String ATTR_DOMAIN_TYPE = "domainType";
   private static final String DOMAIN_CLOUD = "CLOUD";
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   protected final Info info;

   public AbstractLsRegistration(Info info) {
      this.info = info;
   }

   public UUID getUuid() {
      return UUID.fromString(this.info.getServiceId());
   }

   public String getOwnerId() {
      return this.info.getOwnerId();
   }

   public String getVersion() {
      return this.info.getServiceVersion();
   }

   public URI getServiceUrl() {
      return this.getDefaultEndpoint().getUrl();
   }

   public String[] getSslTrust() {
      return this.getDefaultEndpoint().getSslTrust();
   }

   public String getDomainType() {
      return this.findAttributeValue("domainType");
   }

   public String getServiceId() {
      return this.info.serviceId;
   }

   public boolean isVmc() {
      return "CLOUD".equals(this.getDomainType());
   }

   public String toString() {
      return String.format("%s [uuid=%s]", this.getClass().getSimpleName(), this.info.getServiceId());
   }

   protected String findAttributeValue(String attrName) {
      try {
         return this.findAttribute(attrName).value;
      } catch (Exception var3) {
         this.logger.warn("Cannot find attribute '" + attrName + "'. Returning null instead.", var3);
         return null;
      }
   }

   protected Attribute findAttribute(String attrName) {
      Attribute[] var2 = this.info.getServiceAttributes();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Attribute a = var2[var4];
         if (a.key.equals(attrName)) {
            return a;
         }
      }

      throw new IllegalStateException("Attribute not found: " + attrName);
   }

   protected Endpoint findEndpoint(String type) {
      Endpoint[] var2 = this.info.getServiceEndpoints();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Endpoint e = var2[var4];
         if (e.getEndpointType().getType().equals(type)) {
            return e;
         }
      }

      throw new IllegalStateException("Endpoint not found: " + type);
   }

   protected Endpoint getDefaultEndpoint() {
      if (this.info.serviceEndpoints.length == 1) {
         return this.info.getServiceEndpoints()[0];
      } else {
         throw new IllegalStateException("Could not determine default endpoint, only one expected in query result.");
      }
   }
}
