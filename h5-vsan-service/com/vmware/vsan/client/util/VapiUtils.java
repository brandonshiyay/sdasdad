package com.vmware.vsan.client.util;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.net.URI;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VapiUtils {
   private static final Log logger = LogFactory.getLog(VapiUtils.class);
   private static final String VAPI_URI_SEPARATOR = ":";
   private static final String VAPI_URI_FORMAT = "urn:vapi:%s:%s:%s";

   public static String getVapiId(URI uri) {
      if (uri == null) {
         logger.warn("No URI");
         return "";
      } else {
         String vapiUrn = uri.getSchemeSpecificPart();
         if (StringUtils.isEmpty(vapiUrn)) {
            logger.warn("No Vapi URI");
            return "";
         } else {
            String[] chunks = vapiUrn.split(":");
            if (!ArrayUtils.isEmpty(chunks) && chunks.length == 4) {
               return chunks[2];
            } else {
               logger.warn("Invalid Vapi URI: " + vapiUrn);
               return "";
            }
         }
      }
   }

   public static URI createVapiUri(String type, String id, String serverGuid) {
      String uri = String.format("urn:vapi:%s:%s:%s", type, id, serverGuid);
      return URI.create(uri);
   }

   public static URI createVapiUri(ManagedObjectReference moRef) {
      String uri = String.format("urn:vapi:%s:%s:%s", moRef.getType(), moRef.getValue(), moRef.getServerGuid());
      return URI.create(uri);
   }
}
