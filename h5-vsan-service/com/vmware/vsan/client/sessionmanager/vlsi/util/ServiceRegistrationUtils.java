package com.vmware.vsan.client.sessionmanager.vlsi.util;

import com.vmware.vim.binding.lookup.ServiceRegistration.Attribute;
import com.vmware.vim.binding.lookup.ServiceRegistration.Info;
import java.util.stream.Stream;

public class ServiceRegistrationUtils {
   private static final String ATTR_DOMAIN_ID = "domainId";

   public static String getDomainId(Info info) {
      Attribute[] serviceAttributes = info.getServiceAttributes();
      return (String)Stream.of(serviceAttributes).filter((attr) -> {
         return "domainId".equalsIgnoreCase(attr.key);
      }).map((attr) -> {
         return attr.value;
      }).findAny().orElse((Object)null);
   }
}
